package com.jjk.entity;

import com.jjk.JJKMod;
import com.jjk.anim.AnimationRegistry;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.UUID;

public class RikaEntity extends PathAwareEntity implements GeoAnimatable {

    private final UUID ownerUuid;
    private int lifetimeTicks = 0;
    private volatile byte pendingAnimId = 0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RikaEntity(EntityType<? extends RikaEntity> type, World world, UUID ownerUuid) {
        super(type, world);
        this.ownerUuid = ownerUuid;
    }

    @Override
    public void tick() {
        super.tick();
        lifetimeTicks++;
        if (!getWorld().isClient && lifetimeTicks >= JJKMod.getConfig().rikaLifetimeTicks) {
            discard();
        }
    }

    // 애니메이션 제어는 클라이언트 렌더러에서 처리 — 서버 엔티티는 no-op
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public double getTick(Object worldObj) { return lifetimeTicks; }

    public void sendAnimToNearby(byte animId) {
        if (getWorld().isClient) return;
        ServerWorld sw = (ServerWorld) getWorld();
        AnimationTriggerS2CPacket pkt = new AnimationTriggerS2CPacket(getUuid(), animId);
        for (ServerPlayerEntity player : sw.getPlayers()) {
            if (player.squaredDistanceTo(this) <= 32 * 32) {
                ServerPlayNetworking.send(player, pkt);
            }
        }
    }

    public byte getPendingAnimId() { return pendingAnimId; }
    public void setPendingAnimId(byte animId) { this.pendingAnimId = animId; }
    public UUID getOwnerUuid() { return ownerUuid; }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new RikaMeleeAttackGoal(this));
        this.goalSelector.add(2, new RikaPursueGoal(this));
        this.goalSelector.add(3, new RikaReturnGoal(this));
        this.targetSelector.add(1, new RikaFindTargetGoal(this));
    }

    private ServerPlayerEntity getSummoner() {
        if (!(getWorld() instanceof ServerWorld sw)) return null;
        return sw.getServer().getPlayerManager().getPlayer(ownerUuid);
    }

    // ── 전투 AI: 기본 행동 패턴 (탐색 → 추격 → 근접 공격 / 대상 없을 시 복귀) ──

    private static final double SEARCH_RADIUS  = 10.0;
    private static final double ATTACK_RANGE   = 2.0;
    private static final double RETURN_RADIUS  = 3.0;
    private static final double MOVE_SPEED     = 0.6;
    private static final int    ATTACK_COOLDOWN_TICKS = 20;

    /** Goal 1 — 소환자 기준 10블록 이내 적대 플레이어/CursedSpiritEntity 탐색 */
    private static class RikaFindTargetGoal extends Goal {
        private final RikaEntity rika;

        RikaFindTargetGoal(RikaEntity rika) {
            this.rika = rika;
            this.setControls(EnumSet.noneOf(Control.class));
        }

        @Override
        public boolean canStart() {
            return rika.getTarget() == null || !rika.getTarget().isAlive();
        }

        @Override
        public void tick() {
            ServerPlayerEntity summoner = rika.getSummoner();
            if (!(rika.getWorld() instanceof ServerWorld sw) || summoner == null) return;

            PlayerData summonerData = JJKMod.getPlayerRepository().load(rika.ownerUuid);
            LivingEntity nearest = null;
            double nearestDist = Double.MAX_VALUE;
            for (LivingEntity e : sw.getEntitiesByClass(LivingEntity.class,
                    summoner.getBoundingBox().expand(SEARCH_RADIUS),
                    le -> le.isAlive() && !le.equals(rika) && !le.equals(summoner))) {
                boolean hostile;
                if (e instanceof ServerPlayerEntity sp) {
                    PlayerData spData = JJKMod.getPlayerRepository().load(sp.getUuid());
                    hostile = !JJKMod.getTeamManager().isSameTeam(summonerData, spData);
                } else if (e instanceof CursedSpiritEntity) {
                    hostile = true;
                } else {
                    continue;
                }
                if (!hostile) continue;
                double dist = e.squaredDistanceTo(summoner);
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = e;
                }
            }
            if (nearest != null) {
                rika.setTarget(nearest);
            }
        }
    }

    /** Goal 2 — 대상 추격 (속도 0.6) */
    private static class RikaPursueGoal extends Goal {
        private final RikaEntity rika;

        RikaPursueGoal(RikaEntity rika) {
            this.rika = rika;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = rika.getTarget();
            return target != null && target.isAlive()
                    && rika.squaredDistanceTo(target) > ATTACK_RANGE * ATTACK_RANGE;
        }

        @Override
        public void tick() {
            LivingEntity target = rika.getTarget();
            if (target == null) return;
            rika.getLookControl().lookAt(target, 30f, 30f);
            rika.getNavigation().startMovingTo(target, MOVE_SPEED);
        }
    }

    /** Goal 3 — 근접 공격 (2블록 이내, 20틱 쿨다운) */
    private static class RikaMeleeAttackGoal extends Goal {
        private final RikaEntity rika;
        private int cooldown = 0;

        RikaMeleeAttackGoal(RikaEntity rika) {
            this.rika = rika;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = rika.getTarget();
            return target != null && target.isAlive()
                    && rika.squaredDistanceTo(target) <= ATTACK_RANGE * ATTACK_RANGE;
        }

        @Override
        public void start() {
            cooldown = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = rika.getTarget();
            if (target == null) return;
            rika.getLookControl().lookAt(target, 30f, 30f);
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            cooldown = ATTACK_COOLDOWN_TICKS;
            if (!(rika.getWorld() instanceof ServerWorld sw)) return;
            ServerPlayerEntity summoner = rika.getSummoner();
            if (summoner == null) return;
            float rawDamage = JJKMod.getConfig().rikaMeleeDamage();
            JJKMod.getCombatPipeline().applyDomainDamage(summoner, target, rawDamage, 1.0f, sw);
            if (JJKMod.getAuditLogger() != null) {
                JJKMod.getAuditLogger().logEvent("DAMAGE", target.getUuid(),
                    String.format("attacker=%s dmg=%.1f src=rika_auto", rika.ownerUuid, rawDamage),
                    sw.getTime());
            }
        }
    }

    /** Goal 4 — 대상이 없을 시 소환자 3블록 이내로 복귀 */
    private static class RikaReturnGoal extends Goal {
        private final RikaEntity rika;

        RikaReturnGoal(RikaEntity rika) {
            this.rika = rika;
            this.setControls(EnumSet.of(Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (rika.getTarget() != null) return false;
            ServerPlayerEntity summoner = rika.getSummoner();
            return summoner != null
                    && rika.squaredDistanceTo(summoner) > RETURN_RADIUS * RETURN_RADIUS;
        }

        @Override
        public void tick() {
            ServerPlayerEntity summoner = rika.getSummoner();
            if (summoner == null) return;
            rika.getNavigation().startMovingTo(summoner, MOVE_SPEED);
        }
    }
}
