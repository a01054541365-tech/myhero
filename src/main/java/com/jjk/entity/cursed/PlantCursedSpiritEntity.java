package com.jjk.entity.cursed;

import com.jjk.JJKMod;
import com.jjk.effect.EffectManager;
import com.jjk.effect.EffectType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

// 하나미 계열 식물 주령. 1급. 근접 명중 시 STUN + DoT (10/틱 × 5틱).
public class PlantCursedSpiritEntity extends BaseCursedSpiritEntity {

    private static final float BASE_DAMAGE     =  45f;
    private static final int   STUN_TICKS      =  40;  // 2초
    private static final float DOT_PER_TICK    =  10f;
    private static final int   DOT_COUNT       =   5;

    public PlantCursedSpiritEntity(EntityType<? extends PlantCursedSpiritEntity> type, World world) {
        super(type, world, CursedSpiritGrade.GRADE_1);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     CursedSpiritGrade.GRADE_1.hpMultiplier())
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  BASE_DAMAGE)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   24.0);
    }

    @Override
    protected void initEntityGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        goalSelector.add(9, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (target instanceof PlayerEntity player) {
            applyDamageToPlayer(player, BASE_DAMAGE);
            if (!getWorld().isClient() && player instanceof ServerPlayerEntity spe
                    && getWorld() instanceof ServerWorld sw) {
                EffectManager.apply(spe, EffectType.STUN, STUN_TICKS);
                long now = sw.getTime();
                for (int i = 1; i <= DOT_COUNT; i++) {
                    JJKMod.getEffectDeferQueue().schedule(
                        player.getBlockPos(), DOT_PER_TICK, i, null, now);
                }
            }
            return true;
        }
        return super.tryAttack(target);
    }
}
