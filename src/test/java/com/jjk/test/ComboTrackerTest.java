package com.jjk.test;

import com.jjk.zone.ComboTracker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// §LOCK 수치 보호: Just Frame 8~12틱 / 콤보 리셋 30틱
class ComboTrackerTest {

    @Test
    void testJustFrameWindowHit() {
        // attackTick=0, currentTick=8 → diff=8 → §LOCK 하한 8 이상 → true
        assertTrue(ComboTracker.checkBlackFlash(0L, 8L), "diff=8 → Just Frame 적중");
    }

    @Test
    void testJustFrameWindowHit2() {
        // attackTick=0, currentTick=12 → diff=12 → §LOCK 상한 12 이하 → true
        assertTrue(ComboTracker.checkBlackFlash(0L, 12L), "diff=12 → Just Frame 적중");
    }

    @Test
    void testJustFrameWindowMiss_TooEarly() {
        // attackTick=0, currentTick=7 → diff=7 < 8 → false
        assertFalse(ComboTracker.checkBlackFlash(0L, 7L), "diff=7 → Just Frame 미달");
    }

    @Test
    void testJustFrameWindowMiss_TooLate() {
        // attackTick=0, currentTick=13 → diff=13 > 12 → false
        assertFalse(ComboTracker.checkBlackFlash(0L, 13L), "diff=13 → Just Frame 초과");
    }

    @Test
    void testComboReset() {
        // §LOCK: 마지막 공격 후 30틱 초과 시 자동 리셋
        ComboTracker tracker = new ComboTracker();
        tracker.recordAttack(0L);
        assertEquals(0L, tracker.getLastAttackTick(), "recordAttack 후 lastAttackTick=0");

        // 30틱 — 아직 리셋 안 됨 (> 30 아님)
        tracker.tick(30L);
        assertEquals(0L, tracker.getLastAttackTick(), "30틱에는 아직 리셋 안 됨");

        // 31틱 — 30틱 초과 → 리셋
        tracker.tick(31L);
        assertEquals(-1L, tracker.getLastAttackTick(), "31틱 후 콤보 리셋 (§LOCK: 30틱)");
    }
}
