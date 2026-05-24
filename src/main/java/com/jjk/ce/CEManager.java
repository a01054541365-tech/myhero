package com.jjk.ce;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

public class CEManager {

    private static final int COMBAT_WINDOW_TICKS = 100;

    private final JjkConfig config;

    public CEManager(JjkConfig config) {
        this.config = config;
    }

    public void regenTick(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.characterId == null) return;

        long currentTick = player.getWorld().getTime();
        boolean inCombat = (currentTick - data.lastCombatTick) < COMBAT_WINDOW_TICKS;
        float rate = inCombat ? (float) config.ceRegenInCombat : (float) config.ceRegenOutOfCombat;

        data.ceCurrent = Math.min(data.ceCurrent + rate, data.ceMax);
        JJKMod.getPlayerRepository().save(data);
    }

    public boolean canAfford(ServerPlayerEntity player, float amount) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        return data.ceCurrent >= amount;
    }

    public boolean consume(ServerPlayerEntity player, float amount) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.ceCurrent < amount) return false;
        data.ceCurrent -= amount;
        JJKMod.getPlayerRepository().save(data);
        return true;
    }
}
