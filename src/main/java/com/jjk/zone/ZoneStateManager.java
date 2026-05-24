package com.jjk.zone;

import com.jjk.JJKMod;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ZoneStateManager {

    private final Set<UUID> playersInZone = new HashSet<>();

    public void tick(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean insideNow = JJKMod.getDomainManager()
                .getDomainAt(player.getBlockPos()).isPresent();

        if (insideNow && !playersInZone.contains(uuid)) {
            playersInZone.add(uuid);
            onEnter(player);
        } else if (!insideNow && playersInZone.contains(uuid)) {
            playersInZone.remove(uuid);
            onExit(player);
        }
    }

    public boolean isInZone(UUID uuid) {
        return playersInZone.contains(uuid);
    }

    private void onEnter(ServerPlayerEntity player) {
        // TODO: send ZoneEnterS2CPacket, apply zone penalty
    }

    private void onExit(ServerPlayerEntity player) {
        // TODO: send ZoneExitS2CPacket, remove zone penalty
    }
}
