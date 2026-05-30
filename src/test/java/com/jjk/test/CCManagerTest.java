package com.jjk.test;

import com.jjk.combat.CCManager;
import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CCManagerTest {

    private PlayerData newData() {
        return new PlayerData();
    }

    @Test
    void testFirstCCApplied() {
        PlayerData data = newData();
        boolean result = CCManager.tryApplyCC(data, "stun", 40, 0L);
        assertTrue(result);
        assertEquals(40L, data.cooldowns.get("status_stun"));
    }

    @Test
    void testDRWindowReducesDuration() {
        PlayerData data = newData();
        CCManager.tryApplyCC(data, "stun", 40, 0L);
        // window = 0 + 80 = 80, re-apply within window at tick 10
        CCManager.tryApplyCC(data, "stun", 40, 10L);
        // duration halved: 40 * 0.5 = 20, stored as currentTick(10) + 20 = 30
        assertEquals(30L, data.cooldowns.get("status_stun"));
    }

    @Test
    void testImmunityGrantedWhenDurationZero() {
        PlayerData data = newData();
        CCManager.tryApplyCC(data, "stun", 1, 0L);
        // second call in window: (int)(1 * 0.5f) = 0 → immunity granted
        boolean result = CCManager.tryApplyCC(data, "stun", 1, 10L);
        assertFalse(result);
        assertNotNull(data.cooldowns.get("cc_immune_stun"));
    }

    @Test
    void testCCBlockedDuringImmunity() {
        PlayerData data = newData();
        data.cooldowns.put("cc_immune_stun", 100L);
        boolean result = CCManager.tryApplyCC(data, "stun", 40, 50L);
        assertFalse(result);
    }

    @Test
    void testCCAppliedAfterImmunityExpires() {
        PlayerData data = newData();
        data.cooldowns.put("cc_immune_stun", 100L);
        boolean result = CCManager.tryApplyCC(data, "stun", 40, 200L);
        assertTrue(result);
    }
}
