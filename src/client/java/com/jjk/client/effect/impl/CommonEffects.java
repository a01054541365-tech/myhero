package com.jjk.client.effect.impl;

import com.jjk.client.effect.SkillEffectRegistry;
import com.jjk.client.fx.ParticleThrottle;
import com.jjk.client.hud.BlackFlashOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

/** 공통 피격·흑섬·영역 이펙트. 호무라쿠이 경보 포함. */
@Environment(EnvType.CLIENT)
public final class CommonEffects {
    private CommonEffects() {}

    public static void register() {
        SkillEffectRegistry.register("hit_normal",     (pkt, w) -> spawnHit(w, pkt.x(), pkt.y(), pkt.z(), 8));
        SkillEffectRegistry.register("hit_black_flash",(pkt, w) -> spawnBlackFlash(w, pkt.x(), pkt.y(), pkt.z(), pkt.intensity()));
        SkillEffectRegistry.register("hit_soul_damage",(pkt, w) -> spawnBurst(w, ParticleTypes.SOUL, pkt.x(), pkt.y(), pkt.z(), 16));
        SkillEffectRegistry.register("homuraku_explode_warning", (pkt, w) -> spawnExplodeWarning(w, pkt.x(), pkt.y(), pkt.z()));
    }

    private static void spawnHit(ClientWorld w, double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.CRIT, x, y, z,
                (w.random.nextDouble() - 0.5) * 0.4, w.random.nextDouble() * 0.3,
                (w.random.nextDouble() - 0.5) * 0.4);
        }
    }

    private static void spawnBlackFlash(ClientWorld w, double x, double y, double z, float intensity) {
        int count = (int)(32 * intensity);
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.SOUL, x, y, z,
                (w.random.nextDouble() - 0.5) * 0.3, 0.1, (w.random.nextDouble() - 0.5) * 0.3);
        }
        if (ParticleThrottle.canSpawn()) w.addParticle(ParticleTypes.FLASH, x, y, z, 0, 0, 0);
        BlackFlashOverlay.INSTANCE.triggerHitFlash();
    }

    private static void spawnExplodeWarning(ClientWorld w, double x, double y, double z) {
        int count = 24;
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / count;
            w.addParticle(ParticleTypes.FLAME,
                x + Math.cos(angle) * 3.0, y, z + Math.sin(angle) * 3.0,
                Math.cos(angle) * 0.1, 0.05, Math.sin(angle) * 0.1);
        }
        if (ParticleThrottle.canSpawn()) w.addParticle(ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
    }

    static void spawnBurst(ClientWorld w, net.minecraft.particle.ParticleEffect type,
                            double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(type, x, y, z,
                (w.random.nextDouble() - 0.5) * 0.5, w.random.nextDouble() * 0.3,
                (w.random.nextDouble() - 0.5) * 0.5);
        }
    }

    static void spawnSphere(ClientWorld w, net.minecraft.particle.ParticleEffect type,
                             double cx, double cy, double cz, double radius, int count) {
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double theta = w.random.nextDouble() * 2 * Math.PI;
            double phi   = w.random.nextDouble() * Math.PI;
            double dx = radius * Math.sin(phi) * Math.cos(theta);
            double dy = radius * Math.cos(phi);
            double dz = radius * Math.sin(phi) * Math.sin(theta);
            w.addParticle(type, cx + dx, cy + dy, cz + dz, 0, 0, 0);
        }
    }

    static void spawnLine(ClientWorld w, net.minecraft.particle.ParticleEffect type,
                           double x, double y, double z,
                           double dirX, double dirY, double dirZ, int count) {
        double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (len < 0.001) { dirX = 1; len = 1; }
        double nx = dirX / len, ny = dirY / len, nz = dirZ / len;
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double t = i / (double) count * 5.0;
            w.addParticle(type, x + nx * t, y + ny * t, z + nz * t, nx * 0.1, ny * 0.1, nz * 0.1);
        }
    }
}
