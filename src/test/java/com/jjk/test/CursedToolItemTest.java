package com.jjk.test;

import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import com.jjk.grade.GradeManager;
import com.jjk.item.CursedToolEffect;
import com.jjk.item.CursedToolItem;
import com.jjk.item.CursedToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CursedToolItemTest {

    // ── 1. 저주 단검 공격력 +8% ───────────────────────────────────
    @Test
    void dagger_attackBonus_008() {
        CursedToolEffect e = CursedToolRegistry.getEffect("cursed_dagger");
        assertEquals(0.08f, e.attackBonus(), 0.001f,
            "저주 단검 attackBonus = 0.08");
    }

    // ── 2. 천호창 CE 소모 -8% ────────────────────────────────────
    @Test
    void spear_ceReduction_008() {
        CursedToolEffect e = CursedToolRegistry.getEffect("thousand_spear");
        assertEquals(0.08f, e.ceReduction(), 0.001f,
            "천호창 ceReduction = 0.08");
    }

    // ── 3. 유운 rangeBonus +12% ──────────────────────────────────
    @Test
    void cloud_rangeBonus_012() {
        CursedToolEffect e = CursedToolRegistry.getEffect("playful_cloud");
        assertEquals(0.12f, e.rangeBonus(), 0.001f,
            "유운 rangeBonus = 0.12");
    }

    // ── 4. 천역모 방어 관통 15% ──────────────────────────────────
    @Test
    void inverted_defPenetration_015() {
        CursedToolEffect e = CursedToolRegistry.getEffect("inverted_spear");
        assertEquals(0.15f, e.defPenetration(), 0.001f,
            "천역모 defPenetration = 0.15");
        assertEquals(120, e.sealCooldownTicks(),
            "천역모 sealCooldownTicks = 120");
    }

    // ── 5. 석혼도 isSoulDirect 강제 ──────────────────────────────
    @Test
    void soul_forceSoulDirect_true() {
        CursedToolEffect e = CursedToolRegistry.getEffect("split_soul_blade");
        assertTrue(e.forceSoulDirect(), "석혼도 forceSoulDirect = true");
        assertEquals(5, e.blackFlashBonus(), "석혼도 blackFlashBonus = 5");
    }

    // ── 6. 등급 미달 → isEligible=false ─────────────────────────
    @Test
    void grade4_insufficient_for_invertedSpear() {
        CursedToolEffect invertedEffect = CursedToolRegistry.getEffect("inverted_spear");
        // inverted_spear requires SPECIAL(rank=5)
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.grade = "4급";
        assertFalse(CursedToolItem.isEligible(data, invertedEffect),
            "4급 플레이어는 천역모 장착 불가");
    }

    // ── 7. 등급 충족 → isEligible=true ──────────────────────────
    @Test
    void specialGrade_eligible_for_invertedSpear() {
        CursedToolEffect invertedEffect = CursedToolRegistry.getEffect("inverted_spear");
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.grade = "특급";
        assertTrue(CursedToolItem.isEligible(data, invertedEffect),
            "특급 플레이어는 천역모 장착 가능");
        // 누구나 사용 가능한 저주 단검
        CursedToolEffect daggerEffect = CursedToolRegistry.getEffect("cursed_dagger");
        assertTrue(CursedToolItem.isEligible(data, daggerEffect),
            "특급 플레이어는 저주 단검 장착 가능");
    }

    // ── 8. 데미지 공식: baseDamage × (1 + attackBonus) ──────────
    @Test
    void dagger_damageFormula_plus8percent() {
        float base = 100f;
        CursedToolEffect e = CursedToolRegistry.getEffect("cursed_dagger");
        float result = base * (1.0f + e.attackBonus());
        assertEquals(108f, result, 0.01f,
            "저주 단검 데미지 공식: 100 × 1.08 = 108");
    }
}
