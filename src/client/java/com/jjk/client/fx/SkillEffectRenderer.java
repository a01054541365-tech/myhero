package com.jjk.client.fx;

import com.jjk.client.effect.SkillEffectRegistry;
import com.jjk.client.hud.BlackFlashOverlay;
import com.jjk.network.s2c.SkillEffectS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

import java.util.Random;

@Environment(EnvType.CLIENT)
public final class SkillEffectRenderer {
    private SkillEffectRenderer() {}

    private static final Random RNG = new Random();

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SkillEffectS2CPacket.ID, (pkt, ctx) ->
            ctx.client().execute(() -> handle(pkt, ctx.client())));
    }

    private static void handle(SkillEffectS2CPacket pkt, MinecraftClient mc) {
        if (mc.world == null) return;

        // 1차: SkillEffectRegistry에 등록된 핸들러
        if (SkillEffectRegistry.dispatch(pkt, mc)) return;

        // 2차: 기존 폴백 switch (이전 effectType 문자열 호환)
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        try {
            switch (pkt.effectType()) {
                case "GOJO_BLUE_PULL"     -> spawnSphere(mc.world, ParticleTypes.DRAGON_BREATH, x, y, z, 3.0, 24);
                case "GOJO_RED_BLAST"     -> spawnExplosion(mc.world, x, y, z);
                case "GOJO_PURPLE_BEAM"   -> spawnLine(mc.world, ParticleTypes.END_ROD, x, y, z, 20);
                case "SUKUNA_SLASH"       -> spawnSweep(mc.world, x, y, z, 1);
                case "SUKUNA_CLEAVE"      -> spawnSweep(mc.world, x, y, z, 4);
                case "MAHITO_SOUL_SLASH"  -> spawnBurst(mc.world, ParticleTypes.PORTAL, x, y, z, 18);
                case "ITADORI_BLACK_FLASH" -> {
                    if (ParticleThrottle.canSpawn()) {
                        mc.world.addParticle(ParticleTypes.FLASH, x, y, z, 0, 0, 0);
                        BlackFlashOverlay.INSTANCE.triggerHitFlash();
                    }
                }
                case "NANAMI_RATIO"       -> spawnBurst(mc.world, ParticleTypes.CRIT, x, y, z, 12);
                case "DOMAIN_OPEN"        -> spawnSphere(mc.world, ParticleTypes.PORTAL, x, y, z, 20.0, 80);
                case "DOMAIN_CLOSE"       -> {
                    if (ParticleThrottle.canSpawn())
                        mc.world.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 0, 0, 0);
                }
                case "AWAKENING_TRIGGER"  -> spawnAwakeningUpdraft(mc.world, x, y, z);
            }
        } catch (Exception ignored) {}
    }

    // ── 파티클 형태 헬퍼 ────────────────────────────────────────────────────

    /** 각성 — 발밑 기류 링 + 주변 3블록 내 파티클 부양 (spec_05 §1-3). */
    private static void spawnAwakeningUpdraft(ClientWorld world, double x, double y, double z) {
        for (int i = 0; i < 16; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / 16;
            world.addParticle(ParticleTypes.CLOUD,
                x + Math.cos(angle) * 1.2, y + 0.1, z + Math.sin(angle) * 1.2,
                Math.cos(angle) * 0.08, 0.02, Math.sin(angle) * 0.08);
        }
        for (int i = 0; i < 30; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = RNG.nextDouble() * 2 * Math.PI;
            double r = RNG.nextDouble() * 3.0;
            world.addParticle(i % 3 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.END_ROD,
                x + Math.cos(angle) * r,
                y + RNG.nextDouble() * 0.5,
                z + Math.sin(angle) * r,
                0, 0.35 + RNG.nextDouble() * 0.35, 0);
        }
    }

    private static void spawnSphere(ClientWorld world, net.minecraft.particle.ParticleEffect type,
                                     double cx, double cy, double cz, double radius, int count) {
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double theta = RNG.nextDouble() * 2 * Math.PI;
            double phi   = RNG.nextDouble() * Math.PI;
            double dx = radius * Math.sin(phi) * Math.cos(theta);
            double dy = radius * Math.cos(phi);
            double dz = radius * Math.sin(phi) * Math.sin(theta);
            world.addParticle(type, cx + dx, cy + dy, cz + dz, 0, 0, 0);
        }
    }

    private static void spawnBurst(ClientWorld world, net.minecraft.particle.ParticleEffect type,
                                    double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            world.addParticle(type, x, y, z,
                (RNG.nextDouble() - 0.5) * 0.6,
                RNG.nextDouble() * 0.4,
                (RNG.nextDouble() - 0.5) * 0.6);
        }
    }

    private static void spawnExplosion(ClientWorld world, double x, double y, double z) {
        if (ParticleThrottle.canSpawn())
            world.addParticle(ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
        for (int i = 0; i < 8; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = i * Math.PI / 4;
            world.addParticle(ParticleTypes.EXPLOSION,
                x + Math.cos(angle) * 1.5, y, z + Math.sin(angle) * 1.5, 0, 0, 0);
        }
    }

    private static void spawnLine(ClientWorld world, net.minecraft.particle.ParticleEffect type,
                                   double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double t = i / (double) count;
            world.addParticle(type, x + t * 3, y, z + t * 3, 0, 0.05, 0);
        }
    }

    private static void spawnSweep(ClientWorld world, double x, double y, double z, int directions) {
        double step = (directions > 1) ? (Math.PI * 2 / directions) : 0;
        for (int d = 0; d < Math.max(1, directions); d++) {
            double angle = d * step;
            double vx = Math.cos(angle) * 0.3;
            double vz = Math.sin(angle) * 0.3;
            for (int i = 0; i < 5; i++) {
                if (!ParticleThrottle.canSpawn()) return;
                world.addParticle(ParticleTypes.SWEEP_ATTACK,
                    x + vx * i, y + 1.0, z + vz * i, vx, 0, vz);
            }
        }
    }
}
