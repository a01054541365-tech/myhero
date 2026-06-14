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
import net.minecraft.world.World;

// 주력 흡수 주령 (오리지널). 2급. 근접 시 CE -100 흡수 → 자가 HP +20 회복.
public class CursedEnergyAbsorberEntity extends BaseCursedSpiritEntity {

    private static final float BASE_DAMAGE    = 30f;
    private static final float CE_ABSORB      = 100f;
    private static final float HEAL_ON_ABSORB = 20f;

    public CursedEnergyAbsorberEntity(EntityType<? extends CursedEnergyAbsorberEntity> type,
                                       World world) {
        super(type, world, CursedSpiritGrade.GRADE_2);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     CursedSpiritGrade.GRADE_2.hpMultiplier())
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  BASE_DAMAGE)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
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
        if (target instanceof ServerPlayerEntity spe) {
            // CE 흡수 시도 → 성공 시 자가 회복, 실패 시 회복 없음
            boolean absorbed = JJKMod.getCEManager().consume(spe, CE_ABSORB);
            if (absorbed) {
                heal(HEAL_ON_ABSORB);
            }
            applyDamageToPlayer(spe, BASE_DAMAGE);
            return true;
        }
        if (target instanceof PlayerEntity player) {
            applyDamageToPlayer(player, BASE_DAMAGE);
            return true;
        }
        return super.tryAttack(target);
    }
}
