package com.jjk.trial;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.SealedSkillSyncS2CPacket;
import com.jjk.network.s2c.VerdictS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TrialStateMachine {

    public enum State {
        IDLE, ACCUSED, DELIBERATION, VERDICT, END
    }

    private State state = State.IDLE;
    private UUID accusedUuid;
    private UUID prosecutorUuid;
    private int stateTicks = 0;
    private boolean guilty;
    private final MinecraftServer server;

    public TrialStateMachine(MinecraftServer server) {
        this.server = server;
    }

    public void tick() {
        stateTicks++;
        switch (state) {
            case ACCUSED -> {
                if (stateTicks >= 20) {
                    state = State.DELIBERATION;
                    stateTicks = 0;
                }
            }
            case DELIBERATION -> {
                if (stateTicks >= 60) {
                    guilty = rollVerdict();
                    state = State.VERDICT;
                    stateTicks = 0;
                }
            }
            case VERDICT -> {
                if (stateTicks >= 1) {
                    if (guilty) {
                        applyGuiltyVerdict();
                    } else {
                        broadcastVerdict(false, "");
                    }
                    state = State.END;
                    endTrial();
                }
            }
            default -> {}
        }
    }

    /** 증거 개수 기반 성공률 산출 후 RNG 판정. server == null(테스트 환경)이면 §LOCK trialSuccessRate 단순 적용. */
    private boolean rollVerdict() {
        if (server == null) {
            return ThreadLocalRandom.current().nextFloat() < JJKMod.getConfig().trialSuccessRate;
        }
        long currentTick = server.getOverworld().getTime();
        PlayerData accusedData = JJKMod.getPlayerRepository().load(accusedUuid);
        PlayerData prosecutorData = JJKMod.getPlayerRepository().load(prosecutorUuid);

        int evidenceCount = TrialManager.getEvidenceCount(accusedData, currentTick);
        if (evidenceCount == 0) return false;

        float baseRate = evidenceCount >= 3 ? 1.0f : JJKMod.getConfig().trialSuccessRate;
        float finalRate = prosecutorData.evidenceAmplifyActive ? Math.min(baseRate + 0.30f, 0.90f) : baseRate;
        return ThreadLocalRandom.current().nextFloat() < finalRate;
    }

    private void applyGuiltyVerdict() {
        if (server == null) return;
        long currentTick = server.getOverworld().getTime();
        int sealDuration = JJKMod.getConfig().sealDurationTicks;

        PlayerData accusedData = JJKMod.getPlayerRepository().load(accusedUuid);
        String sealedSkillId = accusedData.lastUsedSkillId != null ? accusedData.lastUsedSkillId : "";
        if (!sealedSkillId.isEmpty()) {
            accusedData.sealedSkills.add(sealedSkillId);
            accusedData.sealExpireTick = currentTick + sealDuration;
            JJKMod.getPlayerRepository().saveImmediate(accusedData);
            broadcastSealedSkillSync(accusedUuid, sealedSkillId, accusedData.sealExpireTick);
        }

        PlayerData prosecutorData = JJKMod.getPlayerRepository().load(prosecutorUuid);
        prosecutorData.hasExecutionSword = true;
        JJKMod.getPlayerRepository().saveImmediate(prosecutorData);

        broadcastVerdict(true, sealedSkillId);
    }

    /** END 정리: 증거 스택 초기화, 증거 강화 버프 해제. */
    private void endTrial() {
        if (server == null) return;
        long currentTick = server.getOverworld().getTime();

        PlayerData accusedData = JJKMod.getPlayerRepository().load(accusedUuid);
        accusedData.cooldowns.entrySet().removeIf(e -> e.getKey().startsWith("evidence_") && e.getValue() > currentTick);
        JJKMod.getPlayerRepository().saveImmediate(accusedData);

        PlayerData prosecutorData = JJKMod.getPlayerRepository().load(prosecutorUuid);
        if (prosecutorData.evidenceAmplifyActive) {
            prosecutorData.evidenceAmplifyActive = false;
            JJKMod.getPlayerRepository().saveImmediate(prosecutorData);
        }
    }

    private void broadcastSealedSkillSync(UUID targetUuid, String sealedSkillId, long expireAtTick) {
        if (server == null) return;
        var pkt = new SealedSkillSyncS2CPacket(targetUuid, sealedSkillId, expireAtTick);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, pkt);
        }
    }

    private void broadcastVerdict(boolean guiltyResult, String sealedSkillId) {
        if (server == null) return;
        var pkt = new VerdictS2CPacket(prosecutorUuid, accusedUuid, guiltyResult, sealedSkillId);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, pkt);
        }
    }

    public void accuse(UUID prosecutor, UUID accused) {
        if (state != State.IDLE) return;
        this.prosecutorUuid = prosecutor;
        this.accusedUuid = accused;
        this.state = State.ACCUSED;
        this.stateTicks = 0;
    }

    public State getState() { return state; }
    public UUID getAccusedUuid() { return accusedUuid; }
}
