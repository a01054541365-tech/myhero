package com.jjk.client.effect;

import com.jjk.client.JjkClientState;
import com.jjk.client.fx.ParticleThrottle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;

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

        // 영역 활성 중: 매 8틱 — 플레이어 주변 압박감 있는 주력 입자 하강
        if (JjkClientState.isInDomain() && ambientTick % 8 == 0) {
            spawnDomainAmbient(mc);
        }

        // 어두운 지하 구역(하늘빛 0 + 블록광 5 이하): 매 30틱 — 주력 감지 분위기
        if (ambientTick % 30 == 0 && isInDarkArea(mc)) {
            spawnDarkAreaAmbient(mc);
        }
    }

    private static boolean isInDarkArea(MinecraftClient mc) {
        BlockPos pos = mc.player.getBlockPos();
        return mc.world.getLightLevel(LightType.SKY, pos) == 0
            && mc.world.getLightLevel(LightType.BLOCK, pos) <= 5;
    }

    private static void spawnDomainAmbient(MinecraftClient mc) {
        for (int i = 0; i < 3; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double px = mc.player.getX() + (mc.world.random.nextDouble() - 0.5) * 24;
            double py = mc.player.getY() + 2 + mc.world.random.nextDouble() * 6;
            double pz = mc.player.getZ() + (mc.world.random.nextDouble() - 0.5) * 24;
            mc.world.addParticle(
                mc.world.random.nextInt(4) == 0 ? ParticleTypes.WITCH : ParticleTypes.PORTAL,
                px, py, pz, 0, -0.04, 0);
        }
    }

    private static void spawnDarkAreaAmbient(MinecraftClient mc) {
        int count = 1 + mc.world.random.nextInt(2);
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double px = mc.player.getX() + (mc.world.random.nextDouble() - 0.5) * 16;
            double py = mc.player.getY() + (mc.world.random.nextDouble() - 0.5) * 4;
            double pz = mc.player.getZ() + (mc.world.random.nextDouble() - 0.5) * 16;
            mc.world.addParticle(ParticleTypes.ASH, px, py, pz, 0, 0.01, 0);
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
