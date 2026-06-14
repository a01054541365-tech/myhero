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
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

// 다곤 계열 수중 주령. 준특급. 수중 1.5×/지상 0.7× 이동속도. 사망 시 반경 8블록 폭발.
public class WaterCursedSpiritEntity extends BaseCursedSpiritEntity {

    private static final float BASE_DAMAGE         =  75f;
    private static final float BASE_MOVE_SPEED     =  0.28f;
    private static final float WATER_SPEED_MULT    =  1.5f;
    private static final float LAND_SPEED_MULT     =  0.7f;
    private static final float DEATH_DAMAGE        =  35f;
    private static final double DEATH_RADIUS       =  8.0;

    public WaterCursedSpiritEntity(EntityType<? extends WaterCursedSpiritEntity> type, World world) {
        super(type, world, CursedSpiritGrade.SEMI_SPECIAL);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     CursedSpiritGrade.SEMI_SPECIAL.hpMultiplier())
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  BASE_DAMAGE)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, BASE_MOVE_SPEED)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   27.0);
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
        EntityAttributeInstance speedAttr = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            float mult = isTouchingWater() ? WATER_SPEED_MULT : LAND_SPEED_MULT;
            speedAttr.setBaseValue(BASE_MOVE_SPEED * mult);
        }
    }

    @Override
    public boolean canSpawn(WorldView world) {
        return world.getFluidState(getBlockPos()).isOf(Fluids.WATER)
            || world.getFluidState(getBlockPos().down()).isOf(Fluids.WATER);
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (target instanceof PlayerEntity player) {
            applyDamageToPlayer(player, BASE_DAMAGE);
            return true;
        }
        return super.tryAttack(target);
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!(getWorld() instanceof ServerWorld sw)) return;
        // 반경 8블록 전원에게 35 데미지 — EffectDeferQueue 사용 (1틱 지연, null attacker)
        JJKMod.getEffectDeferQueue().schedule(
            getBlockPos(), DEATH_DAMAGE, 1, null, sw.getTime(), DEATH_RADIUS);
    }
}
