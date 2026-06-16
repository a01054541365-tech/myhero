package com.jjk.test;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.api.skill.SkillResult;
import com.jjk.character.impl.HigurumaSkillSet;
import com.jjk.data.PlayerData;
import com.jjk.trial.TrialManager;
import com.jjk.trial.TrialStateMachine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Phase I-2 — 심판의 일격(culpable_hit) / 증거 강화(evidence_amplify) / 재판 흐름 검증 */
class HigurumaTrialTest {

    @BeforeAll
    static void setupJJKMod() throws Exception {
        Field instanceField = JJKMod.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        JJKMod mod = JJKMod.class.getDeclaredConstructor().newInstance();
        Field configField = JJKMod.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(mod, new JjkConfig());
        instanceField.set(null, mod);
        // 데이터 주도 전환(2026-06-11): 스킬 CE/CD는 techniques.json에서 읽으므로 로드 필요
        com.jjk.combat.TechniqueLoader.load(java.nio.file.Path.of("run/config/jjk/techniques.json"));
    }

    // 전역 JJKMod.INSTANCE 오염 정리 — 다른 테스트(CombatPipelineIntegrationTest 등)가
    // getCEManager() 등 null 매니저에 NPE 발생하는 것을 방지 (테스트 순서 비의존성 보장)
    @AfterAll
    static void tearDownJJKMod() throws Exception {
        Field instanceField = JJKMod.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    private static PlayerData higuruma() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.characterId = "higuruma";
        data.ceCurrent = 9999f;
        return data;
    }

    // ── culpable_hit (keyId 1) ───────────────────────────────────────────────

    @Test
    void testCulpableHitMetadata() {
        HigurumaSkillSet skill = new HigurumaSkillSet();
        assertEquals("culpable_hit", skill.getSkillName(1));
        assertEquals(160, skill.getCeCost(1));        // techniques.json higuruma keyId1
        assertEquals(18, skill.getCooldownTicks(1));
    }

    @Test
    void testCulpableHitFailsWithInsufficientCe() {
        PlayerData data = higuruma();
        data.ceCurrent = 0f;
        HigurumaSkillSet skill = new HigurumaSkillSet();
        assertEquals(SkillResult.FAIL_CE_INSUFFICIENT, skill.onShiftF(data, null, 0L));
    }

    @Test
    void testCulpableHitFailsWithoutTarget() {
        PlayerData data = higuruma();
        HigurumaSkillSet skill = new HigurumaSkillSet();
        assertEquals(SkillResult.FAIL_NO_TARGET, skill.onShiftF(data, null, 0L));
    }

    @Test
    void testCulpableHitOnCooldown() {
        PlayerData data = higuruma();
        data.cooldowns.put("1", 100L);
        HigurumaSkillSet skill = new HigurumaSkillSet();
        assertEquals(SkillResult.ON_COOLDOWN, skill.onShiftF(data, null, 0L));
    }

    @Test
    void testCulpableHitFailsWhenSkillSealed() {
        PlayerData data = higuruma();
        data.cooldowns.put("skill_seal", 100L);
        HigurumaSkillSet skill = new HigurumaSkillSet();
        assertEquals(SkillResult.FAIL_SKILL_SEALED, skill.onShiftF(data, null, 0L));
    }

    // ── evidence_amplify (keyId 2) ───────────────────────────────────────────

    @Test
    void testEvidenceAmplifyMetadata() {
        HigurumaSkillSet skill = new HigurumaSkillSet();
        assertEquals("evidence_amplify", skill.getSkillName(2));
        assertEquals(80, skill.getCeCost(2));
        assertEquals(45, skill.getCooldownTicks(2));
    }

    @Test
    void testEvidenceAmplifyFailsWithInsufficientCe() {
        PlayerData data = higuruma();
        data.ceCurrent = 0f;
        HigurumaSkillSet skill = new HigurumaSkillSet();
        assertEquals(SkillResult.FAIL_CE_INSUFFICIENT, skill.onR(data, null, 0L));
    }

    @Test
    void testEvidenceAmplifyFailsWhenAlreadyActive() {
        PlayerData data = higuruma();
        data.evidenceAmplifyActive = true;
        HigurumaSkillSet skill = new HigurumaSkillSet();
        assertEquals(SkillResult.FAIL_CONDITION, skill.onR(data, null, 0L));
    }

    @Test
    void testEvidenceAmplifyFailsWithoutTarget() {
        PlayerData data = higuruma();
        HigurumaSkillSet skill = new HigurumaSkillSet();
        assertEquals(SkillResult.FAIL_NO_TARGET, skill.onR(data, null, 0L));
    }

    // ── TrialManager 증거 카운트/소모 ────────────────────────────────────────

    @Test
    void testEvidenceCountAndConsume() {
        PlayerData attacker = higuruma();
        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        long tick = 100L;

        TrialManager.addEvidence(attacker, target, tick);
        TrialManager.addEvidence(attacker, target, tick + 1);
        assertEquals(2, TrialManager.getEvidenceCount(target, tick));

        TrialManager.consumeEvidence(target, tick);
        assertEquals(1, TrialManager.getEvidenceCount(target, tick));
    }

    // ── TrialStateMachine 흐름 ────────────────────────────────────────────────

    @Test
    void testTrialStateMachineReachesVerdictOrEnd() {
        TrialStateMachine sm = new TrialStateMachine(null);
        sm.accuse(UUID.randomUUID(), UUID.randomUUID());
        // ACCUSED(20) → DELIBERATION(60) → VERDICT(1)
        for (int i = 0; i < 81; i++) sm.tick();
        TrialStateMachine.State state = sm.getState();
        assertTrue(
            state == TrialStateMachine.State.VERDICT || state == TrialStateMachine.State.END,
            "Expected VERDICT or END but was " + state
        );
    }
}
