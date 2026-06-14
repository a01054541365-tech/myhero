package com.jjk.entity.cursed;

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
import net.minecraft.world.LightType;
import net.minecraft.world.World;

// 그림자 주령 (오리지널). 3급. 밝기 레벨 7 이하인 위치에서만 이동 가능.
public class ShadowCursedSpiritEntity extends BaseCursedSpiritEntity {

    private static final float BASE_DAMAGE   = 28f;
    private static final int   LIGHT_MAX     =  7;

    public ShadowCursedSpiritEntity(EntityType<? extends ShadowCursedSpiritEntity> type, World world) {
        super(type, world, CursedSpiritGrade.GRADE_3);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     CursedSpiritGrade.GRADE_3.hpMultiplier())
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  BASE_DAMAGE)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   16.0);
    }

    @Override
    protected void initEntityGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(9, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient()) return;
        // 밝기 레벨 7 초과 위치에서는 이동 중지
        if (getWorld().getLightLevel(LightType.BLOCK, getBlockPos()) > LIGHT_MAX) {
            getNavigation().stop();
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (target instanceof PlayerEntity player) {
            applyDamageToPlayer(player, BASE_DAMAGE);
            return true;
        }
        return super.tryAttack(target);
    }
}
