package com.jjk.entity.cursed;

import com.jjk.JJKMod;
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

// 포창할멈. 특급. 반경 50블록 CE 재생 -50% 디버프 + 근접 공격.
public class SmallpoxDeityEntity extends BaseCursedSpiritEntity {

    private static final float  BASE_DAMAGE        =  90f;
    private static final int    DEBUFF_RADIUS_SQ   = 2500; // 50블록²
    private static final int    DEBUFF_APPLY_PERIOD =  60; // 3초마다
    private static final int    DEBUFF_DURATION     = 120; // 6초 지속

    public SmallpoxDeityEntity(EntityType<? extends SmallpoxDeityEntity> type, World world) {
        super(type, world, CursedSpiritGrade.SPECIAL_GRADE);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     CursedSpiritGrade.SPECIAL_GRADE.hpMultiplier())
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  BASE_DAMAGE)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   30.0);
    }

    @Override
    protected void initEntityGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        goalSelector.add(9, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient()) return;
        // 반경 50블록 내 플레이어 CE 재생 -50% 디버프 (매 DEBUFF_APPLY_PERIOD 틱)
        if (age % DEBUFF_APPLY_PERIOD == 0 && JJKMod.getInstance() != null
                && getWorld() instanceof ServerWorld sw) {
            for (ServerPlayerEntity player : sw.getPlayers()) {
                if (squaredDistanceTo(player) <= DEBUFF_RADIUS_SQ) {
                    JJKMod.getCEManager().applyCeRegenDebuff(player, DEBUFF_DURATION);
                }
            }
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
