package com.jjk.test;

import com.jjk.JjkConfig;
import com.jjk.awakening.AwakeningManager;
import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// §LOCK: 160틱 / 2400틱. HP 임계(awakeningHpThreshold=0.05) 및 배율(awakeningMultiplier=1.5)은 config 참조.
class AwakeningManagerTest {

    private final AwakeningManager mgr = new AwakeningManager(new JjkConfig());

    @Test
    void testActivationThreshold() {
        // HP 4.5% (< 5%) → 각성 발동
        PlayerData data1 = PlayerData.createDefault(UUID.randomUUID());
        float maxHp = 20f;
        mgr.checkAndActivate(data1, maxHp * 0.045f, maxHp, 0L);
        assertTrue(data1.awakeningActive, "HP 4.5% → 각성 발동해야 한다");

        // HP 5.5% (> 5%) → 미발동
        PlayerData data2 = PlayerData.createDefault(UUID.randomUUID());
        mgr.checkAndActivate(data2, maxHp * 0.055f, maxHp, 0L);
        assertFalse(data2.awakeningActive, "HP 5.5% → 각성 미발동해야 한다");
    }

    @Test
    void testAutoExpiry() {
        // 발동 후 §LOCK 160틱에 해제
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        mgr.checkAndActivate(data, 20f * 0.04f, 20f, 0L); // 4% = 발동 조건
        assertTrue(data.awakeningActive);
        assertEquals(160L, data.awakeningEndTick, "각성 종료틱 = 160 (§LOCK)");

        // 159틱 — 아직 활성
        mgr.tickCheck(data, 159L);
        assertTrue(data.awakeningActive, "159틱에는 아직 활성이어야 한다");

        // 160틱 — 해제
        mgr.tickCheck(data, 160L);
        assertFalse(data.awakeningActive, "160틱에 각성 해제되어야 한다 (§LOCK)");
    }

    @Test
    void testCooldownPreventsReactivation() {
        // 발동 직후 재시도 → §LOCK 2400틱 쿨타임으로 차단
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        mgr.checkAndActivate(data, 20f * 0.04f, 20f, 0L);
        assertTrue(data.awakeningActive);
        assertEquals(2400L, data.awakeningCooldownUntil, "쿨타임 = 2400 (§LOCK)");

        // 만료 처리 후 쿨타임 중 재시도
        mgr.tickCheck(data, 160L);
        assertFalse(data.awakeningActive);

        mgr.checkAndActivate(data, 20f * 0.04f, 20f, 161L);
        assertFalse(data.awakeningActive, "쿨타임 중 재발동 불가");

        // 쿨타임 종료 후 재발동 가능
        mgr.checkAndActivate(data, 20f * 0.04f, 20f, 2401L);
        assertTrue(data.awakeningActive, "쿨타임 후 재발동 가능");
    }

    @Test
    void testNoDuplicateActivation() {
        // 이미 활성 상태에서 재호출 → 상태 변화 없음
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        mgr.checkAndActivate(data, 20f * 0.04f, 20f, 0L);
        assertTrue(data.awakeningActive);
        long originalEndTick = data.awakeningEndTick;

        mgr.checkAndActivate(data, 20f * 0.04f, 20f, 1L);
        assertEquals(originalEndTick, data.awakeningEndTick, "이미 활성 → awakeningEndTick 변화 없음");
    }

    @Test
    void awakening_blockedDuringJackpot() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.jackpotActive = true;
        mgr.checkAndActivate(data, 20f * 0.03f, 20f, 0L); // HP 3% — 조건 충족
        assertFalse(data.awakeningActive, "잭팟 활성 중 각성 발동 불가");
    }

    @Test
    void awakening_allowedAfterJackpotEnds() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.jackpotActive = true;
        mgr.checkAndActivate(data, 20f * 0.03f, 20f, 0L);
        assertFalse(data.awakeningActive, "잭팟 중 미발동");

        data.jackpotActive = false;
        mgr.checkAndActivate(data, 20f * 0.03f, 20f, 1L);
        assertTrue(data.awakeningActive, "잭팟 종료 후 HP 3% → 각성 발동");
    }
}
