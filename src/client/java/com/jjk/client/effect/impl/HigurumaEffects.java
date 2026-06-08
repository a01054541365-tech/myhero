package com.jjk.client.effect.impl;

import com.jjk.client.effect.SkillEffectRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

/** 히구루마 히로미 시그니처 이펙트 */
@Environment(EnvType.CLIENT)
public final class HigurumaEffects {
    private HigurumaEffects() {}

    public static void register() {
        SkillEffectRegistry.register("higuruma_verdict", HigurumaEffects::verdict);
        SkillEffectRegistry.register("higuruma_domain",  HigurumaEffects::domain);
    }

    private static void verdict(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        // 심판 집행 — ENCHANT 폭발 + CRIT
        int count = (int)(24 * pkt.intensity());
        CommonEffects.spawnBurst(w, ParticleTypes.ENCHANT, pkt.x(), pkt.y(), pkt.z(), count);
        CommonEffects.spawnBurst(w, ParticleTypes.CRIT, pkt.x(), pkt.y(), pkt.z(), 8);
    }

    private static void domain(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        // 심판의 영역 — PORTAL+ENCHANT 구체
        CommonEffects.spawnSphere(w, ParticleTypes.PORTAL, pkt.x(), pkt.y(), pkt.z(), 12.0, 48);
        CommonEffects.spawnSphere(w, ParticleTypes.ENCHANT, pkt.x(), pkt.y(), pkt.z(), 14.0, 32);
    }
}
