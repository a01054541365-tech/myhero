package com.jjk.combat;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.finger.FingerSystem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// §LOCK: 흑섬 Perfect×2.5 / Great×2.0, finalMult ×4.0 — DamageCalculatorTest 건드리지 말 것
public class DamageCalculator {

    private static final float BLACK_FLASH_PERFECT = 2.5f;
    private static final float BLACK_FLASH_GREAT   = 2.0f;
    private static final float MULTI_HIT_2ND       = 0.85f;
    private static final float MULTI_HIT_3RD_PLUS  = 0.70f;
    private static final float AWAKENING_MULT       = 1.25f;
    private static final float MAX_FINAL_MULT       = 4.0f;
    private static final float TICK_CAP_RATIO       = 0.50f;

    private final Map<UUID, TickDamageTracker> tickAccum = new HashMap<>();

    private record TickDamageTracker(long tick, float accumulated) {}

    public float calculate(DamageContext ctx) {
        float damage = ctx.baseDamage;

        // §LOCK: 흑섬 Perfect×2.5 / Great×2.0
        if (ctx.isBlackFlash) {
            damage *= ctx.blackFlashPerfect ? BLACK_FLASH_PERFECT : BLACK_FLASH_GREAT;
        }

        // 다단히트 감쇠: 2타×0.85, 3타+×0.70
        if (ctx.hitIndex == 1) {
            damage *= MULTI_HIT_2ND;
        } else if (ctx.hitIndex >= 2) {
            damage *= MULTI_HIT_3RD_PLUS;
        }

        boolean modEnabled = JJKMod.getInstance() != null;

        // Jogo passive: fire skill immunity (target=jogo)
        if (modEnabled && ctx.skillName != null && isFireSkill(ctx.skillName)) {
            if (ctx.target != null && "jogo".equals(getCharId(ctx.target))) {
                return 0f;
            }
            // rain penalty moved to JogoSkillSet (applied via externalBuffMult)
        }

        // Soul resist (Mahito R skill) + Mahito soul-direct vulnerability (passive)
        if (modEnabled && ctx.isSoulDirect && ctx.target != null) {
            PlayerData targetData = JJKMod.getPlayerRepository().load(ctx.target.getUuid());
            long now = ctx.target.getWorld().getTime();
            Long soulResist = targetData.cooldowns.get("status_soul_resist");
            if (soulResist != null && now < soulResist) {
                damage *= 0.5f;
            }
            if ("mahito".equals(targetData.characterId)) {
                damage *= 1.2f;
            }
        }

        // Mahito passive: +20% damage from gojo/itadori techniques
        if (modEnabled && ctx.attacker != null && ctx.target != null
                && "mahito".equals(getCharId(ctx.target))) {
            String attackerChar = getCharId(ctx.attacker);
            if ("gojo".equals(attackerChar) || "itadori".equals(attackerChar)) {
                damage *= 1.2f;
            }
        }

        // 스쿠나 손가락 보정: mastery + fingerBonus, §LOCK 합산 상한 30%
        if (modEnabled && ctx.attacker != null
                && "sukuna".equals(getCharId(ctx.attacker))) {
            PlayerData attackerData =
                JJKMod.getPlayerRepository().load(ctx.attacker.getUuid());
            FingerSystem fs = JJKMod.getFingerSystem();
            float mastery = attackerData.mastery / 100f;
            float fingerBonus = fs.getFingerSkillDmgBonus(attackerData.fingerCount);
            // §LOCK: masteryBonus 합산 상한 30%
            float effectiveBonus = Math.min(mastery + fingerBonus, 0.30f);
            damage *= (1f + effectiveBonus);
        }

        // External buff multiplier (Nanami overtime_work, Jogo rain debuff, etc.)
        damage *= ctx.externalBuffMult;

        // 각성 데미지 배율 ×1.25
        if (modEnabled && ctx.attacker != null) {
            PlayerData attackerData = JJKMod.getPlayerRepository().load(ctx.attacker.getUuid());
            if (attackerData.awakeningActive) {
                damage *= AWAKENING_MULT;
            }
        }

        if (modEnabled && ctx.attacker != null
                && "nanami".equals(getCharId(ctx.attacker))) {
            PlayerData d = JJKMod.getPlayerRepository().load(ctx.attacker.getUuid());
            if (d.overtimeWork) damage *= 1.20f;
        }

        // §LOCK: finalMultiplier 클램프 ×4.0
        if (ctx.baseDamage > 0) {
            damage = Math.min(damage, ctx.baseDamage * MAX_FINAL_MULT);
        }

        // 동일 틱 누적 캡 max_hp×0.50 — PvP(플레이어 타겟)에만 적용
        if (ctx.target instanceof ServerPlayerEntity && ctx.target != null) {
            damage = applyTickCap(ctx, damage);
        }

        return damage;
    }

    private float applyTickCap(DamageContext ctx, float damage) {
        UUID targetId = ctx.target.getUuid();
        long tick = ctx.target.getWorld().getTime();
        float cap = ctx.target.getMaxHealth() * TICK_CAP_RATIO;

        TickDamageTracker tracker = tickAccum.get(targetId);
        if (tracker == null || tracker.tick() != tick) {
            tracker = new TickDamageTracker(tick, 0f);
        }

        float remaining = cap - tracker.accumulated();
        if (remaining <= 0f) return 0f;
        damage = Math.min(damage, remaining);
        tickAccum.put(targetId, new TickDamageTracker(tick, tracker.accumulated() + damage));
        return damage;
    }

    private static boolean isFireSkill(String skillName) {
        return skillName.contains("fire") || skillName.contains("burn")
                || skillName.contains("flame") || skillName.contains("volcanic")
                || skillName.contains("coffin") || skillName.contains("ring_of")
                || skillName.contains("volcano") || skillName.contains("meteor");
    }

    private static String getCharId(LivingEntity entity) {
        if (entity == null) return null;
        return JJKMod.getPlayerRepository().load(entity.getUuid()).characterId;
    }
}
