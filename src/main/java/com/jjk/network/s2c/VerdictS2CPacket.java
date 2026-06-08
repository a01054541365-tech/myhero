package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** 재판 판결 결과 통지. guilty=false면 sealedSkillId는 빈 문자열. */
public record VerdictS2CPacket(UUID attackerUuid, UUID targetUuid, boolean guilty, String sealedSkillId)
        implements CustomPayload {

    public static final CustomPayload.Id<VerdictS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "verdict"));

    public static final PacketCodec<RegistryByteBuf, VerdictS2CPacket> CODEC = PacketCodec.of(
            (pkt, buf) -> {
                buf.writeString(pkt.attackerUuid().toString());
                buf.writeString(pkt.targetUuid().toString());
                buf.writeBoolean(pkt.guilty());
                buf.writeString(pkt.sealedSkillId());
            },
            buf -> new VerdictS2CPacket(
                    UUID.fromString(buf.readString()),
                    UUID.fromString(buf.readString()),
                    buf.readBoolean(),
                    buf.readString()
            )
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
