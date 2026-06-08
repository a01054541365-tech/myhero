package com.jjk.client.effect;

import com.jjk.client.fx.ParticleThrottle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;

/**
 * 야간·날씨 연동 주변 파티클 이펙트.
 * 클라이언트 전용 — 서버 사이드 영향 없음.
 * ClientTickEvents.END_CLIENT_TICK에 등록.
 */
@Environment(EnvType.CLIENT)
public final class WorldAmbientEffects {
    private WorldAmbientEffects() {}

    private static int ambientTick = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WorldAmbientEffects::tick);
    }

    private static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        // FPS 30 이하이면 ambient 이펙트 비활성화
        if (mc.getCurrentFps() < 30) return;

        ambientTick++;

        long timeOfDay = mc.world.getTime() % 24000L;
        boolean isNight = timeOfDay >= 13000L && timeOfDay <= 23000L;

        // 야간: 매 40틱 — 플레이어 주변 랜덤 어두운 보라색 파티클 1~3개
        if (isNight && ambientTick % 40 == 0) {
            spawnNightParticles(mc);
        }

        // 비: 매 2틱, 10% 확률 — 착지 물방울 파티클
        if (mc.world.isRaining() && ambientTick % 2 == 0
                && mc.world.random.nextFloat() < 0.10f) {
            spawnRainParticles(mc);
        }
    }

    private static void spawnNightParticles(MinecraftClient mc) {
        double px = mc.player.getX() + (mc.world.random.nextDouble() - 0.5) * 32;
        double py = mc.player.getY() + (mc.world.random.nextDouble() - 0.5) * 8;
        double pz = mc.player.getZ() + (mc.world.random.nextDouble() - 0.5) * 32;
        int count = 1 + mc.world.random.nextInt(3);
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            mc.world.addParticle(ParticleTypes.PORTAL, px, py, pz,
                (mc.world.random.nextDouble() - 0.5) * 0.05,
                0.02,
                (mc.world.random.nextDouble() - 0.5) * 0.05);
        }
    }

    private static void spawnRainParticles(MinecraftClient mc) {
        if (!ParticleThrottle.canSpawn()) return;
        double px = mc.player.getX() + (mc.world.random.nextDouble() - 0.5) * 10;
        double pz = mc.player.getZ() + (mc.world.random.nextDouble() - 0.5) * 10;
        mc.world.addParticle(ParticleTypes.SPLASH,
            px, mc.player.getY(), pz,
            0, 0.05, 0);
    }
}
