package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record ChantingStateS2CPacket(UUID playerUuid, boolean chanting, int chantTicks)
        implements CustomPayload {

    public static final CustomPayload.Id<ChantingStateS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "chanting_state"));

    public static final PacketCodec<RegistryByteBuf, ChantingStateS2CPacket> CODEC =
            PacketCodec.of(
                    (pkt, buf) -> {
                        buf.writeString(pkt.playerUuid().toString());
                        buf.writeBoolean(pkt.chanting());
                        buf.writeInt(pkt.chantTicks());
                    },
                    buf -> new ChantingStateS2CPacket(
                            UUID.fromString(buf.readString()), buf.readBoolean(), buf.readInt())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
