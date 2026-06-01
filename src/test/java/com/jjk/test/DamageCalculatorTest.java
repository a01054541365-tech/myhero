package com.jjk.test;

import com.jjk.JjkConfig;
import com.jjk.api.combat.IDamageSource;
import com.jjk.combat.CombatPipeline;
import com.jjk.combat.DamageCalculator;
import com.jjk.combat.DamageContext;
import com.jjk.combat.TickDamageCap;
import com.jjk.data.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// §LOCK 수치 보호: 흑섬 base × 2.5 / PvP 캡 max_hp × 0.40 / clamp ×0.25~×4.0
class DamageCalculatorTest {

    private final DamageCalculator calculator = new DamageCalculator();
    private final CombatPipeline   pipeline   = new CombatPipeline();

    // ─── 기존 테스트 ─────────────────────────────────────────────────────────

    @Test
    void blackFlash_multipliesBaseBy2_5() {
        float base = 40f;
        DamageContext ctx = DamageContext.builder(null, null, IDamageSource.BLACK_FLASH, base)
                .blackFlash()
                .build();
        float result = calculator.calculate(ctx);
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

    // ─── §4 공식 기반 calculatePure 테스트 ──────────────────────────────────

    @Test
    void testPvpCapApplied() {
        // §LOCK: PvP 상한 targetHpMax × 0.40f
        float result = TickDamageCap.apply(9999f, 20f, 0.40f);
        assertEquals(8.0f, result, 0.001f, "PvP 캡: min(9999, 20×0.40) = 8.0");
        assertTrue(result <= 20f * 0.40f);
    }

    @Test
    void testSoulDirectBypassesDefense() {
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        attacker.attackStat = 0;
        attacker.grade = "4급";

        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        target.defenseStat = 100;

        float baseDamage = 50f;
        JjkConfig config = new JjkConfig();

        // 일반 공격: 방어력 100 → effectiveDamage = max(0, 50-100) = 0
        DamageContext ctxNormal = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, baseDamage).build();
        float normalResult = calculator.calculatePure(ctxNormal, attacker, target, baseDamage, config, 0L);
        assertEquals(0f, normalResult, 0.001f, "방어력 100 → 데미지 0");

        // Soul Direct: effectiveDefense=0 → 50 그대로
        DamageContext ctxSoul = DamageContext.builder(null, null, IDamageSource.SOUL_DIRECT, baseDamage).soulDirect().build();
        float soulResult = calculator.calculatePure(ctxSoul, attacker, target, baseDamage, config, 0L);
        assertEquals(50f, soulResult, 0.001f, "isSoulDirect=true → 방어력 무시, 50 데미지");
    }

    @Test
    void testMultiplierClampMin() {
        // Zone 페널티(×0.5)가 있어도 배율은 §LOCK 하한 0.25 이상
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        attacker.attackStat = 0;
        attacker.grade = "4급";
        attacker.zonePenaltyUntilTick = Long.MAX_VALUE;  // zone penalty 활성

        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        target.defenseStat = 0;

        DamageContext ctx = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, 100f).build();
        float result = calculator.calculatePure(ctx, attacker, target, 100f, new JjkConfig(), 0L);

        // condMult=0.5, total=0.5 → clamp 미발동(>0.25), result=50
        assertTrue(result >= 100f * 0.25f, "최소 배율 클램프: finalDamage >= baseDamage × 0.25");
    }

    @Test
    void testMultiplierClampMax() {
        // 극단적 고스탯: total > 4.0 → §LOCK 상한 4.0으로 클램프
        // ctx.target=null → isPvP=false → gradeMult = pveGradeMultiplier
        // pveGradeMultiplier=1.80f 설정으로 클램프 발동: 2.20 × 1.80 × 1.25 = 4.95 → 4.0
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        attacker.attackStat = 1000;
        attacker.burstActive = true;
        attacker.awakeningActive = true;
        attacker.grade = "특급";

        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        target.defenseStat = 0;

        JjkConfig config = new JjkConfig();
        config.pveGradeMultiplier = 1.80f; // 클램프 발동용

        DamageContext ctx = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, 100f).build();
        float result = calculator.calculatePure(ctx, attacker, target, 100f, config, 0L);

        // 자연 배율 ≈ 2.2 × 1.80 × 1.25 = 4.95 → 클램프 후 4.0
        assertTrue(result <= 100f * 4.0f, "최대 배율 클램프: 전체 배율 ≤ 4.0");
        assertEquals(100f * 4.0f, result, 0.001f, "클램프 상한 4.0 적용 확인");
    }

    @Test
    void testBlackFlashMultiplier() {
        // §LOCK: 흑섬 base × 2.5
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        attacker.attackStat = 0;
        attacker.grade = "4급";

        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        target.defenseStat = 0;

        DamageContext ctx = DamageContext.builder(null, null, IDamageSource.BLACK_FLASH, 100f)
                .blackFlash()
                .build();
        float result = calculator.calculatePure(ctx, attacker, target, 100f, new JjkConfig(), 0L);

        // rawDamage = 100 × 2.5 × 1.0 × 1.0 × 1.0 = 250, defense=0 → finalDamage=250
        assertEquals(250f, result, 0.001f, "흑섬 base × 2.5 → rawDamage=250f (§LOCK)");
        assertEquals(250f, ctx.rawDamage, 0.001f);
    }

    // ─── TASK-45: applyDefenseStat 비율 감쇠 4케이스 ────────────────────────────

    @Test
    void defenseStatReducesDamage() {
        // defenseStat=60, defenseMultiplier=1.0 → effectiveDefense=60
        // 100 × (100/160) = 62.5
        float result = CombatPipeline.applyDefenseStat(100f, 60, 1.0f, false);
        assertEquals(62.5f, result, 0.01f);
    }

    @Test
    void defenseMultiplier_80_reducesEffectiveDefense() {
        // defenseStat=75, defenseMultiplier=0.80 → effectiveDefense=60
        // 100 × (100/160) = 62.5
        float result = CombatPipeline.applyDefenseStat(100f, 75, 0.80f, false);
        assertEquals(62.5f, result, 0.01f);
    }

    @Test
    void soulDirect_bypassesDefense() {
        // isSoulDirect=true → 방어 무시, 100f 그대로
        float result = CombatPipeline.applyDefenseStat(100f, 75, 1.0f, true);
        assertEquals(100f, result, 0.01f);
    }

    @Test
    void zeroDefense_noDamageReduction() {
        // defenseStat=0 → damage × (100/100) = 100f 그대로
        float result = CombatPipeline.applyDefenseStat(100f, 0, 1.0f, false);
        assertEquals(100f, result, 0.01f);
    }

    @Test
    void testAwakeningMultiplier() {
        // 각성 활성 → conditionMultiplier에 ×1.25 포함
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        attacker.attackStat = 0;
        attacker.grade = "4급";

        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        target.defenseStat = 0;

        JjkConfig config = new JjkConfig();

        DamageContext ctxBase = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, 100f).build();
        float withoutAwakening = calculator.calculatePure(ctxBase, attacker, target, 100f, config, 0L);

        attacker.awakeningActive = true;
        DamageContext ctxAwake = DamageContext.builder(null, null, IDamageSource.NORMAL_TECHNIQUE, 100f).build();
        float withAwakening = calculator.calculatePure(ctxAwake, attacker, target, 100f, config, 0L);

        // 확정 배율 1.5 (config.awakeningMultiplier 기본값)
        assertEquals(withoutAwakening * 1.5f, withAwakening, 0.001f, "각성 배율 ×1.5 (확정)");
    }
}
