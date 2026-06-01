package com.jjk.client.fx;

import com.jjk.client.JjkClientState;
import com.jjk.client.hud.JjkHudRenderer;
import com.jjk.network.s2c.SkillResultS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

/**
 * SkillResultS2CPacket 수신 시 클라이언트 파티클/이펙트 발생.
 * 패킷 필드: keyId(int), result(String), finalDamage(float)
 */
@Environment(EnvType.CLIENT)
public final class SkillFxDispatcher {
    private SkillFxDispatcher() {}

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
        if (charId == null) { spawnGenericHitFx(pos, mc); return; }
        switch (charId) {
            case "gojo" -> {
                if (keyId == 0) spawnBlueOrb(pos, mc);
                else if (keyId == 1) spawnRedOrb(pos, mc);
                else if (keyId == 2) spawnPurpleOrb(pos, mc);
                else spawnGenericHitFx(pos, mc);
            }
            case "itadori" -> spawnFistFx(pos, mc);
            case "jogo" -> spawnFireFx(pos, mc);
            default -> spawnGenericHitFx(pos, mc);
        }
    }

    // 고죠 Blue — Portal 파티클
    private static void spawnBlueOrb(Vec3d pos, MinecraftClient mc) {
        for (int i = 0; i < 20; i++) {
            mc.world.addParticle(ParticleTypes.PORTAL,
                pos.x, pos.y, pos.z,
                (Math.random() - 0.5) * 0.5, Math.random() * 0.3, (Math.random() - 0.5) * 0.5);
        }
    }

    // 고죠 Red — Flame 파티클
    private static void spawnRedOrb(Vec3d pos, MinecraftClient mc) {
        for (int i = 0; i < 15; i++) {
            mc.world.addParticle(ParticleTypes.FLAME,
                pos.x, pos.y, pos.z,
                (Math.random() - 0.5) * 0.3, Math.random() * 0.2, (Math.random() - 0.5) * 0.3);
        }
    }

    // 고죠 Purple — Witch + Soul 혼합
    private static void spawnPurpleOrb(Vec3d pos, MinecraftClient mc) {
        for (int i = 0; i < 25; i++) {
            mc.world.addParticle(ParticleTypes.WITCH,
                pos.x, pos.y, pos.z,
                (Math.random() - 0.5) * 0.5, Math.random() * 0.3, (Math.random() - 0.5) * 0.5);
        }
        for (int i = 0; i < 10; i++) {
            mc.world.addParticle(ParticleTypes.SOUL,
                pos.x, pos.y, pos.z,
                (Math.random() - 0.5) * 0.3, Math.random() * 0.2, (Math.random() - 0.5) * 0.3);
        }
    }

    // 이타도리 — Crit 파티클
    private static void spawnFistFx(Vec3d pos, MinecraftClient mc) {
        for (int i = 0; i < 10; i++) {
            mc.world.addParticle(ParticleTypes.CRIT,
                pos.x, pos.y, pos.z,
                (Math.random() - 0.5) * 0.3, Math.random() * 0.2, (Math.random() - 0.5) * 0.3);
        }
    }

    // 죠고 — Flame
    private static void spawnFireFx(Vec3d pos, MinecraftClient mc) {
        for (int i = 0; i < 20; i++) {
            mc.world.addParticle(ParticleTypes.FLAME,
                pos.x, pos.y, pos.z,
                (Math.random() - 0.5) * 0.4, Math.random() * 0.4, (Math.random() - 0.5) * 0.4);
        }
    }

    private static void spawnGenericHitFx(Vec3d pos, MinecraftClient mc) {
        mc.world.addParticle(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 0, 0.1, 0);
    }

    // 흑섬: Soul + Flash 파티클
    private static void triggerBlackFlashFx(AbstractClientPlayerEntity player, Vec3d hitPos, MinecraftClient mc) {
        for (int i = 0; i < 30; i++) {
            mc.world.addParticle(ParticleTypes.SOUL,
                hitPos.x, hitPos.y, hitPos.z,
                (Math.random() - 0.5) * 0.3, 0.1, (Math.random() - 0.5) * 0.3);
        }
        mc.world.addParticle(ParticleTypes.FLASH,
            player.getX(), player.getY() + 1.0, player.getZ(), 0, 0, 0);
    }
}
