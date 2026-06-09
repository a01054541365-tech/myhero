package com.jjk.defense;

import com.jjk.JJKMod;
import com.jjk.combat.DamageContext;
import com.jjk.data.PlayerData;

import java.util.concurrent.ThreadLocalRandom;

public final class DefenseHandler {

    private DefenseHandler() {}

    /**
     * 수동 방어(shieldActive=true): 데미지 × (1 - shieldDamageReduction).
     * CE 소모는 CEManager.regenTick()에서 처리.
     */
    public static float applyShield(PlayerData target, float damage) {
        if (!target.shieldActive) return damage;
        float reduction = JJKMod.getConfig().shieldDamageReduction(); // 0.02
        return damage * (1.0f - reduction);
    }

    /**
     * 낙화의 정: 1급 이상 + simpleBarrierActive + ctx.nKeyApplied.
     * 80% 확률 완전 차단 + CE 소모.
     */
    public static float applyFallingBlossom(PlayerData target, DamageContext ctx, float damage) {
        if (!target.fallingBlossomActive) return damage;
        if (ctx == null || !ctx.nKeyApplied) return damage;

        boolean eligible = target.grade != null
                && target.grade.ordinal() >= com.jjk.data.Grade.GRADE_1.ordinal();
        if (!eligible || !target.simpleBarrierActive) return damage;

        float blockChance = JJKMod.getConfig().fallingBlossomSureHitBlock(); // 0.80
        if (ThreadLocalRandom.current().nextFloat() < blockChance) {
            float ceDrain = target.ceMax * JJKMod.getConfig().fallingBlossomCeDrain();
            target.ceCurrent = Math.max(0f, target.ceCurrent - ceDrain);
            return 0f;
        }
        return damage;
    }

    /**
     * 간이영역: simpleBarrierActive + ctx.nKeyApplied → 70% 확률 무효.
     */
    public static float applySimpleBarrier(PlayerData target, DamageContext ctx, float damage) {
        if (!target.simpleBarrierActive) return damage;
        if (ctx == null || !ctx.nKeyApplied) return damage;

        float negate = JJKMod.getConfig().simpleBarrierSureHitNegate(); // 0.70
        if (ThreadLocalRandom.current().nextFloat() < negate) {
            return 0f;
        }
        return damage;
    }
}
