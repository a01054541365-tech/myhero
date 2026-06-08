package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/** 단일-스킬 봉인 동기화. sealedSkillId가 빈 문자열이면 봉인 해제. */
public record SealedSkillSyncS2CPacket(UUID targetUuid, String sealedSkillId, long expireAtTick)
        implements CustomPayload {

    public static final CustomPayload.Id<SealedSkillSyncS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "sealed_skill_sync"));

    public static final PacketCodec<RegistryByteBuf, SealedSkillSyncS2CPacket> CODEC = PacketCodec.of(
            (pkt, buf) -> {
                buf.writeString(pkt.targetUuid().toString());
                buf.writeString(pkt.sealedSkillId());
                buf.writeLong(pkt.expireAtTick());
            },
            buf -> new SealedSkillSyncS2CPacket(
                    UUID.fromString(buf.readString()),
                    buf.readString(),
                    buf.readLong()
            )
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
