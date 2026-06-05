package com.jjk.client.fx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

/** 파티클 스폰 빈도 제한 — TPS 보호용. 틱당 최대 MAX_PER_TICK개. */
@Environment(EnvType.CLIENT)
public final class ParticleThrottle {
    private ParticleThrottle() {}

    private static final int MAX_PER_TICK = 60;
    private static long lastTick = -1L;
    private static int countThisTick = 0;

    public static boolean canSpawn() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return false;
        long now = mc.world.getTime();
        if (now != lastTick) {
            lastTick = now;
            countThisTick = 0;
        }
        if (countThisTick >= MAX_PER_TICK) return false;
        countThisTick++;
        return true;
    }
}
