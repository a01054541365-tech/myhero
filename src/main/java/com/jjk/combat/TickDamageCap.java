package com.jjk.combat;

import com.jjk.JJKMod;
import net.minecraft.entity.LivingEntity;

// §LOCK: PvP 상한 max_hp × 0.40 — DamageCalculatorTest 수치 보호
public class TickDamageCap {

    static final float CAP_RATIO = 0.40f;

    // §LOCK: capRatio는 config.pvpDamageCapMaxHpRatio 전달 필수, 0.40f 하드코딩 금지
    public static float apply(float damage, float targetHpMax, float capRatio) {
        return Math.min(damage, targetHpMax * capRatio);
    }

    public float apply(float damage, LivingEntity target) {
        float ratio = (JJKMod.getInstance() != null)
                ? JJKMod.getConfig().pvpDamageCapMaxHpRatio
                : CAP_RATIO;
        return apply(damage, target.getMaxHealth(), ratio);
    }
}
