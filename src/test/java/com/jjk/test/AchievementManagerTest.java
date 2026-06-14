package com.jjk.test;

import com.jjk.achievement.AchievementRegistry;
import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AchievementManagerTest {

    @Test
    void registry_allAchievementsRegistered() {
        assertEquals(21, AchievementRegistry.all().size(), "업적 21개 등록 확인");
    }

    @Test
    void registry_knownIdReturnsNonNull() {
        assertNotNull(AchievementRegistry.get("first_character"), "first_character 등록됨");
        assertNotNull(AchievementRegistry.get("first_domain"),    "first_domain 등록됨");
        assertNotNull(AchievementRegistry.get("all_fingers"),     "all_fingers 등록됨");
    }

    @Test
    void registry_unknownIdReturnsNull() {
        assertNull(AchievementRegistry.get("does_not_exist"), "미등록 ID → null");
    }

    @Test
    void playerData_unlockedAchievements_duplicatePrevented() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        assertTrue(data.unlockedAchievements.isEmpty(), "초기 비어있음");

        data.unlockedAchievements.add("first_character");
        assertEquals(1, data.unlockedAchievements.size());

        // 두 번째 add → Set이므로 중복 방지
        data.unlockedAchievements.add("first_character");
        assertEquals(1, data.unlockedAchievements.size(), "중복 unlock 방지");
        assertTrue(data.unlockedAchievements.contains("first_character"));
    }

    @Test
    void playerData_snapshot_achievementsIsolated() {
        PlayerData original = PlayerData.createDefault(UUID.randomUUID());
        original.unlockedAchievements.add("first_character");

        PlayerData copy = original.snapshot();
        copy.unlockedAchievements.add("first_domain");

        assertEquals(1, original.unlockedAchievements.size(), "snapshot 후 원본 격리");
        assertEquals(2, copy.unlockedAchievements.size(),     "copy에만 추가됨");
    }
}
