package com.jjk.test;

import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// decisions §3-2·§3-3: cooldowns Map 금지 키 충돌 방지 검증
class CooldownKeyConflictTest {

    // 1. domainCooldownUntil 전용 필드 존재 + cooldowns Map에 "domain" 키 없음
    @Test
    void testDomainCooldownNotInMap() throws NoSuchFieldException {
        Field f = PlayerData.class.getDeclaredField("domainCooldownUntil");
        assertNotNull(f, "domainCooldownUntil 전용 필드 존재 확인");

        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        assertFalse(data.cooldowns.containsKey("domain"),
                "cooldowns Map에 'domain' 키 사용 금지 (decisions §3-2)");
    }

    // 2. awakeningCooldownUntil 전용 필드 존재 + cooldowns Map에 "awakening" 키 없음
    @Test
    void testAwakeningCooldownNotInMap() throws NoSuchFieldException {
        Field f = PlayerData.class.getDeclaredField("awakeningCooldownUntil");
        assertNotNull(f, "awakeningCooldownUntil 전용 필드 존재 확인");

        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        assertFalse(data.cooldowns.containsKey("awakening"),
                "cooldowns Map에 'awakening' 키 사용 금지 (decisions §3-3)");
    }

    // 3. createDefault() 후 cooldowns Map 비어있음 + 금지 키 없음
    @Test
    void testAllowedCooldownKeys() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());

        assertTrue(data.cooldowns.isEmpty(), "초기 상태 cooldowns 비어있어야 함");
        assertFalse(data.cooldowns.containsKey("domain"),    "금지 키 'domain' 없음");
        assertFalse(data.cooldowns.containsKey("awakening"), "금지 키 'awakening' 없음");
    }

    // 4. "skill_seal" 키는 히구루마·마허라가 봉인이 공유 (decisions §3-6)
    @Test
    void testSkillSealKeyShared() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        long tick = 1000L;
        long sealDuration = 400L; // decisions §2-2 sealDurationTicks

        data.cooldowns.put("skill_seal", tick + sealDuration);

        long sealUntil = data.cooldowns.getOrDefault("skill_seal", 0L);
        assertTrue(sealUntil > tick,
                "skill_seal 키로 봉인 상태 확인 — 히구루마·마허라가 공용 키 (decisions §3-6)");
        assertEquals(tick + sealDuration, sealUntil);
    }

    // 5. Phase 3 신규 허용 키 — 금지 키와 충돌 없음
    @Test
    void testAllowedCooldownKeysPhase3() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        long tick = 100L;
        data.cooldowns.put("status_fire",        tick + 80);
        data.cooldowns.put("status_freeze",      tick + 40);
        data.cooldowns.put("status_sleep",       tick + 100);
        data.cooldowns.put("status_slow",        tick + 60);
        data.cooldowns.put("status_soul_resist", tick + 80);
        data.cooldowns.put("jackpot_retry_used", tick + 1);
        data.cooldowns.put("trial_state",        1L);
        data.cooldowns.put("trial_timeout",      tick + 60);
        data.cooldowns.put("trial_target",       12345L);
        // 금지 키는 여전히 없어야 함
        assertFalse(data.cooldowns.containsKey("domain"),    "금지 키 'domain' 없음");
        assertFalse(data.cooldowns.containsKey("awakening"), "금지 키 'awakening' 없음");
        // 허용 키는 정상 저장됨
        assertTrue(data.cooldowns.containsKey("status_fire"),       "status_fire 허용");
        assertTrue(data.cooldowns.containsKey("status_soul_resist"), "status_soul_resist 허용");
        assertTrue(data.cooldowns.containsKey("jackpot_retry_used"), "jackpot_retry_used 허용");
        assertTrue(data.cooldowns.containsKey("trial_state"),        "trial_state 허용");
    }
}
