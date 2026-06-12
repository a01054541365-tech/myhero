package com.jjk.client.fx;

import com.jjk.client.JjkClientState;
import com.jjk.client.hud.JjkHudRenderer;
import com.jjk.network.s2c.SkillResultS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * SkillResultS2CPacket 수신 시 클라이언트 파티클/이펙트 발생.
 * 패킷 필드: keyId(int), result(String), finalDamage(float)
 */
@Environment(EnvType.CLIENT)
public final class SkillFxDispatcher {
    private SkillFxDispatcher() {}

    private static final DustParticleEffect DUST_BLUE   = new DustParticleEffect(new Vector3f(0.25f, 0.55f, 1.0f), 1.3f);
    private static final DustParticleEffect DUST_RED    = new DustParticleEffect(new Vector3f(1.0f, 0.15f, 0.1f), 1.3f);
    private static final DustParticleEffect DUST_PURPLE = new DustParticleEffect(new Vector3f(0.6f, 0.1f, 0.95f), 1.6f);
    private static final DustParticleEffect DUST_CRIMSON = new DustParticleEffect(new Vector3f(0.7f, 0.0f, 0.1f), 1.2f);
    private static final DustParticleEffect DUST_GOLD   = new DustParticleEffect(new Vector3f(1.0f, 0.85f, 0.2f), 1.1f);

    public static void dispatch(SkillResultS2CPacket packet, MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        AbstractClientPlayerEntity player = client.player;
        JjkHudRenderer.INSTANCE.showDamage(packet.finalDamage());

        Vec3d hitPos = player.getEyePos()
                .add(player.getRotationVec(1.0f).multiply(3.0));

        if (packet.result().contains("BLACK_FLASH")) {
            triggerBlackFlashFx(player, hitPos, client);
        } else if (packet.result().equals("SUCCESS") || packet.result().equals("HIT")) {
            String charId = JjkClientState.getCharacterId();
            dispatchCharacterFx(charId, packet.keyId(), hitPos, client);
        }
    }

    private static void dispatchCharacterFx(String charId, int keyId, Vec3d pos, MinecraftClient mc) {
        if (mc.world == null) return;
        if (charId == null) { spawnGenericHitFx(pos, mc); return; }
        switch (charId) {
            case "gojo" -> {
                if      (keyId == 0) spawnBlueOrb(pos, mc);
                else if (keyId == 1) spawnRedOrb(pos, mc);
                else if (keyId == 2) spawnPurpleOrb(pos, mc);
                else                 spawnGenericHitFx(pos, mc);
            }
            case "sukuna"  -> spawnSlashFx(pos, mc);
            case "itadori" -> spawnFistFx(pos, mc);
            case "jogo"    -> spawnFireFx(pos, mc);
            case "mahito"  -> spawnSoulFx(pos, mc);
            case "nanami"  -> spawnRatioFx(pos, mc);
            default        -> spawnGenericHitFx(pos, mc);
        }
    }

    // ── 공통 도형 헬퍼 ──────────────────────────────────────────────────────

