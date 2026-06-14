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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

// 두더지 주령. 4급. 지하 대기 → 플레이어 감지 시 지상 출현 → 근접 공격.
public class MoleEntity extends BaseCursedSpiritEntity {

    private static final float BASE_DAMAGE     = 20f;
    private static final int   DETECT_RANGE_SQ = 64;  // 8블록²
    private static final int   DETECT_PERIOD   = 20;  // 매 20틱 감지
    private static final int   BURROW_DEPTH    = 3;

    private boolean isUnderground = false;
    private double  surfaceY;
    private int     detectTick = 0;

    public MoleEntity(EntityType<? extends MoleEntity> type, World world) {
        super(type, world, CursedSpiritGrade.GRADE_4);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     CursedSpiritGrade.GRADE_4.hpMultiplier())
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  BASE_DAMAGE)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
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
        // 최초 스폰 시 지하로 이동
        if (age == 1) {
            surfaceY = getY();
            tryMoveToUnderground();
            isUnderground = true;
        }
        // 지하 대기 중: 플레이어 반경 8블록 감지
        if (isUnderground && ++detectTick >= DETECT_PERIOD) {
            detectTick = 0;
            if (!(getWorld() instanceof ServerWorld sw)) return;
            for (ServerPlayerEntity player : sw.getPlayers()) {
                if (squaredDistanceTo(player) <= DETECT_RANGE_SQ) {
                    isUnderground = false;
                    refreshPositionAndAngles(getX(), surfaceY, getZ(), getYaw(), getPitch());
                    break;
                }
            }
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (isUnderground) return false;
        if (target instanceof PlayerEntity player) {
            applyDamageToPlayer(player, BASE_DAMAGE);
            return true;
        }
        return super.tryAttack(target);
    }

    private void tryMoveToUnderground() {
        refreshPositionAndAngles(getX(), getY() - BURROW_DEPTH, getZ(), getYaw(), getPitch());
    }
}
