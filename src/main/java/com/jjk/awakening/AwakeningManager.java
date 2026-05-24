package com.jjk.awakening;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

// 吏쟊OCK: HP 30% / 160??/ 2400??????륂뒄 癰궰野???AwakeningManagerTest????ｍ뜞 ??륁젟
public class AwakeningManager {

    private static final float AWAKENING_HP_THRESHOLD = 0.30f;
    private static final int AWAKENING_DURATION_TICKS = 160;
    private static final int AWAKENING_COOLDOWN_TICKS = 2400;

    private final JjkConfig config;

    public AwakeningManager(JjkConfig config) {
        this.config = config;
    }

    public void tickCheck(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long currentTick = player.getWorld().getTime();

        if (data.awakeningActive && currentTick >= data.awakeningEndTick) {
            deactivate(data, currentTick);
            JJKMod.getPlayerRepository().save(data);
        }
    }

    public boolean checkAndActivate(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long currentTick = player.getWorld().getTime();

        if (data.awakeningActive) return false;
        if (currentTick < data.awakeningCooldownUntil) return false;

        float hpRatio = player.getHealth() / player.getMaxHealth();
        if (hpRatio > AWAKENING_HP_THRESHOLD) return false;

        data.awakeningActive = true;
        data.awakeningEndTick = currentTick + AWAKENING_DURATION_TICKS;
        JJKMod.getPlayerRepository().save(data);
        // TODO: send AwakeningS2CPacket
        return true;
    }

    private void deactivate(PlayerData data, long currentTick) {
        data.awakeningActive = false;
        data.awakeningEndTick = 0;
        data.awakeningCooldownUntil = currentTick + AWAKENING_COOLDOWN_TICKS;
    }
}
