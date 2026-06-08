package com.jjk.client.effect.impl;

import com.jjk.client.effect.SkillEffectRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

/** 옷코츠 유타 시그니처 이펙트 */
@Environment(EnvType.CLIENT)
public final class OkkotsuEffects {
    private OkkotsuEffects() {}

    public static void register() {
        SkillEffectRegistry.register("okkotsu_rika_summon", OkkotsuEffects::rikaSummon);
        SkillEffectRegistry.register("okkotsu_rika_attack", OkkotsuEffects::rikaAttack);
        SkillEffectRegistry.register("okkotsu_domain",      OkkotsuEffects::domain);
    }

    private static void rikaSummon(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        // 리카 소환 — TOTEM_OF_UNDYING 화려한 폭발
        int count = (int)(48 * pkt.intensity());
        CommonEffects.spawnBurst(w, ParticleTypes.TOTEM_OF_UNDYING, pkt.x(), pkt.y(), pkt.z(), count);
    }

    private static void rikaAttack(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        // 리카 공격 — SOUL 폭발
        int count = (int)(32 * pkt.intensity());
        CommonEffects.spawnBurst(w, ParticleTypes.SOUL, pkt.x(), pkt.y(), pkt.z(), count);
        CommonEffects.spawnBurst(w, ParticleTypes.CRIT, pkt.x(), pkt.y(), pkt.z(), 8);
    }

    private static void domain(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        // 옷코츠 영역 — END_ROD+PORTAL 혼합 구체
        CommonEffects.spawnSphere(w, ParticleTypes.END_ROD, pkt.x(), pkt.y(), pkt.z(), 15.0, 48);
        CommonEffects.spawnSphere(w, ParticleTypes.PORTAL, pkt.x(), pkt.y(), pkt.z(), 12.0, 32);
    }
}
