package com.jjk.entity;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.entity.ai.CursedSpiritGoals;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class CursedSpiritEntity extends HostileEntity {

    private final CursedSpiritGrade grade;

    public CursedSpiritEntity(EntityType<? extends CursedSpiritEntity> type,
                               World world, CursedSpiritGrade grade) {
        super(type, world);
        this.grade = grade;
    }

    public static DefaultAttributeContainer.Builder createAttributes(CursedSpiritGrade grade) {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     grade.maxHp)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  grade.attackDamage)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, grade.movementSpeed)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   grade.detectionRange);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, grade.detectionRange));
        goalSelector.add(9, new LookAroundGoal(this));

        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));

        addGradeSpecificGoals();
    }

    private void addGradeSpecificGoals() {
        switch (grade) {
            case GRADE_3 -> goalSelector.add(3,
                new CursedSpiritGoals.RangedCeAttackGoal(this, 20, 12f));
            case GRADE_2 -> {
                goalSelector.add(3, new CursedSpiritGoals.RangedCeAttackGoal(this, 16, 15f));
                goalSelector.add(4, new CursedSpiritGoals.EvasiveGoal(this, 0.5f));
            }
            case GRADE_1 -> {
                goalSelector.add(3, new CursedSpiritGoals.RangedCeAttackGoal(this, 12, 18f));
                goalSelector.add(4, new CursedSpiritGoals.EvasiveGoal(this, 0.6f));
                goalSelector.add(5, new CursedSpiritGoals.CeBurstGoal(this, 60, 25f));
            }
            case SPECIAL -> {
                goalSelector.add(3, new CursedSpiritGoals.RangedCeAttackGoal(this, 10, 22f));
                goalSelector.add(4, new CursedSpiritGoals.EvasiveGoal(this, 0.7f));
                goalSelector.add(5, new CursedSpiritGoals.CeBurstGoal(this, 40, 35f));
                goalSelector.add(6, new CursedSpiritGoals.DomainDeployGoal(this, 1200));
            }
            default -> { /* GRADE_4: 기본 근접만 */ }
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        // 사망 시 활성 영역 해제
        JJKMod.getDomainManager().collapseDomain(getUuid(), getWorld().getTime());
        if (source.getAttacker() instanceof ServerPlayerEntity killer) {
            PlayerData killerData = JJKMod.getPlayerRepository().load(killer.getUuid());
            JJKMod.getGradeManager().addXp(killerData, grade.xpDrop, killer);
        }
    }

    @Override
    protected int getXpToDrop() { return grade.xpDrop; }

    public CursedSpiritGrade getGrade() { return grade; }
}
