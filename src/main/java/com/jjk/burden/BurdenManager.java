package com.jjk.burden;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

public class BurdenManager {

    private static final int SEAL_THRESHOLD = 100;
    private static final int DECAY_PER_TICK = 1;

    private final JjkConfig config;

    public BurdenManager(JjkConfig config) {
        this.config = config;
    }

    public void tick(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (!"inumaki".equals(data.characterId)) return;

        if (data.burden > 0) {
            data.burden = Math.max(0, data.burden - DECAY_PER_TICK);
            JJKMod.getPlayerRepository().save(data);
        }
    }

    public void addBurden(ServerPlayerEntity player, int amount) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        data.burden = Math.min(data.burden + amount, SEAL_THRESHOLD);

        if (data.burden >= SEAL_THRESHOLD) {
            long sealUntil = player.getWorld().getTime() + config.sealDurationTicks;
            data.cooldowns.put("skill_seal", sealUntil);
            JJKMod.getPlayerRepository().saveImmediate(data);
        } else {
            JJKMod.getPlayerRepository().save(data);
        }
    }
}
