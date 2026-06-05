package com.jjk.entity.ai;

import com.jjk.JJKMod;
import com.jjk.domain.DomainInstance;
import com.jjk.entity.CeProjectileEntity;
import com.jjk.entity.CursedSpiritEntity;
import com.jjk.entity.CursedSpiritEntityTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class CursedSpiritGoals {

    private CursedSpiritGoals() {}

    // ── 공통 원거리 CE 투사체 ──────────────────────────────────────
    public static class RangedCeAttackGoal extends Goal {

        private final CursedSpiritEntity spirit;
        private final int cooldownTicks;
        private final float damage;
        private int ticksUntilAttack;
        private LivingEntity target;

        public RangedCeAttackGoal(CursedSpiritEntity spirit, int cooldownTicks, float damage) {
            this.spirit        = spirit;
            this.cooldownTicks = cooldownTicks;
            this.damage        = damage;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            this.target = spirit.getTarget();
            return target != null && target.isAlive()
                && spirit.squaredDistanceTo(target) > 36.0;
        }

        @Override
        public void tick() {
            spirit.getLookControl().lookAt(target, 30f, 30f);
            if (--ticksUntilAttack <= 0) {
                ticksUntilAttack = cooldownTicks;
                if (spirit.getWorld() instanceof ServerWorld sw) {
                    Vec3d dir = target.getEyePos()
                        .subtract(spirit.getEyePos()).normalize();
                    CeProjectileEntity proj =
                        new CeProjectileEntity(CursedSpiritEntityTypes.CE_PROJECTILE, sw);
                    proj.setOwner(spirit);
                    Vec3d eye = spirit.getEyePos();
                    proj.setPosition(eye.x, eye.y, eye.z);
                    proj.setDamage(damage);
                    proj.setVelocity(dir.x, dir.y, dir.z, 1.6f, 1.0f);
                    sw.spawnEntity(proj);
                }
            }
        }
    }

    // ── 회피 Goal (피격 시 후퇴) ──────────────────────────────────
    public static class EvasiveGoal extends Goal {

        private final CursedSpiritEntity spirit;
        private final float evadeChance;

        public EvasiveGoal(CursedSpiritEntity spirit, float evadeChance) {
            this.spirit      = spirit;
            this.evadeChance = evadeChance;
        }

        @Override
        public boolean canStart() {
            return spirit.getLastAttacker() != null
                && ThreadLocalRandom.current().nextFloat() < evadeChance;
        }

        @Override
        public void start() {
            LivingEntity attacker = spirit.getTarget();
            if (attacker == null) return;
            Vec3d flee = spirit.getPos()
                .subtract(attacker.getPos()).normalize().multiply(4.0);
            spirit.getNavigation().startMovingTo(
                spirit.getX() + flee.x,
                spirit.getY(),
                spirit.getZ() + flee.z, 1.4);
        }
    }

    // ── CE 폭발 스킬 (1급+) ──────────────────────────────────────
    public static class CeBurstGoal extends Goal {

        private final CursedSpiritEntity spirit;
        private final int cooldownTicks;
        private final float damage;
        private int cooldown = 0;

        public CeBurstGoal(CursedSpiritEntity spirit, int cooldownTicks, float damage) {
            this.spirit        = spirit;
            this.cooldownTicks = cooldownTicks;
            this.damage        = damage;
        }

        @Override
        public boolean canStart() {
            return cooldown <= 0
                && spirit.getTarget() != null
                && spirit.squaredDistanceTo(spirit.getTarget()) <= 64.0;
        }

        @Override
        public void start() {
            cooldown = cooldownTicks;
            if (!(spirit.getWorld() instanceof ServerWorld sw)) return;
            List<LivingEntity> targets = sw.getEntitiesByClass(
                LivingEntity.class,
                spirit.getBoundingBox().expand(5.0),
                e -> e.isAlive() && !e.equals(spirit));
            for (LivingEntity t : targets) {
                t.damage(spirit.getDamageSources().mobAttack(spirit), damage);
            }
        }

        @Override
        public void tick() { if (cooldown > 0) cooldown--; }
    }

    // ── 영역 전개 Goal (특급 전용) ────────────────────────────────
    public static class DomainDeployGoal extends Goal {

        private final CursedSpiritEntity spirit;
        private final int cooldownTicks;
        private int cooldown = 0;

        public DomainDeployGoal(CursedSpiritEntity spirit, int cooldownTicks) {
            this.spirit        = spirit;
            this.cooldownTicks = cooldownTicks;
        }

        @Override
        public boolean canStart() {
            return cooldown <= 0
                && spirit.getHealth() < spirit.getMaxHealth() * 0.5f
                && spirit.getTarget() != null;
        }

        @Override
        public void start() {
            cooldown = cooldownTicks;
            if (!(spirit.getWorld() instanceof ServerWorld sw)) return;

            long currentTick = sw.getTime();
            boolean deployed = JJKMod.getDomainManager().deployNpcDomain(
                spirit.getUuid(),
                spirit.getPos(),
                "cursed_spirit_domain",
                currentTick);

            if (!deployed) {
                // 전개 실패 시 CE 폭발 fallback
                for (PlayerEntity p : sw.getPlayers()) {
                    if (spirit.squaredDistanceTo(p) <= 400.0) {
                        p.damage(spirit.getDamageSources().mobAttack(spirit),
                            spirit.getGrade().attackDamage * 1.5f);
                    }
                }
            }
        }

        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;

            // 영역 활성 중: 매 틱 범위 내 플레이어에게 자동 데미지
            if (!(spirit.getWorld() instanceof ServerWorld sw)) return;
            long currentTick = sw.getTime();
            DomainInstance domain = JJKMod.getDomainManager()
                .getActiveDomain(spirit.getUuid());
            if (domain == null || !domain.isSureHitReady(currentTick)) return;

            float tickDmg = spirit.getGrade().attackDamage * 0.3f;
            for (PlayerEntity p : sw.getPlayers()) {
                double r = domain.currentRadius;
                if (spirit.squaredDistanceTo(p) <= r * r) {
                    p.damage(spirit.getDamageSources().mobAttack(spirit), tickDmg);
                }
            }
        }
    }
}
