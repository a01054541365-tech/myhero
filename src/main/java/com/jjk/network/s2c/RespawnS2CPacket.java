package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RespawnS2CPacket(int delayTicks) implements CustomPayload {

    public static final CustomPayload.Id<RespawnS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "respawn"));

    public static final PacketCodec<RegistryByteBuf, RespawnS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, RespawnS2CPacket::delayTicks, RespawnS2CPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
