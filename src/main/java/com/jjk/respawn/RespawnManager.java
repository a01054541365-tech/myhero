package com.jjk.respawn;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.RespawnS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
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

    // §26-3: restore HP/CE, clear awakening·zone·binding state, teleport to spawn
    private void triggerRespawn(UUID uuid) {
        ServerPlayerEntity player = JJKMod.getServer().getPlayerManager().getPlayer(uuid);
        if (player == null) return;

        PlayerData data = JJKMod.getPlayerRepository().load(uuid);
        float respawnHp = (float) (data.hpMax * config.respawnHpPercent);
        data.hpCurrent = respawnHp;
        data.ceCurrent = (float) (data.ceMax * config.respawnCePercent);
        data.awakeningActive = false;
        data.awakeningEndTick = 0;
        data.zoneActive = false;
        data.zoneEndTick = 0;
        data.bindingVowDeclaredTick = 0;
        JJKMod.getPlayerRepository().saveImmediate(data);

        player.setHealth(respawnHp);
        BlockPos spawn = player.getServerWorld().getSpawnPos();
        player.teleport(player.getServerWorld(),
                spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                player.getYaw(), player.getPitch());
        ServerPlayNetworking.send(player, new RespawnS2CPacket(0));
    }
}
