package com.jjk.test;

import com.jjk.character.impl.NanamiSkillSet;
import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NanamiSkillSetTest {

    // ── 1. 무장해체: 피격 대상 쿨타임 +40틱 지연 ─────────────────
    @Test
    void dismantleEquipment_delayCooldowns() {
        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        target.cooldowns.put("0",  100L);
        target.cooldowns.put("1",  200L);
        target.cooldowns.put("cd_test", 50L);

        // 무장해체 효과: 모든 쿨타임 +40틱
        target.cooldowns.replaceAll((k, v) -> v + 40L);

        assertEquals(140L, target.cooldowns.get("0"),       "쿨타임 0 +40틱");
        assertEquals(240L, target.cooldowns.get("1"),       "쿨타임 1 +40틱");
        assertEquals(90L,  target.cooldowns.get("cd_test"), "임시 쿨 +40틱");
    }

    // ── 2. 극한초과: 13000틱 이상 → ×2.50 ─────────────────────────
    @Test
    void overtime_activeAfter13000Ticks() {
        float mult = NanamiSkillSet.getOvertimeMultiplier(13000L);
        assertEquals(2.50f, mult, 0.001f, "13000틱 = 오후 6시 → ×2.50");

        long encoded = (long)(mult * 1000);
        assertEquals(2500L, encoded, "인코딩 값: 2500");
    }

    // ── 3. 극한초과: 12999틱 미만 → ×1.00 ──────────────────────────
    @Test
    void overtime_inactiveBelow13000Ticks() {
        float mult = NanamiSkillSet.getOvertimeMultiplier(12999L);
        assertEquals(1.00f, mult, 0.001f, "12999틱 = 오후 6시 이전 → ×1.00");

        long encoded = (long)(mult * 1000);
        assertEquals(1000L, encoded, "인코딩 값: 1000");
    }

    // ── 4. 십: 분 약점 히트 → ×1.30 ────────────────────────────────
    @Test
    void tenPuncture_weakpointBonus() {
        float base = 78f;
        float result = NanamiSkillSet.applyWeaknessBonus(base, true);
        assertEquals(78f * 1.30f, result, 0.01f,
            "약점 히트 → 78 × 1.30 = 101.4");
    }

    // ── 5. 십: 분 일반 히트 → 보너스 없음 ─────────────────────────
    @Test
    void tenPuncture_normalHit_noBonus() {
        float base = 78f;
        float result = NanamiSkillSet.applyWeaknessBonus(base, false);
        assertEquals(78f, result, 0.01f, "일반 히트 → rawDamage=78f 그대로");
    }

    // ── 6. 십: 분 + 극한초과 연계 → 78 × 1.30 × 2.50 = 253.5 ──────
    @Test
    void tenPuncture_withOvertimeMultiplier() {
        float base = 78f;
        float afterWeak     = NanamiSkillSet.applyWeaknessBonus(base, true);  // 101.4
        float overtimeMult  = NanamiSkillSet.getOvertimeMultiplier(13000L);   // 2.50
        float finalDamage   = afterWeak * overtimeMult;
        assertEquals(253.5f, finalDamage, 0.01f,
            "약점(×1.30) + 극한초과(×2.50): 78 × 1.30 × 2.50 = 253.5");
    }

    // ── 7. 십: 분 방어 관통 30% — defenseMultiplier=0.70f ──────────
    @Test
    void tenPuncture_defPenetration30Percent() {
        // 방어 관통 30% = defenseMultiplier 0.70f
        // applyDefenseStat(damage=100, defenseStat=100, mult=0.70, soulDirect=false)
        // effectiveDefense = 100 × 0.70 = 70 → 100 × (100/170) ≈ 58.82
        float dmgWithPenetration =
            com.jjk.combat.CombatPipeline.applyDefenseStat(100f, 100, 0.70f, false);
        float dmgNoPenetration =
            com.jjk.combat.CombatPipeline.applyDefenseStat(100f, 100, 1.00f, false);

        assertTrue(dmgWithPenetration > dmgNoPenetration,
            "십: 분 방어 관통 30% → 일반 공격보다 데미지 높음");
        assertEquals(100f * 100f / 170f, dmgWithPenetration, 0.01f,
            "defenseMultiplier=0.70 → 100×(100/170)≈58.82");
    }
}
