package com.jjk.entity.cursed;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.List;

// 蠅頭 (승두 — 파리머리). 4급 이하. 밟히면 사라지는 무해한 주령.
public class SyoutoEntity extends BaseCursedSpiritEntity {

    private static final float LOOK_RANGE = 4.0f;

    public SyoutoEntity(EntityType<? extends SyoutoEntity> type, World world) {
        super(type, world, CursedSpiritGrade.GRADE_4_BELOW);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     CursedSpiritGrade.GRADE_4_BELOW.hpMultiplier())
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  0.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.20)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   8.0);
    }

    @Override
    protected void initEntityGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, LOOK_RANGE));
        goalSelector.add(8, new LookAroundGoal(this));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient()) return;
        // 밟히면 사라짐: 바운딩 박스와 겹치는 다른 엔티티 감지
        List<Entity> colliding = getWorld().getOtherEntities(this, getBoundingBox(), Entity::isAlive);
        if (!colliding.isEmpty()) {
            discard();
        }
    }

    @Override
    protected int getXpToDrop() { return 0; }
}
