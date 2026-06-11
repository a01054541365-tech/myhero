package com.jjk.client.effect.impl;

import com.jjk.client.effect.SkillEffectRegistry;
import com.jjk.client.fx.ParticleThrottle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

/** 고죠 사토루 시그니처 이펙트 */
@Environment(EnvType.CLIENT)
public final class GojoEffects {
    private GojoEffects() {}

    public static void register() {
        SkillEffectRegistry.register("gojo_blue",            GojoEffects::blue);
        SkillEffectRegistry.register("gojo_red",             GojoEffects::red);
        SkillEffectRegistry.register("gojo_purple",          GojoEffects::purple);
        SkillEffectRegistry.register("gojo_infinity_block",  GojoEffects::infinityBlock);
        SkillEffectRegistry.register("gojo_domain",          GojoEffects::domain);
    }

    private static void blue(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y() + 1.0, z = pkt.z();
        int count = (int)(40 * pkt.intensity());
        int rings = 3;
        for (int r = rings; r >= 1; r--) {
            int perRing = count / rings;
            for (int i = 0; i < perRing; i++) {
                if (!ParticleThrottle.canSpawn()) return;
                double angle = 2 * Math.PI * i / perRing;
                double radius = r * 1.5;
                double vx = -Math.cos(angle) * 0.15 * r;
                double vz = -Math.sin(angle) * 0.15 * r;
                w.addParticle(ParticleTypes.DRAGON_BREATH,
                    x + Math.cos(angle) * radius,
                    y + w.random.nextDouble() * 0.5,
                    z + Math.sin(angle) * radius,
                    vx, 0.02, vz);
            }
        }
        for (int i = 0; i < 8; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.END_ROD, x, y, z,
                (w.random.nextDouble() - 0.5) * 0.05,
                w.random.nextDouble() * 0.05,
                (w.random.nextDouble() - 0.5) * 0.05);
        }
    }

    private static void red(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y() + 1.0, z = pkt.z();
        int count = (int)(40 * pkt.intensity());
        int[] radii = {2, 4, 6};
        for (int ri = 0; ri < radii.length; ri++) {
            int perRing = count / 3;
            for (int i = 0; i < perRing; i++) {
                if (!ParticleThrottle.canSpawn()) return;
                double angle = 2 * Math.PI * i / perRing;
                double vx = Math.cos(angle) * (0.3 + ri * 0.1);
                double vz = Math.sin(angle) * (0.3 + ri * 0.1);
                w.addParticle(ParticleTypes.FLAME,
                    x + Math.cos(angle) * radii[ri],
                    y + 0.3,
                    z + Math.sin(angle) * radii[ri],
                    vx, 0.05, vz);
            }
        }
        for (int i = 0; i < 16; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z,
                (w.random.nextDouble() - 0.5) * 0.2,
                0.3 + w.random.nextDouble() * 0.4,
                (w.random.nextDouble() - 0.5) * 0.2);
        }
        if (ParticleThrottle.canSpawn())
            w.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 0, 0, 0);
    }

    private static void purple(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y() + 1.0, z = pkt.z();
        double dx = pkt.dirX(), dy = pkt.dirY(), dz = pkt.dirZ();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001) { dx = 1; len = 1; }
        double nx = dx / len, ny = dy / len, nz = dz / len;
        int count = (int)(60 * pkt.intensity());
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double t = i / (double) count * 12.0;
            double spread = 0.3;
            double ox = (w.random.nextDouble() - 0.5) * spread;
            double oz = (w.random.nextDouble() - 0.5) * spread;
            net.minecraft.particle.ParticleEffect type =
                (i % 2 == 0) ? ParticleTypes.WITCH : ParticleTypes.DRAGON_BREATH;
            w.addParticle(type,
                x + nx * t + ox, y + ny * t, z + nz * t + oz,
                nx * 0.3, ny * 0.1, nz * 0.3);
        }
        double ex = x + nx * 10, ey = y + ny * 10, ez = z + nz * 10;
        CommonEffects.spawnBurst(w, ParticleTypes.SOUL, ex, ey, ez, 24);
        CommonEffects.spawnBurst(w, ParticleTypes.PORTAL, ex, ey, ez, 16);
        if (ParticleThrottle.canSpawn())
            w.addParticle(ParticleTypes.EXPLOSION_EMITTER, ex, ey, ez, 0, 0, 0);
    }

    private static void infinityBlock(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y() + 1.0, z = pkt.z();
        int sides = 6;
        for (int i = 0; i < sides; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / sides;
            double cx = x + Math.cos(angle) * 1.2;
            double cz = z + Math.sin(angle) * 1.2;
            w.addParticle(ParticleTypes.END_ROD, cx, y + 1.0, cz, 0, 0.05, 0);
            double nextAngle = 2 * Math.PI * ((i + 1) % sides) / sides;
            double nx2 = x + Math.cos(nextAngle) * 1.2;
            double nz2 = z + Math.sin(nextAngle) * 1.2;
            for (int s = 1; s <= 3; s++) {
                if (!ParticleThrottle.canSpawn()) return;
                double t = s / 4.0;
                w.addParticle(ParticleTypes.ENCHANT,
                    cx + (nx2 - cx) * t, y + 1.0, cz + (nz2 - cz) * t,
                    0, 0.08, 0);
            }
        }
    }

    private static void domain(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // 공간 왜곡 — 검정+흰 소용돌이
        CommonEffects.spawnSphere(w, ParticleTypes.PORTAL, x, y, z, 15.0, 64);
        for (int i = 0; i < 16; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / 16;
            double r = 8 + w.random.nextDouble() * 7;
            w.addParticle(ParticleTypes.END_ROD,
                x + Math.cos(angle) * r, y + w.random.nextDouble() * 10, z + Math.sin(angle) * r,
                0, 0.05, 0);
        }
    }
}
