package com.jjk.entity.cursed;

import com.jjk.entity.JJKEntities;
import com.jjk.item.CECrystalItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

// 骨縛 (코츠바쿠) — 4급 주령. 뼈 투척 원거리 공격 + BIND 상태이상
public class KotsibakuEntity extends HostileEntity {

    public KotsibakuEntity(EntityType<? extends KotsibakuEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     40.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,   6.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,  0.23)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   20.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new RangedBoneAttackGoal());
        goalSelector.add(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        goalSelector.add(9, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!(getWorld() instanceof ServerWorld sw)) return;
        if (!(source.getAttacker() instanceof ServerPlayerEntity killer)) return;
        if (sw.getRandom().nextFloat() < 0.40f && CECrystalItem.INSTANCE != null) {
            ItemStack drop = new ItemStack(CECrystalItem.INSTANCE, 1);
            if (!killer.getInventory().insertStack(drop)) {
                killer.dropItem(drop, false);
            }
        }
    }

    // 원거리 뼈 투척 — 10블록 범위, 40틱 쿨타임
    private class RangedBoneAttackGoal extends Goal {

        private int cooldown = 0;

        RangedBoneAttackGoal() {
            setControls(EnumSet.of(Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = KotsibakuEntity.this.getTarget();
            if (target == null || !target.isAlive() || cooldown > 0) return false;
            double distSq = KotsibakuEntity.this.squaredDistanceTo(target);
            return distSq >= 9.0 && distSq <= 100.0; // 3~10블록
        }

        @Override
        public void start() {
            cooldown = 40;
            if (!(KotsibakuEntity.this.getWorld() instanceof ServerWorld sw)) return;
            LivingEntity target = KotsibakuEntity.this.getTarget();
            if (target == null) return;

            KotsibakuProjectileEntity proj = new KotsibakuProjectileEntity(
                JJKEntities.KOTSIBAKU_PROJECTILE, sw);
            proj.setOwner(KotsibakuEntity.this);
            Vec3d eye = KotsibakuEntity.this.getEyePos();
            Vec3d dir = target.getEyePos().subtract(eye).normalize();
            proj.setPosition(eye.x, eye.y, eye.z);
            proj.setDamage(6f);
            proj.setVelocity(dir.x, dir.y, dir.z, 1.6f, 1.0f);
            sw.spawnEntity(proj);
        }

        @Override
        public boolean shouldContinue() { return false; }

        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;
            LivingEntity target = KotsibakuEntity.this.getTarget();
            if (target != null) {
                KotsibakuEntity.this.getLookControl().lookAt(target, 30f, 30f);
            }
        }
    }
}
