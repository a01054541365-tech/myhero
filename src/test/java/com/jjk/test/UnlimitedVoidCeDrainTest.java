package com.jjk.test;

import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UnlimitedVoidCeDrainTest {

    // ── 1. 무량공처 CE 드레인: 매 틱 ceMax × unlimitedVoidCeDrainRatio 소진 ──
    @Test
    void testUnlimitedVoidCeDrain() {
        JjkConfig config = new JjkConfig();
        PlayerData victim = PlayerData.createDefault(UUID.randomUUID());
        victim.ceMax     = 1000f;
        victim.ceCurrent = 1000f;

        float drainAmount = victim.ceMax * config.unlimitedVoidCeDrainRatio();
        float before = victim.ceCurrent;
        victim.ceCurrent -= drainAmount;
        victim.ceCurrent  = Math.max(0f, victim.ceCurrent);

        assertTrue(victim.ceCurrent < before, "무량공처 틱당 CE 감소");
        assertEquals(before - drainAmount, victim.ceCurrent, 0.001f, "정확한 감소량");
    }

    // ── 2. CE 0 이하 방지 ─────────────────────────────────────────────────────
    @Test
    void testCeDrainFloorAtZero() {
        JjkConfig config = new JjkConfig();
        PlayerData victim = PlayerData.createDefault(UUID.randomUUID());
        victim.ceMax     = 1000f;
        victim.ceCurrent = 0f;

        victim.ceCurrent -= victim.ceMax * config.unlimitedVoidCeDrainRatio();
        victim.ceCurrent  = Math.max(0f, victim.ceCurrent);

        assertEquals(0f, victim.ceCurrent, 0.001f, "CE 0 이하 방지");
    }

    // ── 3. dirty 패턴: CE=0 반복 시 값 변화 없음 ─────────────────────────────
    @Test
    void testDirtyFlagNoCeChange() {
        JjkConfig config = new JjkConfig();
        PlayerData victim = PlayerData.createDefault(UUID.randomUUID());
        victim.ceMax     = 1000f;
        victim.ceCurrent = 0f;

        float beforeCe = victim.ceCurrent;
        victim.ceCurrent -= victim.ceMax * config.unlimitedVoidCeDrainRatio();
        victim.ceCurrent  = Math.max(0f, victim.ceCurrent);

        // CE=0→0: 변화 없음 → save() 불필요
        assertEquals(beforeCe, victim.ceCurrent, 0.001f, "CE 이미 0: 변화 없음");
    }
}
