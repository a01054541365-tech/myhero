package com.jjk.character.impl;

import com.jjk.JjkConfig;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ShadowMarkerRegistry {

    private final ConcurrentHashMap<UUID, BlockPos> markers      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>     markerExpiry = new ConcurrentHashMap<>();
    private final JjkConfig config;

    public ShadowMarkerRegistry(JjkConfig config) {
        this.config = config;
    }

    public void placeMarker(UUID playerUuid, BlockPos pos, long currentTick) {
        markers.put(playerUuid, pos);
        markerExpiry.put(playerUuid, currentTick + config.shadowMarkerLifetimeTicks);
    }

    public BlockPos getMarker(UUID playerUuid, long currentTick) {
        Long expiry = markerExpiry.get(playerUuid);
        if (expiry == null || currentTick >= expiry) {
            markers.remove(playerUuid);
            markerExpiry.remove(playerUuid);
            return null;
        }
        return markers.get(playerUuid);
    }

    public void clearMarker(UUID playerUuid) {
        markers.remove(playerUuid);
        markerExpiry.remove(playerUuid);
    }
}
