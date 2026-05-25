package com.jjk.combat;

import net.minecraft.entity.LivingEntity;

// 吏쟊OCK: PvP 筌?max_hp ??0.40 ????륂뒄 癰궰野???DamageCalculatorTest????ｍ뜞 ??륁젟
public class TickDamageCap {

    private static final float CAP_RATIO = 0.40f;

    public float apply(float damage, LivingEntity target) {
        float maxHp = target.getMaxHealth();
        float cap = maxHp * CAP_RATIO;
        return Math.min(damage, cap);
    }
}
