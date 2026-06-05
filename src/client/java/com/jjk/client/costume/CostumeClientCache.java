package com.jjk.client.costume;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class CostumeClientCache {
    private CostumeClientCache() {}

    private static final Map<UUID, String> CACHE = new ConcurrentHashMap<>();

    public static void update(UUID uuid, String costumeId) {
        CACHE.put(uuid, costumeId);
    }

    public static String get(UUID uuid) {
        return CACHE.getOrDefault(uuid, "default");
    }

    public static void clear() {
        CACHE.clear();
    }
}
