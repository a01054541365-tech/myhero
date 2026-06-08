package com.jjk.client.effect.impl;

import com.jjk.client.effect.SkillEffectRegistry;
import com.jjk.client.fx.ParticleThrottle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

/** 이누마키 토게 시그니처 이펙트 */
@Environment(EnvType.CLIENT)
public final class InumakiEffects {
    private InumakiEffects() {}

    public static void register() {
        SkillEffectRegistry.register("inumaki_voice_blast", InumakiEffects::voiceBlast);
        SkillEffectRegistry.register("inumaki_stop",        InumakiEffects::stop);
        SkillEffectRegistry.register("inumaki_dont_stop",   InumakiEffects::dontStop);
    }

    private static void voiceBlast(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // 음파 폭발 — CLOUD+EXPLOSION 전방 원뿔
        int count = (int)(32 * pkt.intensity());
        CommonEffects.spawnLine(w, ParticleTypes.CLOUD, x, y, z, pkt.dirX(), pkt.dirY(), pkt.dirZ(), count);
        if (ParticleThrottle.canSpawn()) w.addParticle(ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
    }

    private static void stop(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // !멈춰! — 피격자 주위 END_ROD 원
        for (int i = 0; i < 16; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double angle = 2 * Math.PI * i / 16;
            w.addParticle(ParticleTypes.END_ROD,
                x + Math.cos(angle) * 1.5, y + 1.0, z + Math.sin(angle) * 1.5, 0, 0.05, 0);
        }
    }

    private static void dontStop(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        // !달려! — CRIT 폭발
        int count = (int)(24 * pkt.intensity());
        CommonEffects.spawnBurst(w, ParticleTypes.CRIT, pkt.x(), pkt.y(), pkt.z(), count);
    }
}
