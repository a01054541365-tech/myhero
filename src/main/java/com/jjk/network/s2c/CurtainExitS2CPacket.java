package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record CurtainExitS2CPacket(UUID curtainOwnerId) implements CustomPayload {

    public static final CustomPayload.Id<CurtainExitS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "curtain_exit"));

    public static final PacketCodec<RegistryByteBuf, CurtainExitS2CPacket> CODEC =
            PacketCodec.of(
                    (pkt, buf) -> buf.writeString(pkt.curtainOwnerId().toString()),
                    buf -> new CurtainExitS2CPacket(UUID.fromString(buf.readString()))
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
