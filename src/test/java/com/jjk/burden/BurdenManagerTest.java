package com.jjk.burden;

import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BurdenManagerTest {

    private BurdenManager manager;

    @BeforeEach
    void setup() {
        manager = new BurdenManager(new JjkConfig());
    }

    private PlayerData inumakiData() {
        PlayerData d = new PlayerData();
        d.uuid = UUID.randomUUID();
        d.characterId = "inumaki";
        d.burden = 50;
        return d;
    }

    @Test
    void testBurdenIncrease() {
        PlayerData data = inumakiData();
        int before = data.burden;
        manager.addBurdenInternal(data, 10);
        assertEquals(before + 10, data.burden);
    }

    @Test
    void testBurdenReductionOutOfCombat() {
        PlayerData data = inumakiData();
        data.lastCombatTick = 0;
        int before = data.burden;
        // 4 ticks × 0.25 = 1.0 accumulated → 1 drop
        for (int i = 0; i < 4; i++) {
            manager.tickInternal(data, 200 + i);
        }
        assertTrue(data.burden < before, "Out-of-combat burden should decrease after 4 ticks");
    }

    @Test
    void testBurdenReductionInCombat() {
        long baseTick = 200;

        PlayerData oocData = inumakiData();
        oocData.lastCombatTick = 0; // gap >= 100 → out of combat, 0.25/tick

        PlayerData icData = inumakiData();
        icData.lastCombatTick = baseTick - 50; // gap < 100 → in combat, 0.1/tick

        // After 10 ticks: OOC accumulated=2.5 → 2 drops (burden=48),
        //                 IC  accumulated=1.0 → 1 drop  (burden=49)
        for (int i = 0; i < 10; i++) {
            manager.tickInternal(oocData, baseTick + i);
            manager.tickInternal(icData, baseTick + i);
        }
        assertTrue(icData.burden > oocData.burden,
            "In-combat burden should decrease less than out-of-combat (0.1/tick vs 0.25/tick)");
    }

    @Test
    void testBurdenSealOnOverflow() {
        PlayerData data = inumakiData();
        data.burden = 101;
        manager.tickInternal(data, 100L);
        assertTrue(data.cooldowns.containsKey("skill_seal"));
        assertEquals(0, data.burden);
    }

    @Test
    void testNonInumakiSkipped() {
        PlayerData data = new PlayerData();
        data.characterId = "gojo";
        data.burden = 50;
        manager.tickInternal(data, 100L);
        assertEquals(50, data.burden);
    }
}
