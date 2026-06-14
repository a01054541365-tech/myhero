package com.jjk.entity.npc;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.combat.DamageCalculator;
import com.jjk.combat.DamageContext;
import com.jjk.data.PlayerData;
import com.jjk.entity.cursed.BaseCursedSpiritEntity;
import com.jjk.item.CursedToolRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// 주술사 NPC. PathAwareEntity 기반. 3단계 AI(중립/협력/적대).
public class SorcererNPCEntity extends PathAwareEntity {

    public enum SorcererNPCGrade {
        GRADE_4( 40f, 15f, 0.30f, 50),
        GRADE_3( 60f, 25f, 0.32f, 100),
        GRADE_2( 90f, 40f, 0.34f, 150),
        GRADE_1(130f, 60f, 0.36f, 200);

        public final float maxHp;
        public final float baseDamage;
        public final float moveSpeed;
        public final int   xpDrop;

        SorcererNPCGrade(float maxHp, float baseDamage, float moveSpeed, int xpDrop) {
            this.maxHp      = maxHp;
            this.baseDamage = baseDamage;
            this.moveSpeed  = moveSpeed;
            this.xpDrop     = xpDrop;
        }
    }

    private enum AIState { NEUTRAL, COOPERATIVE, HOSTILE }

    private static final double COOPERATION_RANGE    = 32.0;
    private static final double COOPERATION_RANGE_SQ = COOPERATION_RANGE * COOPERATION_RANGE;
    private static final int    MAX_COOPERATORS      = 3;
    private static final long   COMBAT_RECENT_TICKS  = 100L; // 5초 이내 전투 중
    private static final int    NO_SPIRIT_TIMEOUT    = 60;   // 3초 후 중립 복귀
    private static final double HOSTILE_RANGE_SQ     = 10000.0; // 100블록²
    private static final float  TOOL_DROP_CHANCE     = 0.10f;

    private final SorcererNPCGrade grade;
    private AIState aiState       = AIState.NEUTRAL;
    private UUID    hostileTargetUuid = null;
    private int     cooperationCount  = 0;
    private int     hostileCooldown   = 0;
    private int     noSpiritTick      = 0;

    public SorcererNPCEntity(EntityType<? extends SorcererNPCEntity> type, World world,
                              SorcererNPCGrade grade) {
        super(type, world);
        this.grade = grade;
        initEntityGoals();
    }

    @Override
    protected void initGoals() {
        // grade가 null인 상태(MobEntity 생성자 호출 시점)에서 호출되므로 비워둠.
        // 실제 Goal 등록은 initEntityGoals()에서 수행.
    }

