package com.jjk.curtain;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import com.jjk.data.PlayerRepository;
import com.jjk.network.s2c.CurtainEnterS2CPacket;
import com.jjk.network.s2c.CurtainExitS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CurtainManager {

    public record CurtainInstance(
            UUID ownerId,
            String curtainType,
            Vec3d center,
            int radius,
            long deployedAtTick,
            long expiresAtTick,
            Set<UUID> playersInside
    ) {}

    private final JjkConfig config;
    private final PlayerRepository repository;
    private final Map<UUID, CurtainInstance> activeCurtains = new ConcurrentHashMap<>();

    public CurtainManager(JjkConfig config, PlayerRepository repository) {
        this.config = config;
        this.repository = repository;
    }

    public boolean deployCurtain(ServerPlayerEntity player, PlayerData data,
                                  String curtainType, long currentTick) {
        float ceCost = "basic".equals(curtainType)
                ? config.curtainBasicCeCost() : config.curtainSpecialCeCost();
        if (data.ceCurrent < ceCost) return false;
        if (currentTick < data.curtainCooldownUntil) return false;

        data.ceCurrent -= ceCost;

        int cd = "basic".equals(curtainType)
                ? config.curtainBasicCooldownTicks() : config.curtainSpecialCooldownTicks();
        data.curtainCooldownUntil = currentTick + cd;

        int radius = "basic".equals(curtainType)
                ? config.curtainBasicRadius() : config.curtainSpecialRadius();
        long duration = "basic".equals(curtainType)
                ? config.curtainBasicDurationTicks() : config.curtainSpecialDurationTicks();

        CurtainInstance curtain = new CurtainInstance(
                player.getUuid(), curtainType,
                player.getPos(), radius,
                currentTick, currentTick + duration,
                new HashSet<>()
        );
        activeCurtains.put(player.getUuid(), curtain);
        data.curtainActive = true;
        repository.saveImmediate(data);
        return true;
    }

    public void tickCurtains(ServerWorld world, long currentTick) {
        Iterator<Map.Entry<UUID, CurtainInstance>> it = activeCurtains.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CurtainInstance> entry = it.next();
            UUID ownerId = entry.getKey();
            CurtainInstance curtain = entry.getValue();

            if (currentTick >= curtain.expiresAtTick()) {
                collapseCurtain(ownerId, world);
                it.remove();
                continue;
            }

            PlayerData ownerData = repository.load(ownerId);
            float drain = "basic".equals(curtain.curtainType())
                    ? config.curtainBasicCePerTick() : config.curtainSpecialCePerTick();
            ownerData.ceCurrent -= drain;
            if (ownerData.ceCurrent <= 0f) {
                ownerData.ceCurrent = 0f;
                collapseCurtain(ownerId, world);
                ownerData.curtainActive = false;
                repository.save(ownerData);
                it.remove();
                continue;
            }
            repository.save(ownerData);
            tickPlayerTracking(curtain, world);
        }
    }

    private void tickPlayerTracking(CurtainInstance curtain, ServerWorld world) {
        // BUG-01 fix: 접속 해제된 플레이어 UUID를 playersInside에서 즉시 제거 (메모리 누수 방지)
        Set<UUID> online = world.getPlayers().stream()
                .map(ServerPlayerEntity::getUuid)
                .collect(Collectors.toSet());
        curtain.playersInside().retainAll(online);

        for (ServerPlayerEntity p : world.getPlayers()) {
            UUID uuid = p.getUuid();
            boolean inside = isInsideCurtain(p.getPos(), curtain);
            boolean was = curtain.playersInside().contains(uuid);
            if (inside && !was) {
                curtain.playersInside().add(uuid);
                ServerPlayNetworking.send(p, new CurtainEnterS2CPacket(curtain.ownerId(), curtain.radius()));
            } else if (!inside && was) {
                curtain.playersInside().remove(uuid);
                ServerPlayNetworking.send(p, new CurtainExitS2CPacket(curtain.ownerId()));
            }
        }
    }

    private void collapseCurtain(UUID ownerId, ServerWorld world) {
        CurtainInstance curtain = activeCurtains.get(ownerId);
        if (curtain == null) return;
        for (UUID uuid : curtain.playersInside()) {
            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(uuid);
            if (p != null) ServerPlayNetworking.send(p, new CurtainExitS2CPacket(ownerId));
        }
        curtain.playersInside().clear();
        PlayerData ownerData = repository.load(ownerId);
        ownerData.curtainActive = false;
        repository.save(ownerData);
    }

    public static boolean isInsideCurtain(Vec3d pos, CurtainInstance curtain) {
        double dx = pos.x - curtain.center().x;
        double dz = pos.z - curtain.center().z;
        return (dx * dx + dz * dz) <= (double)(curtain.radius() * curtain.radius());
    }

    public CurtainInstance getCurtainAt(Vec3d pos) {
        for (CurtainInstance c : activeCurtains.values()) {
            if (isInsideCurtain(pos, c)) return c;
        }
        return null;
    }

    /** 경계 통과 차단: 내부→외부(기본·특수 공통) + 외부→내부(촉탁식 완전 봉쇄) */
    public void enforceOnPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        for (CurtainInstance curtain : activeCurtains.values()) {
            boolean inside = curtain.playersInside().contains(uuid);
            boolean isOwner = curtain.ownerId().equals(uuid);

            if (inside && !isInsideCurtain(player.getPos(), curtain)) {
                // 내부 → 외부 이동 차단 (기본·특수 공통)
                Vec3d center = curtain.center();
                Vec3d toPlayer = player.getPos().subtract(center);
                double len = toPlayer.horizontalLength();
                double r = curtain.radius() - 0.5;
                if (len > 0) {
                    Vec3d boundary = center.add(toPlayer.multiply(r / len));
                    player.teleport(boundary.x, player.getY(), boundary.z, true);
                }
            } else if (!inside && !isOwner && "special".equals(curtain.curtainType())
                    && isInsideCurtain(player.getPos(), curtain)) {
                // 외부 → 내부 진입 차단 (촉탁식 완전 봉쇄, 시전자 제외)
                Vec3d center = curtain.center();
                Vec3d toPlayer = player.getPos().subtract(center);
                double len = toPlayer.horizontalLength();
                double r = curtain.radius() + 0.5;
                if (len > 0) {
                    Vec3d outside = center.add(toPlayer.multiply(r / len));
                    player.teleport(outside.x, player.getY(), outside.z, true);
                } else {
                    player.teleport(center.x + curtain.radius() + 1.0, player.getY(), center.z, true);
                }
            }
        }
    }
}
