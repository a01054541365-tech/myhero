package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record CurtainEnterS2CPacket(UUID curtainOwnerId, int radius) implements CustomPayload {

    public static final CustomPayload.Id<CurtainEnterS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "curtain_enter"));

    public static final PacketCodec<RegistryByteBuf, CurtainEnterS2CPacket> CODEC =
            PacketCodec.of(
                    (pkt, buf) -> {
                        buf.writeString(pkt.curtainOwnerId().toString());
                        buf.writeInt(pkt.radius());
                    },
                    buf -> new CurtainEnterS2CPacket(
                            UUID.fromString(buf.readString()), buf.readInt())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
