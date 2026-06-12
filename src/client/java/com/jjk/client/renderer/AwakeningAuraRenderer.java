package com.jjk.client.renderer;

import com.jjk.client.JjkClientState;
import com.jjk.client.fx.ParticleThrottle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.random.Random;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 각성 중인 플레이어 주변에 오라 파티클 렌더.
 * WorldRenderEvents.AFTER_ENTITIES 에 등록.
 */
@Environment(EnvType.CLIENT)
public final class AwakeningAuraRenderer {

    private static final Set<UUID> awakeningPlayers = new HashSet<>();

    private AwakeningAuraRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(AwakeningAuraRenderer::onAfterEntities);
    }

    /** 서버가 AwakeningS2CPacket 으로 로컬 플레이어 상태를 전달. */
    public static void setLocalPlayerAwakening(boolean active) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (active) awakeningPlayers.add(mc.player.getUuid());
        else        awakeningPlayers.remove(mc.player.getUuid());
    }

    private static void onAfterEntities(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        Random rng = mc.world.getRandom();

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (!awakeningPlayers.contains(player.getUuid())
                    && !(player.equals(mc.player) && JjkClientState.isAwakening())) {
                continue;
            }
            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();
            // 상승 기류: TOTEM + END_ROD 혼합 4개 (spec_05 §1-3 각성 분위기)
            for (int i = 0; i < 4; i++) {
                if (!ParticleThrottle.canSpawn()) break;
                double ox = (rng.nextFloat() - 0.5f) * 1.2;
                double oy = rng.nextFloat() * 2.2;
                double oz = (rng.nextFloat() - 0.5f) * 1.2;
                mc.world.addParticle(
                    i % 2 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.END_ROD,
                    px + ox, py + oy, pz + oz,
                    (rng.nextFloat() - 0.5f) * 0.1, 0.2f + rng.nextFloat() * 0.15f,
                    (rng.nextFloat() - 0.5f) * 0.1);
            }
            // 발밑 기류: 낮은 확률로 CLOUD 1개
            if (rng.nextInt(4) == 0 && ParticleThrottle.canSpawn()) {
                double angle = rng.nextFloat() * 2 * Math.PI;
                mc.world.addParticle(ParticleTypes.CLOUD,
                    px + Math.cos(angle) * 0.9, py + 0.1, pz + Math.sin(angle) * 0.9,
                    Math.cos(angle) * 0.06, 0.03, Math.sin(angle) * 0.06);
            }
        }
    }
}
