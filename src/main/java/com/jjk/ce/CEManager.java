package com.jjk.ce;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

public class CEManager {

    private final JjkConfig config;
    private final CEPool pool = new CEPool();

    public CEManager(JjkConfig config) {
        this.config = config;
    }

    public void regenTick(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.characterId == null) return;

        long currentTick = player.getWorld().getTime();
        CERegenRule rule = pool.getRule(data.characterId);
        float rate = rule.regenPerTick(data, currentTick, config);

        data.ceCurrent = Math.min(data.ceCurrent + rate, data.ceMax);

        // §6-2 고죠 무한 유지비: 30/s = 1.5/틱, CE 부족 시 자동 해제
        if (data.infinityActive) {
            if (data.ceCurrent < 1.5f) {
                data.infinityActive = false;
            } else {
                data.ceCurrent -= 1.5f;
            }
        }

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

    // Pure-data overloads (CombatPipeline.processData 및 테스트용)
    public boolean consumeCE(PlayerData data, float amount) {
        // 잭팟 중 CE CAP 체크 우회 — 차감은 항상 진행, 음수 보호만 유지
        if (!data.jackpotActive && data.ceCurrent < amount) return false;
        data.ceCurrent = Math.max(0f, data.ceCurrent - amount);
        return true;
    }

    public void refundCE(PlayerData data, float amount) {
        data.ceCurrent = Math.min(data.ceCurrent + amount, data.ceMax);
    }
}
