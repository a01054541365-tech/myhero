package com.jjk.test;

import com.jjk.JjkConfig;
import com.jjk.api.combat.IDamageSource;
import com.jjk.combat.CombatPipeline;
import com.jjk.combat.DamageCalculator;
import com.jjk.combat.DamageContext;
import com.jjk.combat.TickDamageCap;
import com.jjk.data.PlayerData;
import com.jjk.grade.GradeManager;
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

    // ─── H-3-3: shrine 스킬 테스트 ──────────────────────────────────────────

    @Test
    void testShrineSoulDirectBypassesDefense() {
        // shrine isSoulDirect=true → 방어 관통 (effectiveDefense=0)
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        attacker.attackStat = 0;
        attacker.grade = "4급";

        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        target.defenseStat = 200; // 방어력 매우 높음

        float shrineBase = 42f;
        JjkConfig config = new JjkConfig();

        DamageContext ctx = DamageContext.builder(null, null, IDamageSource.SOUL_DIRECT, shrineBase)
                .soulDirect()
                .skillName("shrine")
                .build();
        float result = calculator.calculatePure(ctx, attacker, target, shrineBase, config, 0L);
        assertEquals(shrineBase, result, 0.001f,
                "shrine isSoulDirect=true → defenseStat=200 무시, baseDamage=42 그대로");
    }

    @Test
    void testShrineBfComboClampAt4x() {
        // shrine 흑섬 콤보 첫 히트: baseDamage = 42 × 3.0 = 126
        // 극단 스탯으로 자연 배율 >4.0 → §LOCK 클램프 4.0 적용
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        attacker.attackStat = 1000;  // attackMult >> 4.0 유도
        attacker.awakeningActive = true;
        attacker.grade = "준특급";

        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        target.defenseStat = 0;

        float shrineBase = 42f * 3.0f; // bfMult=3.0, 첫 히트
        JjkConfig config = new JjkConfig();
        config.pveGradeMultiplier = 1.65f;

        DamageContext ctx = DamageContext.builder(null, null, IDamageSource.SOUL_DIRECT, shrineBase)
                .soulDirect()
                .skillName("shrine")
                .build();
        float result = calculator.calculatePure(ctx, attacker, target, shrineBase, config, 0L);

        // 자연 배율 >> 4.0 → 클램프 후 shrineBase × 4.0 = 504
        assertTrue(result <= shrineBase * 4.0f,
                "shrine 흑섬 콤보 + 극단 스탯은 §LOCK ×4.0 클램프 이내여야 함");
        assertEquals(shrineBase * 4.0f, result, 0.001f,
                "클램프 상한 4.0 × 126 = 504");
    }

    @Test
    void testShrineMultiHitDecay() {
        // shrine 3타 decay [1.0, 0.90, 0.75] — baseDamage에 사전 적용 후 hitIndex=0 전달
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        attacker.attackStat = 0;
        attacker.grade = "4급";
        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        target.defenseStat = 0;
        JjkConfig config = new JjkConfig();

        float base = 42f;
        float[] expectedBases = {base * 1.0f, base * 0.90f, base * 0.75f};
        float[] results = new float[3];

        for (int i = 0; i < 3; i++) {
            DamageContext ctx = DamageContext.builder(null, null, IDamageSource.SOUL_DIRECT, expectedBases[i])
                    .soulDirect()
                    .hitIndex(0)   // 사전 decay 적용 → 기본 다단 감쇠 우회
                    .build();
            results[i] = calculator.calculatePure(ctx, attacker, target, expectedBases[i], config, 0L);
        }

        assertEquals(42f * 1.00f, results[0], 0.001f, "shrine 1타: decay=1.0 → 42");
        assertEquals(42f * 0.90f, results[1], 0.001f, "shrine 2타: decay=0.90 → 37.8");
        assertEquals(42f * 0.75f, results[2], 0.001f, "shrine 3타: decay=0.75 → 31.5");
        assertTrue(results[0] > results[1] && results[1] > results[2],
                "shrine 다단히트 데미지 감소 확인");
    }

    @Test
    void testShrineGradeCheckRejectsBelow준특급() {
        // 준특급 미만 + 각성 비활성 → shrine 발동 거부 조건 검증
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.awakeningActive = false;

        data.grade = "4급";
        assertFalse(GradeManager.Grade.fromLabel(data.grade).rank
                            >= GradeManager.Grade.SEMI_SPECIAL.rank || data.awakeningActive,
                "4급 + 각성 비활성: shrine 거부 조건");

        data.grade = "1급";
        assertFalse(GradeManager.Grade.fromLabel(data.grade).rank
                            >= GradeManager.Grade.SEMI_SPECIAL.rank || data.awakeningActive,
                "1급 + 각성 비활성: shrine 거부 조건");

        // 준특급 이상 → 허용
        data.grade = "준특급";
        assertTrue(GradeManager.Grade.fromLabel(data.grade).rank
                           >= GradeManager.Grade.SEMI_SPECIAL.rank || data.awakeningActive,
                "준특급: shrine 허용 조건");

        // 4급 + 각성 활성 → 허용
        data.grade = "4급";
        data.awakeningActive = true;
        assertTrue(GradeManager.Grade.fromLabel(data.grade).rank
                           >= GradeManager.Grade.SEMI_SPECIAL.rank || data.awakeningActive,
                "4급 + 각성 활성: shrine 허용 조건");
    }
}
