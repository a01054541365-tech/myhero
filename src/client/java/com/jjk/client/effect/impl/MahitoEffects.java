package com.jjk.client.effect.impl;

import com.jjk.client.effect.SkillEffectRegistry;
import com.jjk.client.fx.ParticleThrottle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

/** 마히토 시그니처 이펙트 */
@Environment(EnvType.CLIENT)
public final class MahitoEffects {
    private MahitoEffects() {}

    public static void register() {
        SkillEffectRegistry.register("mahito_idle_transfigure", MahitoEffects::idleTransfigure);
        SkillEffectRegistry.register("mahito_soul_punch",       MahitoEffects::soulPunch);
        SkillEffectRegistry.register("mahito_domain",           MahitoEffects::domain);
    }

    private static void idleTransfigure(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y() + 1.0, z = pkt.z();
        for (int i = 0; i < 24; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / 24 + i * 0.3;
            double r = 0.8 + Math.sin(i * 0.5) * 0.4;
            double h = i / 24.0 * 2.5;
            w.addParticle(ParticleTypes.WITCH,
                x + Math.cos(angle) * r, y + h, z + Math.sin(angle) * r,
                -Math.sin(angle) * 0.05, 0.04, Math.cos(angle) * 0.05);
        }
        for (int i = 0; i < 12; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.PORTAL,
                x + (w.random.nextDouble() - 0.5) * 0.8,
                y + w.random.nextDouble() * 2.0,
                z + (w.random.nextDouble() - 0.5) * 0.8,
                0, 0.1, 0);
        }
    }

    private static void soulPunch(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y() + 1.0, z = pkt.z();
        int count = (int)(40 * pkt.intensity());
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / count;
            double speed = 0.25 + w.random.nextDouble() * 0.15;
            w.addParticle(ParticleTypes.SOUL, x, y, z,
                Math.cos(angle) * speed, 0.1, Math.sin(angle) * speed);
        }
        for (int i = 0; i < 12; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.CRIT, x, y, z,
                (w.random.nextDouble() - 0.5) * 0.4,
                w.random.nextDouble() * 0.3,
                (w.random.nextDouble() - 0.5) * 0.4);
        }
        for (int i = 0; i < 8; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.WITCH,
                x + (w.random.nextDouble() - 0.5) * 1.0,
                y + w.random.nextDouble() * 1.5,
                z + (w.random.nextDouble() - 0.5) * 1.0,
                0, 0.05, 0);
        }
    }

    private static void domain(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        // 자폐원돈과 영역 — PORTAL+WITCH 소용돌이
        CommonEffects.spawnSphere(w, ParticleTypes.PORTAL, pkt.x(), pkt.y(), pkt.z(), 10.0, 48);
        CommonEffects.spawnSphere(w, ParticleTypes.WITCH, pkt.x(), pkt.y(), pkt.z(), 12.0, 32);
    }
}
