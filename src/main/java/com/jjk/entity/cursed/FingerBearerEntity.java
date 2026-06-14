package com.jjk.entity.cursed;

import com.jjk.JJKMod;
import com.jjk.entity.CeProjectileEntity;
import com.jjk.entity.JJKEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

// 손가락 기생 주령. 특급. 근접 + 원거리 CE 폭발 발사체. 사망 시 2배 확률 스쿠나 손가락 드롭.
public class FingerBearerEntity extends BaseCursedSpiritEntity {

    private static final float BASE_DAMAGE_MELEE  = 120f;
    private static final float BASE_DAMAGE_RANGED =  80f;
    private static final int   RANGED_RANGE_SQ    = 225; // 15블록²
    private static final int   RANGED_COOLDOWN    =  40;

    public FingerBearerEntity(EntityType<? extends FingerBearerEntity> type, World world) {
        super(type, world, CursedSpiritGrade.SPECIAL_GRADE);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     CursedSpiritGrade.SPECIAL_GRADE.hpMultiplier())
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  BASE_DAMAGE_MELEE)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   32.0);
    }

    @Override
    protected void initEntityGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new CeBlastGoal());
        goalSelector.add(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        goalSelector.add(9, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (target instanceof PlayerEntity player) {
            applyDamageToPlayer(player, BASE_DAMAGE_MELEE);
            return true;
        }
        return super.tryAttack(target);
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!(getWorld() instanceof ServerWorld)) return;
        if (!(source.getAttacker() instanceof ServerPlayerEntity killer)) return;
        // §LOCK: fingerDropRate × 2.0 보너스 드롭
        float bonusRate = Math.min(JJKMod.getConfig().fingerDropRate * 2.0f, 1.0f);
        JJKMod.getFingerSystem().tryDropFromKill(
            getUuid(), killer.getUuid(), "finger_bearer_npc", bonusRate);
    }

    // CE 폭발 발사체 원거리 공격 Goal
    private class CeBlastGoal extends Goal {

        private int cooldown = 0;

        CeBlastGoal() {
            setControls(EnumSet.of(Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = FingerBearerEntity.this.getTarget();
            if (target == null || !target.isAlive() || cooldown > 0) return false;
            double distSq = FingerBearerEntity.this.squaredDistanceTo(target);
            return distSq <= RANGED_RANGE_SQ;
        }

        @Override
        public void start() {
            cooldown = RANGED_COOLDOWN;
            if (!(FingerBearerEntity.this.getWorld() instanceof ServerWorld sw)) return;
            LivingEntity target = FingerBearerEntity.this.getTarget();
            if (target == null) return;
            CeProjectileEntity proj = new CeProjectileEntity(JJKEntities.CE_PROJECTILE, sw);
            proj.setOwner(FingerBearerEntity.this);
            Vec3d eye = FingerBearerEntity.this.getEyePos();
            Vec3d dir = target.getEyePos().subtract(eye).normalize();
            proj.setPosition(eye.x, eye.y, eye.z);
            proj.setDamage(BASE_DAMAGE_RANGED * grade.damageMultiplier());
            proj.setVelocity(dir.x, dir.y, dir.z, 1.5f, 1.0f);
            sw.spawnEntity(proj);
        }

        @Override
        public boolean shouldContinue() { return false; }

        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;
            LivingEntity target = FingerBearerEntity.this.getTarget();
            if (target != null) {
                FingerBearerEntity.this.getLookControl().lookAt(target, 30f, 30f);
            }
        }
    }
}
