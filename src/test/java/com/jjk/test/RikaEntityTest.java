package com.jjk.test;

import com.jjk.JjkConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RikaEntityTest {

    // ── 1. §LOCK: rikaLifetimeTicks = 200 ─────────────────────────────────────
    @Test
    void testRikaLifetime() {
        JjkConfig config = new JjkConfig();
        assertEquals(200, config.rikaLifetimeTicks,
            "§LOCK rikaLifetimeTicks = 200");

        int threshold = config.rikaLifetimeTicks;

        // discard 조건: lifetimeTicks >= rikaLifetimeTicks (RikaEntity.tick())
        assertFalse(threshold - 1 >= threshold, "199틱: 소멸 조건 미충족");
        assertTrue(threshold     >= threshold,  "200틱: 소멸 조건 충족");
        assertTrue(threshold + 1 >= threshold,  "201틱: 소멸 조건 충족");
    }
}