    private void initEntityGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        goalSelector.add(9, new LookAroundGoal(this));
        // targetSelector 없음 — 타겟은 tick()에서 수동 관리
    }

    public static DefaultAttributeContainer.Builder createAttributes(SorcererNPCGrade g) {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     g.maxHp)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  g.baseDamage)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, g.moveSpeed)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   32.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient()) return;
        if (hostileCooldown > 0) hostileCooldown--;
        switch (aiState) {
            case NEUTRAL     -> checkForCursedSpirits();
            case COOPERATIVE -> checkCooperationCondition();
            case HOSTILE     -> checkHostileCondition();
        }
    }

    // NEUTRAL: 반경 32블록 내 주령 + 전투 중 플레이어 감지 시 COOPERATIVE 전환
    private void checkForCursedSpirits() {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        List<BaseCursedSpiritEntity> spirits = sw.getEntitiesByClass(
            BaseCursedSpiritEntity.class, getBoundingBox().expand(COOPERATION_RANGE), e -> e.isAlive());
        if (spirits.isEmpty()) return;

        long now = sw.getTime();
        for (ServerPlayerEntity player : sw.getPlayers()) {
            if (squaredDistanceTo(player) > COOPERATION_RANGE_SQ) continue;
            PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
            if (now - data.lastCombatTick > COMBAT_RECENT_TICKS) continue;

            long cooperators = sw.getEntitiesByClass(SorcererNPCEntity.class,
                getBoundingBox().expand(COOPERATION_RANGE),
                e -> e != this && e.aiState == AIState.COOPERATIVE).size();
            if (cooperators < MAX_COOPERATORS) {
                aiState = AIState.COOPERATIVE;
                noSpiritTick = 0;
                setNearestSpiritTarget(spirits);
            }
            return;
        }
    }

    // COOPERATIVE: 반경 내 주령 소멸 시 60틱 후 NEUTRAL 복귀
    private void checkCooperationCondition() {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        List<BaseCursedSpiritEntity> spirits = sw.getEntitiesByClass(
            BaseCursedSpiritEntity.class, getBoundingBox().expand(COOPERATION_RANGE), e -> e.isAlive());
        if (spirits.isEmpty()) {
            noSpiritTick++;
            if (noSpiritTick >= NO_SPIRIT_TIMEOUT) {
                noSpiritTick = 0;
                aiState = AIState.NEUTRAL;
                setTarget(null);
            }
        } else {
            noSpiritTick = 0;
            setNearestSpiritTarget(spirits);
        }
    }

    // HOSTILE: 타겟 플레이어 오프라인/100블록 초과 시 NEUTRAL 복귀
    private void checkHostileCondition() {
        if (hostileTargetUuid == null) {
            aiState = AIState.NEUTRAL;
            return;
        }
        if (!(getWorld() instanceof ServerWorld sw)) return;
        ServerPlayerEntity target = sw.getServer().getPlayerManager().getPlayer(hostileTargetUuid);
        if (target == null || squaredDistanceTo(target) > HOSTILE_RANGE_SQ) {
            aiState = AIState.NEUTRAL;
            hostileTargetUuid = null;
            setTarget(null);
            return;
        }
        setTarget(target);
    }

    private void setNearestSpiritTarget(List<BaseCursedSpiritEntity> spirits) {
        spirits.stream()
            .min(Comparator.comparingDouble(e -> squaredDistanceTo((Entity) e)))
            .ifPresent(this::setTarget);
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (target instanceof ServerPlayerEntity spe) {
            DamageContext ctx = DamageContext
                .builder((ServerPlayerEntity) null, spe, IDamageSource.NORMAL_TECHNIQUE, grade.baseDamage)
                .build();
            float damage = new DamageCalculator().calculate(ctx);
            spe.damage(getDamageSources().mobAttack(this), damage);
            return true;
        }
        return super.tryAttack(target);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean taken = super.damage(source, amount);
        if (taken && source.getAttacker() instanceof PlayerEntity attacker
                && aiState != AIState.HOSTILE) {
            aiState = AIState.HOSTILE;
            hostileTargetUuid = attacker.getUuid();
            noSpiritTick = 0;
            cooperationCount = 0;
        }
        return taken;
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!(source.getAttacker() instanceof ServerPlayerEntity killer)) return;
        if (!(getWorld() instanceof ServerWorld)) return;

        PlayerData killerData = JJKMod.getPlayerRepository().load(killer.getUuid());
        JJKMod.getGradeManager().addXp(killerData, grade.xpDrop, killer);

        // GRADE_3 이하 주구 드롭 10% 확률
        if (grade.ordinal() <= SorcererNPCGrade.GRADE_3.ordinal()
                && getWorld().getRandom().nextFloat() < TOOL_DROP_CHANCE
                && CursedToolRegistry.CURSED_DAGGER != null) {
            ItemStack drop = new ItemStack(CursedToolRegistry.CURSED_DAGGER);
            if (!killer.getInventory().insertStack(drop)) {
                killer.dropItem(drop, false);
            }
        }
    }

    @Override
    protected int getXpToDrop() { return grade.xpDrop; }

    public SorcererNPCGrade getSorcererGrade() { return grade; }
}
