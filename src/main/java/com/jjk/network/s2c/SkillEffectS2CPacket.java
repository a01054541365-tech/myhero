package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * 서버 → 클라이언트: 스킬 이펙트 트리거 (순수 연출 전용).
 * SkillResultS2CPacket과 별개 — 피해 수치 없음.
 *
 * effectType       — 이펙트 식별자 (SkillEffectRegistry에 등록된 키)
 * targetUuid       — 시전자 UUID (연출 주체)
 * x/y/z            — 이펙트 발생 위치
 * dirX/Y/Z         — 방향 벡터 (투사체·방향성 이펙트용, 없으면 0)
 * attackerEntityId — 시전자 엔티티 ID (-1 = 불명)
 * targetEntityId   — 피격자 엔티티 ID (-1 = 없음)
 * intensity        — 이펙트 강도 0.0~1.0 (숙련도·파티클 수 스케일)
 */
public record SkillEffectS2CPacket(
    String effectType,
    String targetUuid,
    double x, double y, double z,
    double dirX, double dirY, double dirZ,
    int attackerEntityId,
    int targetEntityId,
    float intensity
) implements CustomPayload {

    public static final CustomPayload.Id<SkillEffectS2CPacket> ID =
        new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "skill_effect"));

    public static final PacketCodec<RegistryByteBuf, SkillEffectS2CPacket> CODEC = PacketCodec.of(
        (pkt, buf) -> {
            buf.writeString(pkt.effectType());
            buf.writeString(pkt.targetUuid());
            buf.writeDouble(pkt.x());
            buf.writeDouble(pkt.y());
            buf.writeDouble(pkt.z());
            buf.writeDouble(pkt.dirX());
            buf.writeDouble(pkt.dirY());
            buf.writeDouble(pkt.dirZ());
            buf.writeInt(pkt.attackerEntityId());
            buf.writeInt(pkt.targetEntityId());
            buf.writeFloat(pkt.intensity());
        },
        buf -> new SkillEffectS2CPacket(
            buf.readString(), buf.readString(),
            buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readDouble(), buf.readDouble(), buf.readDouble(),
            buf.readInt(), buf.readInt(),
            buf.readFloat()
        )
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    /** 기존 호환 팩토리 — 방향/엔티티ID 없는 단순 위치 이펙트. */
    public static SkillEffectS2CPacket of(String effectType, UUID target, double x, double y, double z) {
        return new SkillEffectS2CPacket(
            effectType, target.toString(), x, y, z, 0.0, 0.0, 0.0, -1, -1, 1.0f);
    }

    /** 풀 팩토리 — 방향 벡터 + 엔티티 ID + intensity 포함. */
    public static SkillEffectS2CPacket of(String effectType, UUID target,
                                           double x, double y, double z,
                                           double dirX, double dirY, double dirZ,
                                           int attackerId, int targetId, float intensity) {
        return new SkillEffectS2CPacket(
            effectType, target.toString(), x, y, z, dirX, dirY, dirZ,
            attackerId, targetId, intensity);
    }
}
