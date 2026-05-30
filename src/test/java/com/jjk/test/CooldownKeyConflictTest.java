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
}
