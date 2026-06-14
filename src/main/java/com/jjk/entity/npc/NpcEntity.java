package com.jjk.entity.npc;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public abstract class NpcEntity extends MobEntity {

    private final String npcId;

    protected NpcEntity(EntityType<? extends NpcEntity> type, World world, String npcId) {
        super(type, world);
        this.npcId = npcId;
        this.setInvulnerable(true);
        this.setAiDisabled(true);
        this.setPersistent();
    }

    public String getNpcId() { return npcId; }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!player.getWorld().isClient()
                && player instanceof ServerPlayerEntity sp
                && hand == Hand.MAIN_HAND) {
            onInteract(sp);
        }
        return ActionResult.SUCCESS;
    }

    protected abstract void onInteract(ServerPlayerEntity player);

    @Override
    public boolean isCustomNameVisible() { return true; }

    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        if (source.getAttacker() instanceof ServerPlayerEntity attacker) {
            com.jjk.event.EasterEggManager.onNpcAttacked(npcId, attacker);
        }
        return false;
    }

    @Override
    public boolean cannotDespawn() { return true; }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 1000.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25);
    }
}
