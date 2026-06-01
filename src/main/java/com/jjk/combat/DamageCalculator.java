package com.jjk.combat;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
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
        float cap = ctx.target.getMaxHealth() * TickDamageCap.CAP_RATIO;

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

    // ─── Pure-data calculate for testable pipeline (§4 formula) ─────────────

    // 각성 배율: config.awakeningMultiplier() 참조 (기본 1.5f)
    private static final float COND_ZONE_PENALTY = 0.5f;
    private static final float CLAMP_MAX = 4.0f;         // §LOCK
    private static final float CLAMP_MIN = 0.25f;        // §LOCK
    private static final float BF_MULTIPLIER = 2.5f;     // §LOCK: 흑섬 base × 2.5

    // Q3 콤보 레벨별 데미지 배율 (index 0 = Lv.1)
    private static final float[] COMBO_DMG =
        {1.00f,1.02f,1.04f,1.06f,1.08f,1.10f,1.13f,1.15f,1.17f,1.20f};

    /**
     * §4 공식 기반 순수 PlayerData 계산. 테스트 및 processData에서 호출.
     * ctx.isBlackFlash=true → BF 배율 적용 (판정은 CombatPipeline 5단계에서 수행).
     * ctx.rawDamage / ctx.finalDamage 에 계산 결과를 기록.
     */
    public float calculatePure(DamageContext ctx, PlayerData attacker, PlayerData target,
                                float baseDamage, com.jjk.JjkConfig config, long currentTick) {
        // 흑섬 배율 (판정은 외부에서, 적용만 여기)
        float effectiveBase = (ctx != null && ctx.isBlackFlash)
                ? baseDamage * BF_MULTIPLIER  // §LOCK
                : baseDamage;

        // §4-1: attackMultiplier
        float burstBonus = (attacker != null && attacker.burstActive) ? 1.30f : 1.0f;
        int attackStat = attacker != null ? attacker.attackStat : 0;
        float attackMult = 1f + Math.min(attackStat * burstBonus, 120f) / 100f;

        // 스쿠나 손가락 보너스: 1개당 fingerStatBonusPercent% (기본 5%)
        if (attacker != null && "sukuna".equals(attacker.characterId) && attacker.fingerCount > 0) {
            int bonusPct = (config != null) ? config.fingerStatBonusPercent() : 5;
            attackMult *= (1.0f + attacker.fingerCount * bonusPct / 100.0f);
        }

        // §4-2: gradeMultiplier — PvP(ServerPlayerEntity + gradePvpScaling) 이외는 pveGradeMultiplier 적용
        boolean isPvP = ctx != null && ctx.target instanceof ServerPlayerEntity;
        float gradeMult = (isPvP && config.gradePvpScaling)
                ? gradeToMultiplier(attacker != null ? attacker.grade : null)
                : config.pveGradeMultiplier;

        // §4-3: conditionMultiplier
        float awakeningMult = (config != null) ? config.awakeningMultiplier() : 1.5f;
        float condMult = 1.0f;
        if (attacker != null) {
            if (attacker.awakeningActive) condMult *= awakeningMult;
            if (attacker.zonePenaltyUntilTick > currentTick) condMult *= COND_ZONE_PENALTY;
        }

        // 콤보 배율 (Q3 확정 테이블)
        float comboMult = 1.0f;
        if (attacker != null && JJKMod.getInstance() != null) {
            int lv = JJKMod.getComboTracker().getComboLevel(attacker) - 1;
            if (lv >= 0 && lv < COMBO_DMG.length) comboMult = COMBO_DMG[lv];
        }

        // §4-6: 배율 클램프 ×0.25 ~ ×4.0 (§LOCK)
        float totalMult = Math.max(CLAMP_MIN, Math.min(attackMult * gradeMult * condMult * comboMult, CLAMP_MAX));

        float rawDamage = effectiveBase * totalMult;
        if (ctx != null) ctx.rawDamage = rawDamage;

        // §4-4: 방어 처리 (defenseMultiplier < 1.0 = 방어 관통)
        float effectiveDefense = (ctx != null && ctx.isSoulDirect)
                ? 0f
                : (target != null ? target.defenseStat : 0f)
                  * (ctx != null ? ctx.defenseMultiplier : 1.0f);
        float effectiveDamage = Math.max(0f, rawDamage - effectiveDefense);

        if (ctx != null) ctx.finalDamage = effectiveDamage;
        return effectiveDamage;
    }

    private static float gradeToMultiplier(String grade) {
        if (grade == null) return 1.0f;
        return switch (grade) {
            case "4급", "grade_4"       -> 1.00f;
            case "3급", "grade_3"       -> 1.15f;
            case "2급", "grade_2"       -> 1.30f;
            case "1급", "grade_1"       -> 1.50f;
            case "준특급", "semi_grade_1" -> 1.65f;
            case "특급", "special_grade" -> 1.80f;
            default                     -> 1.00f;
        };
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
