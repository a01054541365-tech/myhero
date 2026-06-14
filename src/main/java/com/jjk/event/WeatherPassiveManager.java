package com.jjk.event;

import com.jjk.ce.CEManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/** 비 오는 날 CE 자연 재생 보너스 부여. */
public class WeatherPassiveManager {

    public void tick(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        if (world == null) return;

        CEManager.weatherRegenBonus = world.isRaining() ? 0.2f : 0.0f;
    }
}
