package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record FullRevivalS2CPacket(UUID playerUuid) implements CustomPayload {

    public static final CustomPayload.Id<FullRevivalS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "full_revival"));

    public static final PacketCodec<RegistryByteBuf, FullRevivalS2CPacket> CODEC =
            PacketCodec.of(
                    (pkt, buf) -> buf.writeUuid(pkt.playerUuid()),
                    buf -> new FullRevivalS2CPacket(buf.readUuid())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
