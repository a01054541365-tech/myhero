package com.jjk.client.effect.impl;

import com.jjk.client.effect.SkillEffectRegistry;
import com.jjk.client.fx.ParticleThrottle;
import com.jjk.client.hud.BlackFlashOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

/** 이타도리 유지 시그니처 이펙트 */
@Environment(EnvType.CLIENT)
public final class ItadoriEffects {
    private ItadoriEffects() {}

    public static void register() {
        SkillEffectRegistry.register("itadori_divergent_fist",  ItadoriEffects::divergentFist);
        SkillEffectRegistry.register("itadori_black_flash",     ItadoriEffects::blackFlash);
        SkillEffectRegistry.register("itadori_divergent_combo", ItadoriEffects::divergentCombo);
        SkillEffectRegistry.register("itadori_shrine",          ItadoriEffects::shrine);
    }

    private static void divergentFist(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y() + 1.0, z = pkt.z();
        for (int i = 0; i < 12; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / 12;
            w.addParticle(ParticleTypes.CRIT,
                x, y, z,
                Math.cos(angle) * 0.25, 0.15, Math.sin(angle) * 0.25);
        }
        for (int i = 0; i < 20; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.SOUL,
                x + (w.random.nextDouble() - 0.5) * 0.4,
                y + 0.3,
                z + (w.random.nextDouble() - 0.5) * 0.4,
                (w.random.nextDouble() - 0.5) * 0.3,
                0.2 + w.random.nextDouble() * 0.2,
                (w.random.nextDouble() - 0.5) * 0.3);
        }
        if (ParticleThrottle.canSpawn())
            w.addParticle(ParticleTypes.SWEEP_ATTACK, x, y, z, 0, 0, 0);
    }

    private static void blackFlash(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        CommonEffects.spawnBlackRedLightning(w, x, y + 1.0, z, (int)(6 * pkt.intensity()) + 2);
        int count = (int)(24 * pkt.intensity());
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.SMOKE, x, y, z,
                (w.random.nextDouble() - 0.5) * 0.6, w.random.nextDouble() * 0.5,
                (w.random.nextDouble() - 0.5) * 0.6);
        }
        if (ParticleThrottle.canSpawn()) w.addParticle(ParticleTypes.FLASH, x, y + 1, z, 0, 0, 0);
        BlackFlashOverlay.INSTANCE.triggerHitFlash();
    }

    private static void divergentCombo(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // 잔상 5개 — SWEEP_ATTACK 0.2초 간격 (틱 기반 오프셋 표현: 연속 스폰)
        for (int trail = 0; trail < 5; trail++) {
            double ox = x - pkt.dirX() * trail * 0.4;
            double oz = z - pkt.dirZ() * trail * 0.4;
            for (int j = 0; j < 6; j++) {
                if (!ParticleThrottle.canSpawn()) return;
                w.addParticle(ParticleTypes.SWEEP_ATTACK,
                    ox + (w.random.nextDouble() - 0.5) * 0.3,
                    y + j * 0.25,
                    oz + (w.random.nextDouble() - 0.5) * 0.3,
                    0, 0.05, 0);
            }
        }
    }

    private static void shrine(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y() + 1.0, z = pkt.z();
        int count = (int)(32 * pkt.intensity());
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / count;
            double r = 0.3 + w.random.nextDouble() * 0.5;
            w.addParticle(ParticleTypes.SOUL,
                x + Math.cos(angle) * r,
                y - 0.5 + (i / (double) count) * 3.0,
                z + Math.sin(angle) * r,
                0, 0.15, 0);
        }
        for (int i = 0; i < 16; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.CRIT, x, y, z,
                (w.random.nextDouble() - 0.5) * 0.5,
                w.random.nextDouble() * 0.3,
                (w.random.nextDouble() - 0.5) * 0.5);
        }
        if (ParticleThrottle.canSpawn())
            w.addParticle(ParticleTypes.FLASH, x, y + 1.0, z, 0, 0, 0);
    }
}
