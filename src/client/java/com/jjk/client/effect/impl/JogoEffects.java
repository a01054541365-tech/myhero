package com.jjk.client.effect.impl;

import com.jjk.client.effect.SkillEffectRegistry;
import com.jjk.client.fx.ParticleThrottle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

/** 죠고 시그니처 이펙트 */
@Environment(EnvType.CLIENT)
public final class JogoEffects {
    private JogoEffects() {}

    public static void register() {
        SkillEffectRegistry.register("jogo_ember_insects",   JogoEffects::emberInsects);
        SkillEffectRegistry.register("jogo_maximum_meteor",  JogoEffects::maximumMeteor);
        SkillEffectRegistry.register("jogo_domain",          JogoEffects::domain);
    }

    private static void emberInsects(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // 불씨 벌레 8개 — 나선형 이동 파티클 군집
        int clusters = 8;
        for (int c = 0; c < clusters; c++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * c / clusters;
            double cx = x + Math.cos(angle) * 2.0;
            double cz = z + Math.sin(angle) * 2.0;
            for (int j = 0; j < 4; j++) {
                if (!ParticleThrottle.canSpawn()) return;
                w.addParticle(ParticleTypes.FLAME, cx, y + j * 0.3, cz,
                    Math.cos(angle) * 0.1, 0.05 + j * 0.02, Math.sin(angle) * 0.1);
            }
        }
    }

    private static void maximumMeteor(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        for (int i = 0; i < 30; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double ox = (w.random.nextDouble() - 0.5) * 1.5;
            double oz = (w.random.nextDouble() - 0.5) * 1.5;
            w.addParticle(ParticleTypes.FLAME,
                x + ox, y + 8.0 + w.random.nextDouble() * 4.0, z + oz,
                ox * 0.05, -0.5, oz * 0.05);
        }
        for (int i = 0; i < 20; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / 20;
            double r = 1.0 + w.random.nextDouble() * 2.0;
            w.addParticle(ParticleTypes.LAVA,
                x + Math.cos(angle) * r, y + 0.2, z + Math.sin(angle) * r,
                Math.cos(angle) * 0.2, 0.3, Math.sin(angle) * 0.2);
        }
        for (int i = 0; i < 16; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                x + (w.random.nextDouble() - 0.5) * 3.0,
                y + 0.1,
                z + (w.random.nextDouble() - 0.5) * 3.0,
                0, 0.15 + w.random.nextDouble() * 0.1, 0);
        }
        if (ParticleThrottle.canSpawn())
            w.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 0, 0, 0);
    }

    private static void domain(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // 화산 기둥 4개 + 지면 용암 흐름
        for (int col = 0; col < 4; col++) {
            double angle = Math.PI / 2 * col;
            double px = x + Math.cos(angle) * 8;
            double pz = z + Math.sin(angle) * 8;
            for (int h = 0; h < 8; h++) {
                if (!ParticleThrottle.canSpawn()) return;
                w.addParticle(ParticleTypes.FLAME, px, y + h, pz, 0, 0.1, 0);
                if (!ParticleThrottle.canSpawn()) return;
                w.addParticle(ParticleTypes.SMOKE, px, y + h, pz, 0, 0.05, 0);
            }
        }
        // 지면 용암 흐름
        CommonEffects.spawnSphere(w, ParticleTypes.LAVA, x, y, z, 10.0, 16);
    }
}
