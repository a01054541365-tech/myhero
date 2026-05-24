package com.jjk.combat;

import com.jjk.data.PlayerData;

public final class CooldownManager {

    private CooldownManager() {}

    public static boolean isReady(PlayerData data, String key, long currentTick) {
        Long expiry = data.cooldowns.get(key);
        return expiry == null || currentTick >= expiry;
    }

    public static void set(PlayerData data, String key, long currentTick, int durationTicks) {
        data.cooldowns.put(key, currentTick + (long) durationTicks);
    }

    public static long expiryOf(PlayerData data, String key) {
        return data.cooldowns.getOrDefault(key, 0L);
    }
}
