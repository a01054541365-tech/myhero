package com.jjk.entity.cursed;

import com.jjk.entity.JJKEntities;
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
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

// 분열 주령 (오리지널). 2급. 사망 시 1세대(isSplit=false)이면 2세대 분열체 2마리 스폰.
public class SplittingCursedSpiritEntity extends BaseCursedSpiritEntity {

    private static final float BASE_DAMAGE_1ST  = 35f;
    private static final float BASE_DAMAGE_2ND  = 17f;
    private static final float SPLIT_HP_RATIO   = 0.5f;
    private static final int   SPLIT_COUNT      = 2;

    private boolean isSplit;  // true = 2세대 분열체

    public SplittingCursedSpiritEntity(EntityType<? extends SplittingCursedSpiritEntity> type,
                                        World world) {
        super(type, world, CursedSpiritGrade.GRADE_2);
        this.isSplit = false;
    }

    public void markAsSplit() {
        this.isSplit = true;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     CursedSpiritGrade.GRADE_2.hpMultiplier())
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  BASE_DAMAGE_1ST)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   20.0);
    }

    @Override
    protected void initEntityGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        goalSelector.add(9, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (target instanceof PlayerEntity player) {
            float dmg = isSplit ? BASE_DAMAGE_2ND : BASE_DAMAGE_1ST;
            applyDamageToPlayer(player, dmg);
            return true;
        }
        return super.tryAttack(target);
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (isSplit) return; // 2세대는 분열하지 않음
        if (!(getWorld() instanceof ServerWorld sw)) return;
        for (int i = 0; i < SPLIT_COUNT; i++) {
            SplittingCursedSpiritEntity child = JJKEntities.SPLITTING_CURSED_SPIRIT.create(sw);
            if (child == null) continue;
            child.markAsSplit();
            child.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), getPitch());
            child.setHealth(child.getMaxHealth() * SPLIT_HP_RATIO);
            sw.spawnEntity(child);
        }
    }
}
