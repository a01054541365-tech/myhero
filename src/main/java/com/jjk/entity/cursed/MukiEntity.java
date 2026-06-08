package com.jjk.entity.cursed;

import com.jjk.item.CECrystalItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

// 霧鬼 (무키) — 4급 주령. 안개 biome 스폰 전용, 반경 5블록 시야 방해
public class MukiEntity extends HostileEntity {

    private int blindnessTick = 0;

    public MukiEntity(EntityType<? extends MukiEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     30.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,   8.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,  0.25)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   16.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(9, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        if (++blindnessTick < 60) return;
        blindnessTick = 0;
        if (!(getWorld() instanceof ServerWorld sw)) return;
        for (ServerPlayerEntity player : sw.getPlayers()) {
            if (squaredDistanceTo(player) <= 25.0) { // 반경 5블록²
                player.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.BLINDNESS, 200, 0, false, false));
            }
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!(getWorld() instanceof ServerWorld sw)) return;
        if (!(source.getAttacker() instanceof ServerPlayerEntity killer)) return;
        if (sw.getRandom().nextFloat() < 0.30f && CECrystalItem.INSTANCE != null) {
            ItemStack drop = new ItemStack(CECrystalItem.INSTANCE, 1);
            if (!killer.getInventory().insertStack(drop)) {
                killer.dropItem(drop, false);
            }
        }
    }
}
