package com.jjk.test;

import com.jjk.entity.CursedSpiritGrade;
import com.jjk.entity.CursedSpiritGrade.AiTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CursedSpiritEntityTest {

    // ── 1. GRADE_4 속성값 확인 ───────────────────────────────────
    @Test
    void grade4_attributes() {
        CursedSpiritGrade g = CursedSpiritGrade.GRADE_4;
        assertEquals(15f,   g.maxHp,         0.001f, "GRADE_4 HP = 15");
        assertEquals(4f,    g.attackDamage,   0.001f, "GRADE_4 ATK = 4");
        assertEquals(0.25f, g.movementSpeed,  0.001f, "GRADE_4 SPEED = 0.25");
        assertEquals(12,    g.detectionRange,         "GRADE_4 RANGE = 12");
        assertEquals(10,    g.xpDrop,                 "GRADE_4 XP = 10");
    }

    // ── 2. GRADE_4 AI Tier: BASIC ────────────────────────────────
    @Test
    void grade4_aiTier_basic() {
        assertEquals(AiTier.BASIC, CursedSpiritGrade.GRADE_4.aiTier,
            "GRADE_4 AI = BASIC (근접만)");
    }

    // ── 3. GRADE_3 AI Tier: RANGED ───────────────────────────────
    @Test
    void grade3_aiTier_ranged() {
        assertEquals(AiTier.RANGED, CursedSpiritGrade.GRADE_3.aiTier,
            "GRADE_3 AI = RANGED (원거리 CE 투사체)");
    }

    // ── 4. GRADE_1 AI Tier: SKILLED ──────────────────────────────
    @Test
    void grade1_aiTier_skilled() {
        assertEquals(AiTier.SKILLED, CursedSpiritGrade.GRADE_1.aiTier,
            "GRADE_1 AI = SKILLED (원거리+회피+CE 폭발)");
    }

    // ── 5. SPECIAL AI Tier: BOSS ─────────────────────────────────
    @Test
    void special_aiTier_boss() {
        assertEquals(AiTier.BOSS, CursedSpiritGrade.SPECIAL.aiTier,
            "특급 AI = BOSS (전 패턴 + 영역 전개)");
    }

    // ── 6. 모든 등급 xpDrop 값 확인 ──────────────────────────────
    @Test
    void allGrades_xpDrop_values() {
        assertEquals(10,  CursedSpiritGrade.GRADE_4.xpDrop, "4급 XP = 10");
        assertEquals(25,  CursedSpiritGrade.GRADE_3.xpDrop, "3급 XP = 25");
        assertEquals(60,  CursedSpiritGrade.GRADE_2.xpDrop, "2급 XP = 60");
        assertEquals(150, CursedSpiritGrade.GRADE_1.xpDrop, "1급 XP = 150");
        assertEquals(400, CursedSpiritGrade.SPECIAL.xpDrop, "특급 XP = 400");
    }

    // ── 7. 특급 onDeath xpDrop = 400 ─────────────────────────────
    @Test
    void special_xpDrop_400() {
        assertEquals(400, CursedSpiritGrade.SPECIAL.xpDrop,
            "특급 주령 처치 시 xpDrop = 400");
        assertEquals(200f, CursedSpiritGrade.SPECIAL.maxHp,   0.001f, "특급 HP = 200");
        assertEquals(48f,  CursedSpiritGrade.SPECIAL.attackDamage, 0.001f, "특급 ATK = 48");
        assertEquals(0.35f,CursedSpiritGrade.SPECIAL.movementSpeed, 0.001f, "특급 SPEED = 0.35");
    }
}
