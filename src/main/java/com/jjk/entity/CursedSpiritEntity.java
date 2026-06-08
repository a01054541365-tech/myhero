package com.jjk.entity;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.entity.ai.CursedSpiritGoals;
import com.jjk.item.CursedCrystalItem;
import com.jjk.item.CursedToolRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class CursedSpiritEntity extends HostileEntity {

    private final CursedSpiritGrade grade;

    // Phase system (준특급/특급)
    private int currentPhase = 1;
    private long lastAreaAttackTick = -1L;

    // HP 50% 속도 부스트 (4급/3급)
    private boolean speedBoosted = false;

    public CursedSpiritEntity(EntityType<? extends CursedSpiritEntity> type,
                               World world, CursedSpiritGrade grade) {
        super(type, world);
        this.grade = grade;
        initGoalsAfterGrade();
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;

        // 격노 타이머 감소 (G-2)
        if (enragedTicksRemaining > 0) {
            enragedTicksRemaining--;
            if (enragedTicksRemaining == 0) {
                EntityAttributeInstance attack = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                EntityAttributeInstance speed  = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (attack != null) attack.setBaseValue(grade.attackDamage);
                if (speed  != null) speed.setBaseValue(grade.movementSpeed);
            }
        }

        if (age % 4 != 0) return;

        // HP 50% 속도 부스트 (4급/3급)
        if (!speedBoosted
                && (grade == CursedSpiritGrade.GRADE_4 || grade == CursedSpiritGrade.GRADE_3)
                && getHealth() <= getMaxHealth() * 0.5f) {
            speedBoosted = true;
            EntityAttributeInstance attr = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (attr != null) attr.setBaseValue(grade.movementSpeed * 1.3f);
        }

        // Phase 전환 (준특급/특급)
        if (grade == CursedSpiritGrade.SEMI_SPECIAL || grade == CursedSpiritGrade.SPECIAL) {
            updatePhase();
        }
    }

    private void updatePhase() {
        float ratio = getHealth() / getMaxHealth();
        int newPhase = ratio > 0.66f ? 1 : ratio > 0.33f ? 2 : 3;
        if (newPhase != currentPhase) {
            currentPhase = newPhase;
            applyPhaseAttributes(newPhase);
        }
        // Phase 3: 5초(100틱)마다 반경 5블록 범위 공격
        if (currentPhase == 3 && getWorld() instanceof ServerWorld sw) {
            long now = sw.getTime();
            if (now - lastAreaAttackTick >= 100L) {
                lastAreaAttackTick = now;
                for (LivingEntity target : sw.getEntitiesByClass(
                        LivingEntity.class, getBoundingBox().expand(5.0),
                        e -> e.isAlive() && !e.equals(this))) {
                    target.damage(getDamageSources().mobAttack(this),
                        grade.attackDamage * 0.5f);
                }
            }
        }
    }

    private void applyPhaseAttributes(int phase) {
        EntityAttributeInstance speed  = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        EntityAttributeInstance attack = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (speed == null || attack == null) return;
        switch (phase) {
            case 2 -> {
                speed.setBaseValue(grade.movementSpeed * 1.2f);
                attack.setBaseValue(grade.attackDamage);
            }
            case 3 -> {
                speed.setBaseValue(grade.movementSpeed * 1.5f);
                attack.setBaseValue(grade.attackDamage * 1.5f);
            }
            default -> {
                speed.setBaseValue(grade.movementSpeed);
                attack.setBaseValue(grade.attackDamage);
            }
        }
    }

    public static DefaultAttributeContainer.Builder createAttributes(CursedSpiritGrade grade) {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     grade.maxHp)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  grade.attackDamage)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, grade.movementSpeed)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   grade.detectionRange);
    }

    @Override
    protected void initGoals() {
        // grade는 super() 반환 후에야 세팅되므로, 부모 생성자 호출 시점에는 항상 null.
        // 실제 goal 등록은 initGoalsAfterGrade()에서 수행.
        if (this.grade == null) return;
        initGoalsAfterGrade();
    }

    private void initGoalsAfterGrade() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, grade.detectionRange));
        goalSelector.add(9, new LookAroundGoal(this));

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));

        addGradeSpecificGoals();
    }

    private void addGradeSpecificGoals() {
        switch (grade) {
            case GRADE_3 -> goalSelector.add(3,
                new CursedSpiritGoals.RangedCeAttackGoal(this, 20, 12f));
            case GRADE_2 -> {
                goalSelector.add(3, new CursedSpiritGoals.RangedCeAttackGoal(this, 16, 15f));
                goalSelector.add(4, new CursedSpiritGoals.EvasiveGoal(this, 0.5f));
            }
            case GRADE_1 -> {
                goalSelector.add(3, new CursedSpiritGoals.RangedCeAttackGoal(this, 12, 18f));
                goalSelector.add(4, new CursedSpiritGoals.EvasiveGoal(this, 0.6f));
                goalSelector.add(5, new CursedSpiritGoals.CeBurstGoal(this, 60, 25f));
            }
            case SEMI_SPECIAL -> {
                goalSelector.add(3, new CursedSpiritGoals.RangedCeAttackGoal(this, 10, 22f));
                goalSelector.add(4, new CursedSpiritGoals.EvasiveGoal(this, 0.6f));
                goalSelector.add(5, new CursedSpiritGoals.CeBurstGoal(this, 50, 30f));
            }
            case SPECIAL -> {
                goalSelector.add(3, new CursedSpiritGoals.RangedCeAttackGoal(this, 10, 22f));
                goalSelector.add(4, new CursedSpiritGoals.EvasiveGoal(this, 0.7f));
                goalSelector.add(5, new CursedSpiritGoals.CeBurstGoal(this, 40, 35f));
                goalSelector.add(6, new CursedSpiritGoals.DomainDeployGoal(this, 1200));
            }
            default -> { /* GRADE_4: 기본 근접만 */ }
        }
    }

    // G-2: 포획 실패 시 격노 상태 부여 (durationTicks 동안 공격력 1.5×, 속도 1.3×)
    private int enragedTicksRemaining = 0;

    public void setEnraged(int durationTicks) {
        enragedTicksRemaining = durationTicks;
        EntityAttributeInstance attack = getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        EntityAttributeInstance speed  = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (attack != null) attack.setBaseValue(grade.attackDamage * 1.5f);
        if (speed  != null) speed.setBaseValue(grade.movementSpeed * 1.3f);
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        JJKMod.getDomainManager().collapseDomain(getUuid(), getWorld().getTime());
        if (source.getAttacker() instanceof ServerPlayerEntity killer) {
            PlayerData killerData = JJKMod.getPlayerRepository().load(killer.getUuid());
            JJKMod.getGradeManager().addXp(killerData, grade.xpDrop, killer);

            // P3-1: CE 결정체 드롭
            if (CursedCrystalItem.INSTANCE != null) {
                int crystals = getCrystalDrop(grade);
                if (crystals > 0) {
                    ItemStack crystal = new ItemStack(CursedCrystalItem.INSTANCE, crystals);
                    if (!killer.getInventory().insertStack(crystal)) {
                        killer.dropItem(crystal, false);
                    }
                }
            }

            // P3-3: 주구 드롭
            if (getWorld() instanceof ServerWorld sw) {
                tryDropCursedTool(grade, killer, sw);
            }
        }
        // 4급/3급 사망 시 반경 3블록 폭발 데미지 (baseDamage=6)
        if ((grade == CursedSpiritGrade.GRADE_4 || grade == CursedSpiritGrade.GRADE_3)
                && getWorld() instanceof ServerWorld sw) {
            for (ServerPlayerEntity p : sw.getPlayers()) {
                if (squaredDistanceTo(p) <= 9.0) { // 3블록²
                    p.damage(getDamageSources().mobAttack(this), 6f);
                }
            }
        }
    }

    @Override
    protected int getXpToDrop() { return grade.xpDrop; }

    public CursedSpiritGrade getGrade() { return grade; }

    // ─── 드롭 헬퍼 ────────────────────────────────────────────────────────────

    private static int getCrystalDrop(CursedSpiritGrade g) {
        return switch (g) {
            case GRADE_4      -> 1;
            case GRADE_3      -> 3;
            case GRADE_2      -> 7;
            case GRADE_1      -> 15;
            case SEMI_SPECIAL -> 30;
            case SPECIAL      -> 60;
        };
    }

    private static void tryDropCursedTool(CursedSpiritGrade g,
                                           ServerPlayerEntity killer,
                                           ServerWorld sw) {
        float rate = switch (g) {
            case GRADE_4      -> 0.05f;
            case GRADE_3      -> 0.10f;
            case GRADE_2      -> 0.15f;
            case GRADE_1      -> 0.20f;
            case SEMI_SPECIAL -> 0.30f;
            case SPECIAL      -> 0.50f;
        };
        if (sw.getRandom().nextFloat() >= rate) return;
        ItemStack tool = selectRandomTool(g, sw);
        if (!tool.isEmpty()) {
            if (!killer.getInventory().insertStack(tool)) {
                killer.dropItem(tool, false);
            }
        }
    }

    private static ItemStack selectRandomTool(CursedSpiritGrade g, ServerWorld sw) {
        if (CursedToolRegistry.CURSED_DAGGER == null) return ItemStack.EMPTY;
        return switch (g) {
            case GRADE_4 -> new ItemStack(CursedToolRegistry.CURSED_DAGGER);
            case GRADE_3, GRADE_2 -> sw.getRandom().nextBoolean()
                    ? new ItemStack(CursedToolRegistry.CURSED_DAGGER)
                    : (CursedToolRegistry.THOUSAND_SPEAR != null
                        ? new ItemStack(CursedToolRegistry.THOUSAND_SPEAR)
                        : new ItemStack(CursedToolRegistry.CURSED_DAGGER));
            case GRADE_1, SEMI_SPECIAL, SPECIAL -> {
                var pool = new net.minecraft.item.Item[]{
                    CursedToolRegistry.CURSED_DAGGER,
                    CursedToolRegistry.THOUSAND_SPEAR,
                    CursedToolRegistry.PLAYFUL_CLOUD,
                    CursedToolRegistry.INVERTED_SPEAR,
                    CursedToolRegistry.SPLIT_SOUL_BLADE
                };
                int roll = sw.getRandom().nextInt(pool.length);
                yield pool[roll] != null ? new ItemStack(pool[roll]) : ItemStack.EMPTY;
            }
        };
    }
}
