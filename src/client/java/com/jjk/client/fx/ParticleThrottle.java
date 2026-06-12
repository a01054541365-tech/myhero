package com.jjk.client.fx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

/**
 * 파티클 스폰 빈도 제한 — 렌더 부하 보호용.
 * 서버 TPS는 클라이언트에 동기화되지 않으므로 FPS를 부하 지표로 사용해
 * 틱당 스폰 상한을 동적으로 조정한다.
 */
@Environment(EnvType.CLIENT)
public final class ParticleThrottle {
    private ParticleThrottle() {}

    private static long lastTick = -1L;
    private static int countThisTick = 0;

    /** FPS 구간별 틱당 상한: 60+ → 60개, 45+ → 40개, 30+ → 24개, 그 미만 → 12개. */
    private static int maxPerTick(MinecraftClient mc) {
        int fps = mc.getCurrentFps();
        if (fps >= 60) return 60;
        if (fps >= 45) return 40;
        if (fps >= 30) return 24;
        return 12;
    }

    public static boolean canSpawn() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return false;
        long now = mc.world.getTime();
        if (now != lastTick) {
            lastTick = now;
            countThisTick = 0;
        }
        if (countThisTick >= maxPerTick(mc)) return false;
        countThisTick++;
        return true;
    }
}
