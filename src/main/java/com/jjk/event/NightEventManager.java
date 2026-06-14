package com.jjk.event;

import com.jjk.entity.CursedSpiritSpawnManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/** 야간(timeOfDay 13000~23000) 마계 이벤트 — 저주령 등장률 및 경험치 배율 상승. */
public class NightEventManager {

    public static float xpNightMultiplier = 1.0f;

    private boolean nightActive = false;

    public void tick(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        if (world == null) return;

        long time = world.getTimeOfDay() % 24000;
        boolean isNight = time >= 13000 && time < 23000;

        if (isNight && !nightActive) {
            nightActive = true;
            CursedSpiritSpawnManager.nightMultiplier = 1.5f;
            xpNightMultiplier = 2.0f;
        } else if (!isNight && nightActive) {
            nightActive = false;
            CursedSpiritSpawnManager.nightMultiplier = 1.0f;
            xpNightMultiplier = 1.0f;
        }
    }
}
