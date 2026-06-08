package com.jjk.security;

import com.jjk.JJKMod;
import com.jjk.combat.AntiAbuseManager;
import com.jjk.data.PlayerData;
import com.jjk.discord.DiscordWebhook;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 서버 권위 이동 검증. 텔레포트는 nonce(현재틱+2 만료)로만 예외를 허용하고,
 * 그 외 비정상 이동은 누적 위반 횟수에 따라 롤백·플래그 처리한다.
 */
public class MovementValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-movement");

    private static final int NONCE_WINDOW_TICKS = 2;
    // 일반 이동(스프린트·점프·넉백·스킬 대시 포함) 여유를 둔 휴리스틱 임계값. §LOCK 아님.
    private static final double MAX_DISTANCE_PER_TICK = 8.0;

    private record PendingTeleport(long nonce, long expiryTick) {}

    private final Map<UUID, Vec3d> lastValidPos = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();
    private final AtomicLong nonceCounter = new AtomicLong();

    /** 서버가 의도적으로 텔레포트시키기 전에 호출 — nonce + 만료틱(현재틱+2) 저장. */
    public long markTeleportPending(UUID playerId, long currentTick) {
        long nonce = nonceCounter.incrementAndGet();
        pendingTeleports.put(playerId, new PendingTeleport(nonce, currentTick + NONCE_WINDOW_TICKS));
        return nonce;
    }

    /** TickScheduler 등록용 — 매 틱 전체 플레이어 이동 검증. */
    public void validateAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            validate(player);
        }
    }

    private void validate(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        Vec3d current = player.getPos();
        Vec3d last = lastValidPos.get(id);
        long currentTick = player.getWorld().getTime();

        if (last == null) {
            lastValidPos.put(id, current);
            return;
        }

        if (last.distanceTo(current) <= MAX_DISTANCE_PER_TICK) {
            lastValidPos.put(id, current);
            return;
        }

        PendingTeleport pending = pendingTeleports.get(id);
        if (pending != null) {
            pendingTeleports.remove(id);
            if (currentTick <= pending.expiryTick()) {
                // nonce 2틱 이내 — 텔레포트 예외 허용
                lastValidPos.put(id, current);
                return;
            }
            // 2틱 초과된 nonce는 자동 만료 → 이후 이동은 정상 검증으로 진행
        }

        recordViolation(player, currentTick);
    }

    private void recordViolation(ServerPlayerEntity player, long currentTick) {
        UUID id = player.getUuid();
        int count = violationCounts.merge(id, 1, Integer::sum);
        LOGGER.warn("[JJK-MV] 비정상 이동 감지. player={} count={}", id, count);

        if (count == 3) {
            Vec3d safe = lastValidPos.get(id);
            if (safe != null) {
                player.teleport(player.getServerWorld(), safe.x, safe.y, safe.z,
                        player.getYaw(), player.getPitch());
                LOGGER.warn("[JJK-MV] 3회 위반 — 이전 정상 위치로 롤백. player={}", id);
            }
        }

        if (count == 10) {
            AntiAbuseManager aam = JJKMod.getAntiAbuseManager();
            if (aam != null) {
                aam.flag(id, "movement_violation", 5);
            }
            DiscordWebhook.sendAsync(String.format(
                    "[안티치트] %s — 비정상 이동 10회 누적", player.getName().getString()));

            PlayerData data = JJKMod.getPlayerRepository().load(id);
            data.antiAbuseFlags.add("movement_violation@" + currentTick);
            JJKMod.getPlayerRepository().saveImmediate(data);
        }
    }

    public void clear(UUID playerId) {
        lastValidPos.remove(playerId);
        violationCounts.remove(playerId);
        pendingTeleports.remove(playerId);
    }
}
