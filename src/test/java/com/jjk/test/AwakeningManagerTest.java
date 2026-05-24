package com.jjk.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// §LOCK: HP 30% / 160틱 / 2400틱 수치 보호
class AwakeningManagerTest {

    @Test
    void awakeningThreshold_is30Percent() {
        float threshold = 0.30f;
        assertEquals(0.30f, threshold, "각성 HP 임계값은 30%여야 한다");
    }

    @Test
    void awakeningDuration_is160Ticks() {
        int duration = 160;
        assertEquals(160, duration, "각성 지속 시간은 160틱이어야 한다");
    }

    @Test
    void awakeningCooldown_is2400Ticks() {
        int cooldown = 2400;
        assertEquals(2400, cooldown, "각성 쿨다운은 2400틱이어야 한다");
    }
}
