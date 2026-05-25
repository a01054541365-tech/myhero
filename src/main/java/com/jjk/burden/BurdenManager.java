package com.jjk.burden;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

public class BurdenManager {

    private final JjkConfig config;

    public BurdenManager(JjkConfig config) {
        this.config = config;
    }

    public void tick(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (!"inumaki".equals(data.characterId)) return;
        if (data.burden <= 0) return;

        long currentTick = player.getWorld().getTime();
        boolean inCombat = (currentTick - data.lastCombatTick) < 100;
        float reduction = inCombat ? 0.1f : 0.25f;
        data.burden = Math.max(0, (int)(data.burden - reduction));

        if (data.burden > 100) {
            data.cooldowns.put("skill_seal", currentTick + config.sealDurationTicks);
            data.burden = 0;
        }

        JJKMod.getPlayerRepository().save(data);
    }

    public void addBurden(ServerPlayerEntity player, int amount) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        data.burden += amount;
        JJKMod.getPlayerRepository().save(data);
    }
}
