package com.jjk.client.effect.impl;

import com.jjk.client.effect.SkillEffectRegistry;
import com.jjk.client.fx.ParticleThrottle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

/** 하카리 킨토키 시그니처 이펙트 */
@Environment(EnvType.CLIENT)
public final class HakariEffects {
    private HakariEffects() {}

    public static void register() {
        SkillEffectRegistry.register("hakari_barrier",        HakariEffects::barrier);
        SkillEffectRegistry.register("hakari_jackpot_start",  HakariEffects::jackpotStart);
        SkillEffectRegistry.register("hakari_jackpot_regen",  HakariEffects::jackpotRegen);
    }

    private static void barrier(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // 주력방출 배리어 — ENCHANT 다각형
        for (int i = 0; i < 20; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / 20;
            w.addParticle(ParticleTypes.ENCHANT,
                x + Math.cos(angle) * 5.0, y + 1.0, z + Math.sin(angle) * 5.0, 0, 0.05, 0);
        }
    }

    private static void jackpotStart(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        // 잭팟 발동 — TOTEM_OF_UNDYING 화려한 폭발
        int count = (int)(64 * pkt.intensity());
        CommonEffects.spawnBurst(w, ParticleTypes.TOTEM_OF_UNDYING, pkt.x(), pkt.y(), pkt.z(), count);
    }

    private static void jackpotRegen(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        // 잭팟 CE 재생 — 초록 HEART 파티클
        int count = (int)(16 * pkt.intensity());
        CommonEffects.spawnBurst(w, ParticleTypes.HEART, pkt.x(), pkt.y() + 1.5, pkt.z(), count);
    }
}
