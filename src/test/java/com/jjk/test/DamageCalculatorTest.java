package com.jjk.test;

import com.jjk.api.combat.IDamageSource;
import com.jjk.combat.DamageCalculator;
import com.jjk.combat.DamageContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// §LOCK: 흑섬 base × 2.5 / PvP 캡 max_hp × 0.40 수치 보호
class DamageCalculatorTest {

    private final DamageCalculator calculator = new DamageCalculator();

    @Test
    void blackFlash_multipliesBaseBy2_5() {
        // given
        float base = 40f;
        DamageContext ctx = DamageContext.builder(null, null, IDamageSource.BLACK_FLASH, base)
                .blackFlash()
                .build();
        // when
        float result = calculator.calculate(ctx);
        // then
        assertEquals(100f, result, 0.001f, "흑섬 배율은 반드시 2.5배여야 한다");
    }

    @Test
    void normalHit_noMultiplier() {
        float base = 40f;
        DamageContext ctx = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, base)
                .build();
        float result = calculator.calculate(ctx);
        assertEquals(base, result, 0.001f);
    }
}
