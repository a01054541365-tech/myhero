package com.jjk.test;

import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import com.jjk.finger.FingerSystem;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// §LOCK: fingerDropRate=0.10f, fingerMaxCount=20 — config 참조 필수
class FingerSystemTest {

    private static PlayerData newData() {
        return PlayerData.createDefault(UUID.randomUUID());
    }

    // 1. 같은 mobId 2회 → fingerCount 최대 1 증가 (droppedMobs 중복 방지)
    @Test
    void testDropAtomicity() {
        JjkConfig config = new JjkConfig();
        config.fingerDropRate = 1.0f; // 확정 드롭
        FingerSystem system = new FingerSystem(config);
        PlayerData data = newData();
        data.fingerCount = 0;

        system.tryDrop("mob_atom", data);
        system.tryDrop("mob_atom", data); // 동일 mobId → 차단
        assertEquals(1, data.fingerCount, "같은 mobId 2회 → 최대 1 증가");
    }

    // 2. droppedMobs에 이미 있는 mobId → 드롭 없음
    @Test
    void testDuplicateMobPrevented() {
        JjkConfig config = new JjkConfig();
        config.fingerDropRate = 1.0f;
        FingerSystem system = new FingerSystem(config);
        PlayerData data = newData();

        system.tryDrop("mob_dup", data);
        int afterFirst = data.fingerCount;
        boolean result = system.tryDrop("mob_dup", data);

        assertFalse(result, "중복 mobId → false 반환");
        assertEquals(afterFirst, data.fingerCount, "중복 mobId → 카운트 변화 없음");
    }

    // 3. fingerCount=19, 드롭 성공 → fingerCount=20 (상한 §LOCK)
    @Test
    void testFingerCountCap() {
        JjkConfig config = new JjkConfig();
        config.fingerDropRate = 1.0f;
        config.fingerMaxCount = 20; // §LOCK
        FingerSystem system = new FingerSystem(config);
        PlayerData data = newData();
        data.fingerCount = 19;

        system.tryDrop("mob_cap", data);
        assertEquals(20, data.fingerCount, "19→20 증가 확인");
    }

    // 4. fingerCount=20 → 드롭 성공해도 20 유지 (§LOCK)
    @Test
    void testFingerCountNeverExceedMax() {
        JjkConfig config = new JjkConfig();
        config.fingerDropRate = 1.0f;
        config.fingerMaxCount = 20;
        FingerSystem system = new FingerSystem(config);
        PlayerData data = newData();
        data.fingerCount = 20;

        system.tryDrop("mob_over", data);
        assertEquals(20, data.fingerCount, "이미 20개 → 변화 없음 (§LOCK)");
    }

    // 5. fingerDropRate=0.0f → 드롭 없음
    @Test
    void testDropRateZero() {
        JjkConfig config = new JjkConfig();
        config.fingerDropRate = 0.0f; // §LOCK: 0.10f 기본, 테스트에서만 0으로 세팅
        FingerSystem system = new FingerSystem(config);
        PlayerData data = newData();

        system.tryDrop("mob_rate0", data);
        assertEquals(0, data.fingerCount, "dropRate=0 → 드롭 없음");
    }
}
