package com.jjk.character.impl;

import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.ThreadLocalRandom;

public class JackpotStateMachine {

    // §LOCK: 1/239 확률 고정
    private static final int JACKPOT_ODDS = 239;

    public static boolean tryJackpot(PlayerData data, long tick, JjkConfig config) {
        if (ThreadLocalRandom.current().nextInt(JACKPOT_ODDS) == 0) {
            data.jackpotActive = true;
            data.jackpotEndTick = tick + config.getJackpotDurationTicks();
            return true;
        }
        return false;
    }

    public static void tickJackpot(PlayerData data, long tick, ServerPlayerEntity player) {
        if (!data.jackpotActive) return;
        if (tick >= data.jackpotEndTick) {
            data.jackpotActive = false;
            data.jackpotEndTick = 0L;
        } else {
            data.hpCurrent = Math.min(data.hpCurrent + 1f, data.hpMax);
        }
    }

    public static boolean isJackpotActive(PlayerData data, long tick) {
        return data.jackpotActive && tick < data.jackpotEndTick;
    }
}
