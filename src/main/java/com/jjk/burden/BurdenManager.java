package com.jjk.burden;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BurdenManager {

    private final JjkConfig config;
    private final Map<UUID, Float> burdenFraction = new HashMap<>();

    public BurdenManager(JjkConfig config) {
        this.config = config;
    }

    public void tick(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long currentTick = player.getWorld().getTime();
        tickInternal(data, currentTick);
        JJKMod.getPlayerRepository().save(data);
    }

    void tickInternal(PlayerData data, long currentTick) {
        if (!"inumaki".equals(data.characterId)) return;
        if (data.burden <= 0) return;

        if (data.burden > config.burdenSealThreshold()) {
            data.cooldowns.put("skill_seal", currentTick + config.sealDurationTicks);
            data.burden = 0;
            burdenFraction.remove(data.uuid);
            return;
        }

        boolean inCombat = (currentTick - data.lastCombatTick) < 100;
        float reduction = inCombat
            ? (float) config.burdenDecayInCombat()
            : (float) config.burdenDecayOutOfCombat();
        float acc = burdenFraction.getOrDefault(data.uuid, 0f) + reduction;
        int drop = (int) acc;
        burdenFraction.put(data.uuid, acc - drop);
        if (drop > 0) {
            data.burden = Math.max(0, data.burden - drop);
        }
    }

    public void addBurden(ServerPlayerEntity player, int amount) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        addBurdenInternal(data, amount);
        JJKMod.getPlayerRepository().save(data);
    }

    void addBurdenInternal(PlayerData data, int amount) {
        data.burden += amount;
    }

    // ── Static API (InumakiSkillSet 및 테스트용) ─────────────────────────────

    /** 부담 누적. 100 초과 시 봉인 세팅 후 초기화. */
    public static void addBurden(PlayerData data, float amount, long tick, JjkConfig config) {
        data.burden += (int) amount;
        if (data.burden > 100) {
            data.cooldowns.put("skill_seal", tick + config.sealDurationTicks);
            data.burden = 0;
        }
    }

    public static boolean isSealed(PlayerData data, long currentTick) {
        return data.cooldowns.getOrDefault("skill_seal", 0L) > currentTick;
    }
}
