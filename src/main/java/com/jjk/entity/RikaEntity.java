package com.jjk.entity;

import com.jjk.JJKMod;
import com.jjk.anim.AnimationRegistry;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

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
        // TODO: goalSelector.add(1, new FollowOwnerGoal(this, ownerUuid, 1.2, 3, 10));
        // TODO: goalSelector.add(2, new MeleeAttackGoal(this, 1.2, true));
        // TODO: targetSelector.add(1, new OwnerHurtByTargetGoal(this, ownerUuid));
    }
}
