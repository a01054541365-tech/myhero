package com.jjk.test;

import com.jjk.combat.ResonanceTracker;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResonanceTrackerTest {

    @Test
    void stack4_triggers_bonus_and_resets() {
        ResonanceTracker tracker = new ResonanceTracker();
        UUID target = UUID.randomUUID();
        String skill = "dismantle";
        float base = 100f;

        // 1~3회: 추가 데미지 없음
        assertEquals(0, tracker.checkAndApply(target, skill, base), "1회차 보너스 없음");
        assertEquals(0, tracker.checkAndApply(target, skill, base), "2회차 보너스 없음");
        assertEquals(0, tracker.checkAndApply(target, skill, base), "3회차 보너스 없음");

        // 4회: 보너스 반환 + 스택 초기화
        int bonus = tracker.checkAndApply(target, skill, base);
        assertEquals(50, bonus, "4회째 baseDamage×0.5 반환");

        // 초기화 후 5회차: 다시 보너스 없음 (스택 1)
        assertEquals(0, tracker.checkAndApply(target, skill, base), "리셋 후 1회차 보너스 없음");
    }

    @Test
    void different_skills_tracked_independently() {
        ResonanceTracker tracker = new ResonanceTracker();
        UUID target = UUID.randomUUID();

        // skillA 3회
        tracker.checkAndApply(target, "cleave", 200f);
        tracker.checkAndApply(target, "cleave", 200f);
        tracker.checkAndApply(target, "cleave", 200f);

        // skillB 는 별도 카운터 — 4회째여도 cleave 스택 영향 없음
        assertEquals(0, tracker.checkAndApply(target, "dismantle", 100f),
                "다른 스킬은 독립 카운터");

        // skillA 4회째 → 트리거
        int bonus = tracker.checkAndApply(target, "cleave", 200f);
        assertEquals(100, bonus, "cleave 4회째 200×0.5=100");
    }

    @Test
    void null_skill_returns_zero() {
        ResonanceTracker tracker = new ResonanceTracker();
        assertEquals(0, tracker.checkAndApply(UUID.randomUUID(), null, 100f),
                "null skillId → 0");
    }
}