    /** 수평 링 — 바깥으로 퍼지는 충격파. */
    private static void ring(MinecraftClient mc, ParticleEffect type, Vec3d c,
                              double radius, int count, double outSpeed, double ySpeed) {
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double a = 2 * Math.PI * i / count;
            mc.world.addParticle(type,
                c.x + Math.cos(a) * radius, c.y, c.z + Math.sin(a) * radius,
                Math.cos(a) * outSpeed, ySpeed, Math.sin(a) * outSpeed);
        }
    }

    /** 상승 나선 — 2바퀴 감아 올라가는 소용돌이. */
    private static void spiral(MinecraftClient mc, ParticleEffect type, Vec3d c,
                                double radius, double height, int count) {
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double t = i / (double) count;
            double a = t * 4 * Math.PI;
            mc.world.addParticle(type,
                c.x + Math.cos(a) * radius * (1 - t * 0.5),
                c.y + t * height,
                c.z + Math.sin(a) * radius * (1 - t * 0.5),
                0, 0.05, 0);
        }
    }

    /** 구면 수렴/발산 burst. inward=true면 중심으로 빨려 들어감. */
    private static void sphereBurst(MinecraftClient mc, ParticleEffect type, Vec3d c,
                                     double radius, int count, boolean inward) {
        for (int i = 0; i < count; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double theta = Math.random() * 2 * Math.PI;
            double phi   = Math.random() * Math.PI;
            double dx = Math.sin(phi) * Math.cos(theta);
            double dy = Math.cos(phi);
            double dz = Math.sin(phi) * Math.sin(theta);
            double s = inward ? -0.25 : 0.25;
            mc.world.addParticle(type,
                c.x + dx * radius, c.y + dy * radius, c.z + dz * radius,
                dx * s, dy * s, dz * s);
        }
    }

    // ── 고죠 ────────────────────────────────────────────────────────────────

    // Blue — 푸른 입자가 중심으로 수렴하는 인력 구체 + 포탈 소용돌이
    private static void spawnBlueOrb(Vec3d pos, MinecraftClient mc) {
        sphereBurst(mc, DUST_BLUE, pos, 1.8, 36, true);
        spiral(mc, ParticleTypes.PORTAL, pos.add(0, -0.8, 0), 1.2, 1.6, 24);
        if (ParticleThrottle.canSpawn())
            mc.world.addParticle(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 0, 0, 0);
    }

    // Red — 붉은 충격파가 방사형으로 터져나가는 척력
    private static void spawnRedOrb(Vec3d pos, MinecraftClient mc) {
        sphereBurst(mc, DUST_RED, pos, 0.4, 40, false);
        ring(mc, ParticleTypes.FLAME, pos, 0.8, 24, 0.5, 0.02);
        ring(mc, ParticleTypes.LAVA, pos, 0.5, 8, 0.3, 0.1);
        if (ParticleThrottle.canSpawn())
            mc.world.addParticle(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 0, 0, 0);
    }

    // Purple — 보라 섬광 구체 + 관통 잔광 링 2단
    private static void spawnPurpleOrb(Vec3d pos, MinecraftClient mc) {
        sphereBurst(mc, DUST_PURPLE, pos, 1.0, 48, false);
        ring(mc, ParticleTypes.WITCH, pos, 1.4, 28, 0.4, 0.0);
        ring(mc, ParticleTypes.END_ROD, pos.add(0, 0.6, 0), 0.9, 16, 0.25, 0.05);
        if (ParticleThrottle.canSpawn())
            mc.world.addParticle(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 0, 0, 0);
        if (ParticleThrottle.canSpawn())
            mc.world.addParticle(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 0, 0, 0);
    }

    // ── 이타도리 — 주먹 충격파: 크리트 원뿔 + 지면 파열 링 ──────────────────
    private static void spawnFistFx(Vec3d pos, MinecraftClient mc) {
        for (int i = 0; i < 18; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            mc.world.addParticle(ParticleTypes.CRIT,
                pos.x, pos.y, pos.z,
                (Math.random() - 0.5) * 0.8, Math.random() * 0.4, (Math.random() - 0.5) * 0.8);
        }
        ring(mc, ParticleTypes.CLOUD, pos.add(0, -1.0, 0), 0.6, 12, 0.35, 0.0);
    }

    // ── 죠고 — 화염 기둥 + 용암 비산 + 잔불 링 ──────────────────────────────
    private static void spawnFireFx(Vec3d pos, MinecraftClient mc) {
        for (int i = 0; i < 20; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            mc.world.addParticle(ParticleTypes.FLAME,
                pos.x + (Math.random() - 0.5) * 0.4,
                pos.y + Math.random() * 1.5,
                pos.z + (Math.random() - 0.5) * 0.4,
                0, 0.25 + Math.random() * 0.2, 0);
        }
        ring(mc, ParticleTypes.SMALL_FLAME, pos.add(0, -0.5, 0), 1.0, 20, 0.3, 0.05);
        for (int i = 0; i < 6; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            mc.world.addParticle(ParticleTypes.LAVA, pos.x, pos.y + 0.5, pos.z,
                (Math.random() - 0.5) * 0.4, 0.3, (Math.random() - 0.5) * 0.4);
        }
    }

    private static void spawnGenericHitFx(Vec3d pos, MinecraftClient mc) {
        for (int i = 0; i < 6; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            mc.world.addParticle(ParticleTypes.CRIT, pos.x, pos.y, pos.z,
                (Math.random() - 0.5) * 0.4, 0.15, (Math.random() - 0.5) * 0.4);
        }
    }

    // ── 스쿠나 — 교차 참격: 십자 베기 궤적 + 진홍 잔광 ──────────────────────
    private static void spawnSlashFx(Vec3d pos, MinecraftClient mc) {
        if (mc.world == null) return;
        // 대각 2방향 베기 라인 (X자)
        for (int d = 0; d < 2; d++) {
            double a = (d == 0 ? 1 : -1) * Math.PI / 4;
            for (int i = -6; i <= 6; i++) {
                if (!ParticleThrottle.canSpawn()) return;
                double t = i * 0.22;
                mc.world.addParticle(DUST_CRIMSON,
                    pos.x + Math.cos(a) * t, pos.y + 1.0 + t * 0.5 * (d == 0 ? 1 : -1),
                    pos.z + Math.sin(a) * t, 0, 0, 0);
            }
        }
        ring(mc, ParticleTypes.SWEEP_ATTACK, pos.add(0, 1.0, 0), 0.4, 8, 0.3, 0.05);
        if (ParticleThrottle.canSpawn())
            mc.world.addParticle(ParticleTypes.CRIT, pos.x, pos.y + 1.0, pos.z, 0, 0.2, 0);
    }

    // ── 마히토 — 영혼 소용돌이: 이중 나선 상승 ──────────────────────────────
    private static void spawnSoulFx(Vec3d pos, MinecraftClient mc) {
        if (mc.world == null) return;
        spiral(mc, ParticleTypes.SOUL, pos, 1.0, 2.0, 28);
        spiral(mc, ParticleTypes.SCULK_SOUL, pos.add(0, 0.3, 0), 0.7, 1.8, 20);
        if (ParticleThrottle.canSpawn())
            mc.world.addParticle(ParticleTypes.WITCH, pos.x, pos.y + 1.5, pos.z, 0, 0.1, 0);
    }

    // ── 나나미 — 7:3 비율 절단선: 금색 십자 + 절단점 섬광 ───────────────────
    private static void spawnRatioFx(Vec3d pos, MinecraftClient mc) {
        if (mc.world == null) return;
        for (int i = 0; i < 14; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            double t = i / 14.0 * 4.0 - 2.0;
            mc.world.addParticle(DUST_GOLD, pos.x + t, pos.y + 1.0, pos.z, 0, 0, 0);
            mc.world.addParticle(DUST_GOLD, pos.x, pos.y + 1.0, pos.z + t, 0, 0, 0);
        }
        // 7:3 지점(중심에서 0.4 오프셋) 절단 섬광
        Vec3d cut = pos.add(0.4, 1.0, 0);
        for (int i = 0; i < 8; i++) {
            if (!ParticleThrottle.canSpawn()) return;
            mc.world.addParticle(ParticleTypes.END_ROD, cut.x, cut.y, cut.z,
                (Math.random() - 0.5) * 0.3, Math.random() * 0.2, (Math.random() - 0.5) * 0.3);
        }
        if (ParticleThrottle.canSpawn())
            mc.world.addParticle(ParticleTypes.CRIT, pos.x, pos.y + 1.0, pos.z, 0, 0.2, 0);
    }

    // ── 흑섬 — 번개 다발 + 충격파 링 2단 + 검붉은 구체 잔광 ─────────────────
    private static void triggerBlackFlashFx(AbstractClientPlayerEntity player,
                                              Vec3d hitPos, MinecraftClient mc) {
        if (mc.world == null) return;
        com.jjk.client.effect.impl.CommonEffects.spawnBlackRedLightning(
            mc.world, hitPos.x, hitPos.y + 1.0, hitPos.z, 12);
        sphereBurst(mc, DUST_CRIMSON, hitPos.add(0, 1.0, 0), 0.6, 30, false);
        ring(mc, ParticleTypes.SMOKE, hitPos, 0.8, 20, 0.45, 0.0);
        ring(mc, ParticleTypes.SOUL_FIRE_FLAME, hitPos.add(0, 0.4, 0), 1.2, 16, 0.3, 0.05);
        for (int i = 0; i < 20; i++) {
            if (!ParticleThrottle.canSpawn()) break;
            mc.world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                hitPos.x + (Math.random() - 0.5) * 0.5,
                hitPos.y + 0.5,
                hitPos.z + (Math.random() - 0.5) * 0.5,
                0, 0.3 + Math.random() * 0.2, 0);
        }
        if (ParticleThrottle.canSpawn())
            mc.world.addParticle(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1.0, player.getZ(), 0, 0, 0);
        if (ParticleThrottle.canSpawn())
            mc.world.addParticle(ParticleTypes.EXPLOSION_EMITTER,
                hitPos.x, hitPos.y, hitPos.z, 0, 0, 0);
        if (ParticleThrottle.canSpawn())
            mc.world.addParticle(ParticleTypes.SONIC_BOOM,
                hitPos.x, hitPos.y + 1.0, hitPos.z, 0, 0, 0);
    }
}
