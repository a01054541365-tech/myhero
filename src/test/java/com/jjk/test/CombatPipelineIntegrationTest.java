package com.jjk.test;

import com.jjk.combat.CombatPipeline;
import com.jjk.data.Migrator;
import com.jjk.data.PlayerData;
import com.jjk.data.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// 인메모리 SQLite + PlayerData — ServerPlayerEntity mock 없이 순수 로직 테스트
class CombatPipelineIntegrationTest {

    private CombatPipeline pipeline;

    @BeforeEach
    void setUp() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        new Migrator().migrate(conn);
        // PlayerRepository는 테스트에서 직접 사용하지 않으나 migration 확인용
        pipeline = new CombatPipeline();
    }

    private PlayerData makeSorcerer(String characterId) {
        PlayerData d = PlayerData.createDefault(UUID.randomUUID());
        d.characterId = characterId;
        d.ceCurrent = 100f;
        d.ceMax = 100f;
        d.hpCurrent = 20f;
        d.hpMax = 20f;
        d.attackStat = 10;
        d.defenseStat = 0;
        return d;
    }

    @Test
    void testSameTeamBlocked() {
        // 같은 진영(주술사 vs 주술사) → SAME_TEAM
        PlayerData attacker = makeSorcerer("itadori");
        PlayerData target   = makeSorcerer("megumi");

        CombatPipeline.PipelineResult result = pipeline.processData(attacker, target, 1, 100L);
        assertEquals(CombatPipeline.PipelineResult.SAME_TEAM, result,
                "같은 진영 공격 → SAME_TEAM");
    }

    @Test
    void testCeInsufficientBlocked() {
        // CE 0 → CE_INSUFFICIENT
        PlayerData attacker = makeSorcerer("itadori");
        attacker.ceCurrent = 0f;  // CE 없음
        PlayerData target = makeSorcerer("mahito");  // cursed spirit

        CombatPipeline.PipelineResult result = pipeline.processData(attacker, target, 1, 100L);
        assertEquals(CombatPipeline.PipelineResult.CE_INSUFFICIENT, result,
                "CE 부족 → CE_INSUFFICIENT");
        // 차감되지 않아야 함
        assertEquals(0f, attacker.ceCurrent, 0.001f);
    }

    @Test
    void testTickDamageCapApplied() {
        // §LOCK: PvP 상한 hpMax × 0.40f
        PlayerData attacker = makeSorcerer("itadori");
        attacker.attackStat = 100;  // 높은 스탯으로 rawDamage > cap 보장
        PlayerData target = makeSorcerer("mahito");
        target.hpMax = 20f;
        target.hpCurrent = 20f;
        target.defenseStat = 0;

        CombatPipeline.PipelineResult result = pipeline.processData(attacker, target, 1, 100L);
        assertEquals(CombatPipeline.PipelineResult.SUCCESS, result);

        // 캡 = 20 × 0.40 = 8.0 → target.hpCurrent >= 20 - 8 = 12
        float cap = target.hpMax * 0.40f;
        assertTrue(target.hpCurrent >= target.hpMax - cap - 0.001f,
                "PvP 캡 적용: 최대 데미지 = hpMax × 0.40 = " + cap);
    }

    @Test
    void testAwakeningTriggeredInPipeline() {
        // target HP 25% → CombatPipeline 처리 후 target.awakeningActive=true
        PlayerData attacker = makeSorcerer("itadori");
        PlayerData target = makeSorcerer("mahito");
        target.hpCurrent = target.hpMax * 0.25f;  // 5.0f (25%)
        target.defenseStat = 0;

        assertFalse(target.awakeningActive, "초기 상태: awakeningActive=false");

        CombatPipeline.PipelineResult result = pipeline.processData(attacker, target, 1, 200L);
        assertEquals(CombatPipeline.PipelineResult.SUCCESS, result);

        // hpCurrent ≤ hpMax × 0.30 (6.0) → 각성 발동
        assertTrue(target.awakeningActive,
                "HP 25% 상태에서 피격 후 target.awakeningActive=true");
    }

    // ─── TASK-46: Stage 6c 비율 감쇠 통합 케이스 ────────────────────────────────

    @Test
    void defenseStatApplied_inProcess() {
        // defenseStat=60, defenseMultiplier=1.0
        // 비율 감쇠: 100 × (100/(100+60)) = 62.5
        float result = CombatPipeline.applyDefenseStat(100f, 60, 1.0f, false);
        assertEquals(62.5f, result, 0.01f, "defenseStat=60 → damage×(100/160)=62.5");
    }

    @Test
    void soulDirect_bypassesDefenseStat() {
        // isSoulDirect=true → 방어 무시, 원본 데미지 유지
        float result = CombatPipeline.applyDefenseStat(100f, 75, 1.0f, true);
        assertEquals(100f, result, 0.01f, "isSoulDirect=true → 방어 무시");
    }

    @Test
    void dismantleDefenseMultiplier_appliedInProcess() {
        // defenseMultiplier=0.80 (해체) → effectiveDefense=60 → 100×(100/160)=62.5
        // 일반 공격 defenseStat=75 → effectiveDefense=75 → 100×(100/175)≈57.14
        float dmgDismantle = CombatPipeline.applyDefenseStat(100f, 75, 0.80f, false);
        float dmgNormal    = CombatPipeline.applyDefenseStat(100f, 75, 1.00f, false);
        assertEquals(62.5f, dmgDismantle, 0.01f, "해체 관통: 100×(100/160)=62.5");
        assertTrue(dmgDismantle > dmgNormal, "해체가 일반 공격보다 finalDamage 높아야 함");
    }

    @Test
    void testCooldownSetAfterSkill() {
        // 스킬 성공 후 attacker.cooldowns에 keyId 만료 틱 기록
        PlayerData attacker = makeSorcerer("itadori");
        PlayerData target = makeSorcerer("mahito");
        long currentTick = 500L;

        assertTrue(attacker.cooldowns.isEmpty(), "초기 쿨타임 없음");

        CombatPipeline.PipelineResult result = pipeline.processData(attacker, target, 3, currentTick);
        assertEquals(CombatPipeline.PipelineResult.SUCCESS, result);

        long cooldownUntil = attacker.cooldowns.getOrDefault("3", 0L);
        assertTrue(cooldownUntil > currentTick,
                "스킬 사용 후 keyId '3' 쿨타임이 currentTick보다 커야 한다");
    }
}
