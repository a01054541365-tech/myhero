package com.jjk.client.effect.impl;

import com.jjk.client.effect.SkillEffectRegistry;
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
        // 자폐원돈과 — WITCH+PORTAL 혼합
        CommonEffects.spawnBurst(w, ParticleTypes.WITCH, pkt.x(), pkt.y(), pkt.z(), 24);
        CommonEffects.spawnBurst(w, ParticleTypes.PORTAL, pkt.x(), pkt.y(), pkt.z(), 16);
    }

    private static void soulPunch(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        // 영혼 직접 가격 — SOUL 폭발 + CRIT
        int count = (int)(32 * pkt.intensity());
        CommonEffects.spawnBurst(w, ParticleTypes.SOUL, pkt.x(), pkt.y(), pkt.z(), count);
        CommonEffects.spawnBurst(w, ParticleTypes.CRIT, pkt.x(), pkt.y(), pkt.z(), 8);
    }

    private static void domain(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        // 자폐원돈과 영역 — PORTAL+WITCH 소용돌이
        CommonEffects.spawnSphere(w, ParticleTypes.PORTAL, pkt.x(), pkt.y(), pkt.z(), 10.0, 48);
        CommonEffects.spawnSphere(w, ParticleTypes.WITCH, pkt.x(), pkt.y(), pkt.z(), 12.0, 32);
    }
}
