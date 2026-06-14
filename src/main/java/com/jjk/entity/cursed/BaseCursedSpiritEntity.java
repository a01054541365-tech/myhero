package com.jjk.entity.cursed;

import com.jjk.api.combat.IDamageSource;
import com.jjk.combat.DamageCalculator;
import com.jjk.combat.DamageContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public abstract class BaseCursedSpiritEntity extends PathAwareEntity {

    protected final CursedSpiritGrade grade;
    protected float ceLevel;
    protected boolean isAggressive;

    protected BaseCursedSpiritEntity(EntityType<? extends BaseCursedSpiritEntity> type,
                                      World world, CursedSpiritGrade grade) {
        super(type, world);
        this.grade = grade;
        this.ceLevel = 0f;
        this.isAggressive = false;
        initEntityGoals();
    }

    @Override
    protected final void initGoals() {
        // Grade is null when called from MobEntity constructor before this.grade is assigned.
        // Actual goal registration happens in initEntityGoals() after grade is set.
    }

    protected void initEntityGoals() { /* subclasses override */ }

    protected void applyDamageToPlayer(PlayerEntity target, float baseDamage) {
        float scaled = baseDamage * grade.damageMultiplier();
        if (!(target instanceof ServerPlayerEntity spe)) {
            target.damage(getDamageSources().mobAttack(this), scaled);
            return;
        }
        DamageContext ctx = DamageContext
                .builder((ServerPlayerEntity) null, spe, IDamageSource.NORMAL_TECHNIQUE, scaled)
                .build();
        float damage = new DamageCalculator().calculate(ctx);
        spe.damage(getDamageSources().mobAttack(this), damage);
    }

    public void onHitByPlayer(PlayerEntity attacker) {
        isAggressive = true;
    }

    public CursedSpiritGrade getCursedSpiritGrade() {
        return grade;
    }
}
