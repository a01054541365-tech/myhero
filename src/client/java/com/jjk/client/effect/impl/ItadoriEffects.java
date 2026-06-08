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
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // 충격파 부채꼴 — 방향 기반 6블록
        CommonEffects.spawnLine(w, ParticleTypes.CRIT, x, y, z, pkt.dirX(), pkt.dirY(), pkt.dirZ(), 16);
        // 주황 폭발 파티클
        int count = (int)(24 * pkt.intensity());
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = (w.random.nextDouble() - 0.5) * Math.PI;
            double range = w.random.nextDouble() * 3.0;
            w.addParticle(ParticleTypes.SWEEP_ATTACK,
                x + Math.cos(angle) * range, y + 0.5, z + Math.sin(angle) * range,
                Math.cos(angle) * 0.2, 0, Math.sin(angle) * 0.2);
        }
    }

    private static void blackFlash(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        int count = (int)(64 * pkt.intensity());
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.SOUL, x, y, z,
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
        CommonEffects.spawnBurst(w, ParticleTypes.SOUL, pkt.x(), pkt.y(), pkt.z(), 24);
        CommonEffects.spawnBurst(w, ParticleTypes.CRIT, pkt.x(), pkt.y(), pkt.z(), 16);
    }
}
