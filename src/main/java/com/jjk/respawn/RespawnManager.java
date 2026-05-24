package com.jjk.respawn;

import com.jjk.JjkConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RespawnManager {

    private final JjkConfig config;
    private final Map<UUID, Integer> pendingRespawns = new HashMap<>();

    public RespawnManager(JjkConfig config) {
        this.config = config;
    }

    public void scheduleRespawn(ServerPlayerEntity player) {
        scheduleRespawn(player.getUuid(), config.respawnDelayTicks);
    }

    public void scheduleRespawn(UUID uuid, int delayTicks) {
        pendingRespawns.put(uuid, delayTicks);
    }

    public void tick() {
        pendingRespawns.replaceAll((uuid, ticks) -> ticks - 1);
        pendingRespawns.entrySet().removeIf(e -> {
            if (e.getValue() <= 0) {
                triggerRespawn(e.getKey());
                return true;
            }
            return false;
        });
    }

    private void triggerRespawn(UUID uuid) {
        // TODO: find player, restore HP/CE percent, send RespawnS2CPacket
    }
}
