package com.jjk.finger;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.advancement.AdvancementTriggerManager;
import com.jjk.audit.AuditLogger;
import com.jjk.data.PlayerData;
import com.jjk.event.FullRevivalEvents;
import com.jjk.network.s2c.FingerDropS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class FingerSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(FingerSystem.class);

    private final JjkConfig config;
    private final AtomicBoolean fingerLock = new AtomicBoolean(false);
    // 중복 드롭 방지 — spec §26-4: atomic 처리 필수
    private final Set<String> droppedMobs = Collections.synchronizedSet(new HashSet<>());

    public FingerSystem(JjkConfig config) {
        this.config = config;
    }

    /**
     * 순수 PlayerData 경로 (테스트용). synchronized(fingerLock) + droppedMobs 중복 방지.
     * §LOCK: fingerDropRate, fingerMaxCount config 참조 필수.
     */
    public boolean tryDrop(String mobId, PlayerData sukunaData) {
        synchronized (fingerLock) {
            if (droppedMobs.contains(mobId)) return false;
            if (Math.random() >= config.fingerDropRate) return false;  // §LOCK: 0.10f
            if (sukunaData.fingerCount >= config.fingerMaxCount) return false; // §LOCK: 20
            droppedMobs.add(mobId);
            sukunaData.fingerCount = Math.min(sukunaData.fingerCount + 1, config.fingerMaxCount);
            return true;
        }
    }

    public boolean tryDrop(ServerPlayerEntity victim) {
        // 선행 체크는 lock 밖 — DB 읽기 포함
        if (Math.random() >= config.fingerDropRate) return false;

        ServerPlayerEntity sukunaPlayer =
            victim.getServer().getPlayerManager().getPlayerList()
                .stream()
                .filter(p -> {
                    PlayerData d = JJKMod.getPlayerRepository().load(p.getUuid());
                    return "sukuna".equals(d.characterId);
                })
                .findFirst()
                .orElse(null);

        if (sukunaPlayer == null) return false;

        PlayerData data = JJKMod.getPlayerRepository().load(sukunaPlayer.getUuid());
        if (data.fingerCount >= config.fingerMaxCount) return false;

        // 1단계: lock — 원자성만 확보, DB I/O 없음
        if (!fingerLock.compareAndSet(false, true)) return false;
        try {
            if (data.fingerCount >= config.fingerMaxCount) return false; // double-check
            addFinger(data, victim.getServer());
        } finally {
            fingerLock.set(false);
        }

        // 2단계: lock 밖 — 비동기 저장
        JJKMod.getPlayerRepository().saveAsync(data)
                .whenComplete((v, ex) -> {
                    if (ex != null) LOGGER.error("[FingerSystem] saveAsync failed for {}", data.uuid, ex);
                });
        return true;
    }

    public void addFinger(PlayerData data, MinecraftServer server) {
        if (data.fingerCount >= config.fingerMaxCount) return;
        data.fingerCount++;
        if (data.fingerCount >= config.fingerMaxCount) {
            server.getPlayerManager().broadcast(
                Text.literal("[JJK] 료멘 스쿠나 완전부활!"), false);
            data.ceMax += 2000f;
            data.ceCurrent = data.ceMax;
        }
        // save는 호출자(tryDrop) 책임
    }

    public float getFingerCeBonus(int count) {
        if (count >= 20) return 2000f;
        if (count >= 16) return 1400f;
        if (count >= 11) return 1000f;
        if (count >= 6)  return 600f;
        if (count >= 1)  return 300f;
        return 0f;
    }

    // log10(count+1) / log10(21) — count=20 시 정확히 1.0, count=0 시 0.0
    public float getFingerAtkBonus(int count) {
        if (count <= 0) return 0f;
        return 0.30f * (float) (Math.log10(count + 1) / Math.log10(21));
    }

    /**
     * MC 경로: 스쿠나 플레이어 사망 시 처치자에게 손가락 드롭 시도.
     * S2C 알림 + FullRevivalEvents 포함.
     * sourceType: "player_sukuna" (Phase 3 확장: "cursed_spirit")
     */
    public void tryDropFromKill(UUID sourceUuid, UUID killerUuid, String sourceType) {
        synchronized (fingerLock) {
            if (droppedMobs.contains(sourceUuid.toString())) return;
            if (Math.random() >= config.fingerDropRate) return;
            droppedMobs.add(sourceUuid.toString());
        }

        PlayerData killerData = JJKMod.getPlayerRepository().load(killerUuid);
        if (killerData == null) return;
        if (killerData.fingerCount >= config.fingerMaxCount) return;

        killerData.fingerCount = Math.min(killerData.fingerCount + 1, config.fingerMaxCount);
        boolean maxReached = killerData.fingerCount >= config.fingerMaxCount;
        JJKMod.getPlayerRepository().saveImmediate(killerData);

        AuditLogger auditLogger = JJKMod.getAuditLogger();
        if (auditLogger != null) {
            auditLogger.logEvent("finger_drop", killerUuid,
                String.format("{\"count\":%d,\"source\":\"%s\",\"sourceType\":\"%s\"}",
                    killerData.fingerCount, sourceUuid, sourceType), 0L);
        }

        MinecraftServer server = JJKMod.getServer();
        if (server != null) {
            ServerPlayerEntity killer = server.getPlayerManager().getPlayer(killerUuid);
            if (killer != null) {
                ServerPlayNetworking.send(killer,
                    new FingerDropS2CPacket(killerData.fingerCount, maxReached));
                AdvancementTriggerManager.onFingerCollect(killer, killerData.fingerCount);
            }
            if (maxReached) {
                FullRevivalEvents.trigger(killerUuid, server);
            }
        }
    }

    public float getFingerSkillDmgBonus(int count) {
        if (count <= 0) return 0f;
        return 0.25f * (float) (Math.log10(count + 1) / Math.log10(21));
    }

}
