package com.jjk.client.effect.impl;

import com.jjk.client.effect.SkillEffectRegistry;
import com.jjk.client.fx.ParticleThrottle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;

/** 나나미 켄토 시그니처 이펙트 */
@Environment(EnvType.CLIENT)
public final class NanamiEffects {
    private NanamiEffects() {}

    public static void register() {
        SkillEffectRegistry.register("nanami_ratio_line", NanamiEffects::ratioLine);
        SkillEffectRegistry.register("nanami_overtime",   NanamiEffects::overtime);
        SkillEffectRegistry.register("nanami_blade",      NanamiEffects::blade);
    }

    private static void ratioLine(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // 7:3 격자선 — 수직·수평 황색 파티클 2줄
        for (int i = -3; i <= 3; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.CRIT, x + i * 0.5, y + 1.0, z, 0, 0.05, 0); // 수평
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.CRIT, x, y + 0.3 * (i + 3), z, 0, 0.05, 0); // 수직
        }
        CommonEffects.spawnBurst(w, ParticleTypes.ENCHANT, x, y, z, 8);
    }

    private static void overtime(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // 야간 강화 — 검정+황색 폭발
        CommonEffects.spawnBurst(w, ParticleTypes.SOUL, x, y, z, 20);
        CommonEffects.spawnBurst(w, ParticleTypes.ENCHANT, x, y, z, 16);
    }

    private static void blade(com.jjk.network.s2c.SkillEffectS2CPacket pkt, ClientWorld w) {
        double x = pkt.x(), y = pkt.y(), z = pkt.z();
        // 붕대 날 궤적 — 황색 선형 파티클
        int count = (int)(24 * pkt.intensity());
        CommonEffects.spawnLine(w, ParticleTypes.END_ROD, x, y, z,
            pkt.dirX(), pkt.dirY(), pkt.dirZ(), count);
        for (int i = 0; i < 6; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            w.addParticle(ParticleTypes.CRIT, x, y + i * 0.2, z, 0, 0, 0);
        }
    }
}
