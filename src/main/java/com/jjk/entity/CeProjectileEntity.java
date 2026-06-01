package com.jjk.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class CeProjectileEntity extends ProjectileEntity {

    private float damage = 0f;

    /** 엔티티 타입 등록용 기본 생성자 */
    public CeProjectileEntity(EntityType<? extends CeProjectileEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        // CeProjectileEntity에 추가 tracked data 없음
    }

    public void setDamage(float damage) { this.damage = damage; }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
        if (hit.getType() != HitResult.Type.MISS) {
            onCollision(hit);
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult hitResult) {
        // [BUG-03] 소유자가 LivingEntity인지 확인 후 형변환 — NPE·ClassCastException 방지
        if (hitResult.getEntity() instanceof LivingEntity target
                && getOwner() instanceof LivingEntity owner) {
            target.damage(getDamageSources().mobProjectile(this, owner), damage);
        }
        discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult hitResult) {
        discard();
    }

    @Override
    public boolean isOnFire() { return false; }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putFloat("ce_damage", damage);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.damage = nbt.getFloat("ce_damage");
    }
}
