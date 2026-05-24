package com.jjk.effect;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

public final class EffectManager {

    private EffectManager() {}

    public static void apply(ServerPlayerEntity target, EffectType type, int durationTicks) {
        switch (type) {
            case BURN -> target.setOnFireFor(Math.max(1, durationTicks / 20));
            case STUN -> storeStatus(target, "status_stun_until", durationTicks);
            case SLEEP -> storeStatus(target, "status_sleep_until", durationTicks);
            case SPEED_UP -> storeStatus(target, "status_speed_up_until", durationTicks);
            case SPEED_DOWN -> storeStatus(target, "status_speed_down_until", durationTicks);
        }
    }

    private static void storeStatus(ServerPlayerEntity target, String key, int durationTicks) {
        PlayerData data = JJKMod.getPlayerRepository().load(target.getUuid());
        data.cooldowns.put(key, target.getWorld().getTime() + durationTicks);
        JJKMod.getPlayerRepository().save(data);
    }
}
