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
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        int count = (int)(32 * pkt.intensity());
        // 구형 파티클 배치
        CommonEffects.spawnSphere(w, ParticleTypes.DRAGON_BREATH, x, y, z, 2.5, count);
        // 회전 링 (수평 원)
        int ring = 16;
        for (int i = 0; i < ring; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / ring;
            w.addParticle(ParticleTypes.PORTAL,
                x + Math.cos(angle) * 3.0, y + 0.5, z + Math.sin(angle) * 3.0,
                -Math.sin(angle) * 0.2, 0.05, Math.cos(angle) * 0.2);
        }
    }

    private static void red(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        int count = (int)(32 * pkt.intensity());
        CommonEffects.spawnSphere(w, ParticleTypes.FLAME, x, y, z, 2.5, count);
        for (int i = 0; i < 12; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / 12;
            w.addParticle(ParticleTypes.FLAME,
                x + Math.cos(angle) * 3.0, y, z + Math.sin(angle) * 3.0,
                Math.cos(angle) * 0.4, 0.1, Math.sin(angle) * 0.4);
        }
    }

    private static void purple(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        int count = (int)(48 * pkt.intensity());
        // blue + red 혼합 색상 → WITCH(보라) + DRAGON_BREATH
        CommonEffects.spawnLine(w, ParticleTypes.WITCH, x, y, z, pkt.dirX(), pkt.dirY(), pkt.dirZ(), count / 2);
        CommonEffects.spawnLine(w, ParticleTypes.DRAGON_BREATH, x, y, z, pkt.dirX(), pkt.dirY(), pkt.dirZ(), count / 2);
        CommonEffects.spawnBurst(w, ParticleTypes.SOUL, x, y, z, 16);
    }

    private static void infinityBlock(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // 육각형 배치 파티클 플래시
        int sides = 6;
        for (int i = 0; i < sides; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / sides;
            for (int r = 1; r <= 2; r++) {
                if (!ParticleThrottle.canSpawn()) return;
                w.addParticle(ParticleTypes.ENCHANT,
                    x + Math.cos(angle) * r, y + 1.0, z + Math.sin(angle) * r, 0, 0.1, 0);
            }
        }
        if (ParticleThrottle.canSpawn()) w.addParticle(ParticleTypes.END_ROD, x, y + 1, z, 0, 0.3, 0);
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
