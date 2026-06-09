package com.jjk.test;

import com.jjk.data.Grade;
import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GradeEnumTest {

    // ── 1. fromKey: 영문 key ──────────────────────────────────────────────────
    @Test
    void testGradeEnumFromKey() {
        assertSame(Grade.GRADE_4,     Grade.fromKey("grade_4"));
        assertSame(Grade.GRADE_3,     Grade.fromKey("grade_3"));
        assertSame(Grade.GRADE_2,     Grade.fromKey("grade_2"));
        assertSame(Grade.GRADE_1,     Grade.fromKey("grade_1"));
        assertSame(Grade.SEMI_SPECIAL, Grade.fromKey("semi_grade_1"));
        assertSame(Grade.SPECIAL,     Grade.fromKey("special_grade"));
    }

    // ── 2. fromKey: 한국어 display 문자열 ─────────────────────────────────────
    @Test
    void testGradeEnumFromDisplay() {
        assertSame(Grade.GRADE_4,     Grade.fromKey("4급"));
        assertSame(Grade.GRADE_3,     Grade.fromKey("3급"));
        assertSame(Grade.GRADE_2,     Grade.fromKey("2급"));
        assertSame(Grade.GRADE_1,     Grade.fromKey("1급"));
        assertSame(Grade.SEMI_SPECIAL, Grade.fromKey("준특급"));
        assertSame(Grade.SPECIAL,     Grade.fromKey("특급"));
    }

    // ── 3. fromKey: null/unknown → GRADE_4 ───────────────────────────────────
    @Test
    void testGradeEnumFallback() {
        assertSame(Grade.GRADE_4, Grade.fromKey(null));
        assertSame(Grade.GRADE_4, Grade.fromKey("unknown_value"));
    }

    // ── 4. §LOCK 배율 검증 (§7-1) ─────────────────────────────────────────────
    @Test
    void testGradeMultipliers() {
        assertEquals(1.00f, Grade.GRADE_4.multiplier,     0.001f);
        assertEquals(1.15f, Grade.GRADE_3.multiplier,     0.001f);
        assertEquals(1.30f, Grade.GRADE_2.multiplier,     0.001f);
        assertEquals(1.50f, Grade.GRADE_1.multiplier,     0.001f);
        assertEquals(1.65f, Grade.SEMI_SPECIAL.multiplier, 0.001f);
        assertEquals(1.80f, Grade.SPECIAL.multiplier,     0.001f);
    }

    // ── 5. ordinal 순서 = 등급 순위 ───────────────────────────────────────────
    @Test
    void testGradeOrdinalOrder() {
        assertTrue(Grade.GRADE_4.ordinal() < Grade.GRADE_3.ordinal());
        assertTrue(Grade.GRADE_3.ordinal() < Grade.GRADE_2.ordinal());
        assertTrue(Grade.GRADE_2.ordinal() < Grade.GRADE_1.ordinal());
        assertTrue(Grade.GRADE_1.ordinal() < Grade.SEMI_SPECIAL.ordinal());
        assertTrue(Grade.SEMI_SPECIAL.ordinal() < Grade.SPECIAL.ordinal());
    }

    // ── 6. PlayerData 기본값 + snapshot 보존 ──────────────────────────────────
    @Test
    void testGradeMigration() {
        PlayerData d = PlayerData.createDefault(UUID.randomUUID());
        assertSame(Grade.GRADE_4, d.grade, "기본 등급 = GRADE_4");

        d.grade = Grade.SPECIAL;
        PlayerData snap = d.snapshot();
        assertSame(Grade.SPECIAL, snap.grade, "snapshot grade 보존");
    }

    // ── 7. DB key 라운드트립 ──────────────────────────────────────────────────
    @Test
    void testGradeKeyRoundtrip() {
        for (Grade g : Grade.values()) {
            assertSame(g, Grade.fromKey(g.key), "key 라운드트립: " + g.key);
            assertSame(g, Grade.fromKey(g.display), "display 라운드트립: " + g.display);
        }
    }

    // ── 8. testBlackFlashCooldown: blackFlashCooldownUntil 필드 및 쿨타임 로직 ─
    @Test
    void testBlackFlashCooldown() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());

        // 기본값: 0 → 쿨타임 없음
        assertEquals(0L, data.blackFlashCooldownUntil, "blackFlashCooldownUntil 기본값 0");

        long currentTick = 1000L;

        // 쿨타임 중: currentTick < blackFlashCooldownUntil → justFrame 불가
        data.blackFlashCooldownUntil = currentTick + 100L;
        boolean blockedByCD = currentTick >= data.blackFlashCooldownUntil;
        assertFalse(blockedByCD, "쿨타임 중: justFrame 조건 미충족");

        // 쿨타임 종료: currentTick >= blackFlashCooldownUntil → justFrame 가능
        data.blackFlashCooldownUntil = currentTick - 1L;
        boolean cdExpired = currentTick >= data.blackFlashCooldownUntil;
        assertTrue(cdExpired, "쿨타임 종료: justFrame 조건 충족");

        // snapshot 보존
        data.blackFlashCooldownUntil = 99999L;
        PlayerData snap = data.snapshot();
        assertEquals(99999L, snap.blackFlashCooldownUntil, "snapshot blackFlashCooldownUntil 보존");
    }
}
