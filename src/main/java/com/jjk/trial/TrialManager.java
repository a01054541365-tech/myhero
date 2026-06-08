package com.jjk.trial;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.api.skill.SkillResult;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.SealedSkillSyncS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrialManager {

    private final JjkConfig config;
    private final Map<UUID, TrialStateMachine> activeTrials = new HashMap<>();
    private final Map<UUID, List<UUID>> evidenceMap = new HashMap<>();

    public TrialManager(JjkConfig config) {
        this.config = config;
    }

    public void addEvidence(ServerPlayerEntity prosecutor, ServerPlayerEntity target) {
        evidenceMap.computeIfAbsent(prosecutor.getUuid(), k -> new ArrayList<>())
                   .add(target.getUuid());
    }

    public void startTrial(ServerPlayerEntity higuruma, ServerPlayerEntity target) {
        UUID trialKey = higuruma.getUuid();
        if (activeTrials.containsKey(trialKey)) return;
        TrialStateMachine sm = new TrialStateMachine(JJKMod.getServer());
        sm.accuse(higuruma.getUuid(), target.getUuid());
        activeTrials.put(trialKey, sm);
    }

    /** 단일-스킬 봉인 만료 검사 (TickScheduler 1틱 주기 등록). */
    public static void tickSealExpiry(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (data.sealExpireTick > 0 && tick >= data.sealExpireTick) {
            data.sealedSkills.clear();
            data.sealExpireTick = 0L;
            JJKMod.getPlayerRepository().save(data);

            var pkt = new SealedSkillSyncS2CPacket(player.getUuid(), "", 0L);
            for (ServerPlayerEntity p : player.getServer().getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, pkt);
            }
        }
    }

    public void tick() {
        activeTrials.values().forEach(TrialStateMachine::tick);
        activeTrials.entrySet().removeIf(e -> e.getValue().getState() == TrialStateMachine.State.END);
    }

    // ── Pure-data 경로 (테스트·PlayerData 기반 처리용) ────────────────────────────

    /** 증거 제출: target.cooldowns에 "evidence_{tick}" 키로 600틱 유효 증거 추가. */
    public static void addEvidence(PlayerData attacker, PlayerData target, long tick) {
        if (!"higuruma".equals(attacker.characterId)) return;
        target.cooldowns.put("evidence_" + tick, tick + 600);
    }

    /** 피고(target)에게 누적된 유효 증거 개수. */
    public static int getEvidenceCount(PlayerData target, long tick) {
        return (int) target.cooldowns.entrySet().stream()
                .filter(e -> e.getKey().startsWith("evidence_") && e.getValue() > tick)
                .count();
    }

    /** 유효 증거 1개 소모 (가장 먼저 발견된 항목 제거). */
    public static void consumeEvidence(PlayerData target, long tick) {
        target.cooldowns.entrySet().stream()
                .filter(e -> e.getKey().startsWith("evidence_") && e.getValue() > tick)
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(target.cooldowns::remove);
    }

    /** 재판 시작: 유효 증거가 없으면 FAIL_CONDITION, 있으면 DELIBERATION 상태 세팅 후 SUCCESS. */
    public static SkillResult startTrial(PlayerData attacker, PlayerData target, long tick, JjkConfig config) {
        boolean hasEvidence = target.cooldowns.entrySet().stream()
                .anyMatch(e -> e.getKey().startsWith("evidence_") && e.getValue() > tick);
        if (!hasEvidence) return SkillResult.FAIL_CONDITION;
        attacker.cooldowns.put("trial_state", 1L);
        attacker.cooldowns.put("trial_timeout", tick + 60);
        if (target.uuid != null) {
            attacker.cooldowns.put("trial_target", (long) target.uuid.hashCode());
        }
        return SkillResult.SUCCESS;
    }
}
