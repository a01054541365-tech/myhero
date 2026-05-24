package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AwakeningS2CPacket(boolean active) implements CustomPayload {

    public static final CustomPayload.Id<AwakeningS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "awakening"));

    public static final PacketCodec<RegistryByteBuf, AwakeningS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.BOOL, AwakeningS2CPacket::active, AwakeningS2CPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
