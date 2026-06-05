package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * 서버 → 클라이언트: 스킬 이펙트 트리거.
 * effectType: SkillEffectType.name() 직렬화
 * targetUuid: 이펙트 발생 위치 주체
 * x/y/z:     이펙트 좌표
 */
public record SkillEffectS2CPacket(String effectType, String targetUuid, double x, double y, double z)
        implements CustomPayload {

    public static final CustomPayload.Id<SkillEffectS2CPacket> ID =
        new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "skill_effect"));

    public static final PacketCodec<RegistryByteBuf, SkillEffectS2CPacket> CODEC =
        PacketCodec.tuple(
            PacketCodecs.STRING, SkillEffectS2CPacket::effectType,
            PacketCodecs.STRING, SkillEffectS2CPacket::targetUuid,
            PacketCodecs.DOUBLE, SkillEffectS2CPacket::x,
            PacketCodecs.DOUBLE, SkillEffectS2CPacket::y,
            PacketCodecs.DOUBLE, SkillEffectS2CPacket::z,
            SkillEffectS2CPacket::new
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    /** 서버 편의 팩토리: UUID + 위치 기반. */
    public static SkillEffectS2CPacket of(String effectType, UUID target, double x, double y, double z) {
        return new SkillEffectS2CPacket(effectType, target.toString(), x, y, z);
    }
}
