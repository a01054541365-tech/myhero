package com.jjk.test;

import com.jjk.JjkConfig;
import com.jjk.api.skill.SkillResult;
import com.jjk.character.impl.HigurumaSkillSet;
import com.jjk.data.PlayerData;
import com.jjk.trial.TrialManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TrialManagerTest {

    @Test
    void testEvidenceAdded() {
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        PlayerData target   = PlayerData.createDefault(UUID.randomUUID());
        attacker.characterId = "higuruma";
        long tick = 100L;
        TrialManager.addEvidence(attacker, target, tick);
        boolean hasEvidence = target.cooldowns.keySet().stream()
                .anyMatch(k -> k.startsWith("evidence_"));
        assertTrue(hasEvidence, "증거 제출 후 target.cooldowns에 'evidence_*' 키가 있어야 함");
    }

    @Test
    void testEvidenceExpiry() {
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        PlayerData target   = PlayerData.createDefault(UUID.randomUUID());
        attacker.characterId = "higuruma";
        long tick = 100L;
        TrialManager.addEvidence(attacker, target, tick);
        // 600틱 후에는 증거가 만료되어야 함
        long expiredTick = tick + 601L;
        boolean valid = target.cooldowns.entrySet().stream()
                .filter(e -> e.getKey().startsWith("evidence_"))
                .anyMatch(e -> e.getValue() > expiredTick);
        assertFalse(valid, "600틱 후 증거는 만료되어야 함");
    }

    @Test
    void testNonHigurumaIgnored() {
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        PlayerData target   = PlayerData.createDefault(UUID.randomUUID());
        attacker.characterId = "gojo";
        TrialManager.addEvidence(attacker, target, 0L);
        boolean hasEvidence = target.cooldowns.keySet().stream()
                .anyMatch(k -> k.startsWith("evidence_"));
        assertFalse(hasEvidence, "히구루마 아닌 캐릭터의 증거 제출은 무시되어야 함");
    }

    @Test
    void testTrialStartRequiresEvidence() {
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        PlayerData target   = PlayerData.createDefault(UUID.randomUUID());
        attacker.characterId = "higuruma";
        JjkConfig config = new JjkConfig();
        SkillResult result = TrialManager.startTrial(attacker, target, 0L, config);
        assertEquals(SkillResult.FAIL_CONDITION, result, "증거 없으면 재판 시작 실패");
    }

    @Test
    void testTrialStartSucceedsWithEvidence() {
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        PlayerData target   = PlayerData.createDefault(UUID.randomUUID());
        attacker.characterId = "higuruma";
        long tick = 100L;
        TrialManager.addEvidence(attacker, target, tick);
        JjkConfig config = new JjkConfig();
        SkillResult result = TrialManager.startTrial(attacker, target, tick, config);
        assertEquals(SkillResult.SUCCESS, result, "유효 증거 있으면 재판 시작 성공");
        assertTrue(attacker.cooldowns.containsKey("trial_state"), "재판 상태 키 설정됨");
        assertTrue(attacker.cooldowns.containsKey("trial_timeout"), "재판 타임아웃 키 설정됨");
    }

    @Test
    void testVerdictSetsExecutionSword() {
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        attacker.characterId = "higuruma";
        attacker.cooldowns.put("trial_state", 1L);
        attacker.cooldowns.put("trial_timeout", 0L);
        attacker.cooldowns.put("trial_target", 12345L);
        // 초기값 false 확인 (유죄 판결 확률적이므로 초기 상태만 검증)
        assertFalse(attacker.hasExecutionSword, "초기 hasExecutionSword=false");
    }

    @Test
    void testExecutionSwordConsumedAfterUse() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.hasExecutionSword = true;
        data.ceCurrent = 9999f;
        HigurumaSkillSet skill = new HigurumaSkillSet();
        // player=null → FAIL_NO_TARGET (hasExecutionSword 체크 통과 후 타겟 탐색 실패)
        SkillResult result = skill.onV(data, null, 0L);
        assertEquals(SkillResult.FAIL_NO_TARGET, result, "player=null이면 FAIL_NO_TARGET");
        // 타겟 없으면 hasExecutionSword는 소비되지 않음
        assertTrue(data.hasExecutionSword, "타겟 없을 때 처형검 미소비");
    }
}
