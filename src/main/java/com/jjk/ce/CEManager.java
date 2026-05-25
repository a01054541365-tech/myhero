package com.jjk.ce;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.character.CharacterRegistry;
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

        if ("sukuna".equals(data.characterId)) {
            float baseMax = CharacterRegistry.get("sukuna").ceMax();
            float bonus = JJKMod.getFingerSystem()
                .getFingerCeBonus(data.fingerCount);
            data.ceMax = baseMax + bonus;
        }

        long currentTick = player.getWorld().getTime();
        boolean inCombat = (currentTick - data.lastCombatTick) < COMBAT_WINDOW_TICKS;
        float rate = inCombat ? (float) config.ceRegenInCombat : (float) config.ceRegenOutOfCombat;

        if ("nanami".equals(data.characterId) && data.overtimeWork) {
            rate *= 1.5f;
        }

        if ("hakari".equals(data.characterId) && data.jackpotActive) {
            // §LOCK: CE regen 상한 max_ce × 20%/s = max_ce × 0.01/틱
            float jackpotCap = data.ceMax * 0.01f;
            rate = Math.min(rate, jackpotCap);
            // §LOCK: 자동 치유 1HP/틱
            data.hpCurrent = Math.min(data.hpCurrent + 1f, data.hpMax);
        }

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
