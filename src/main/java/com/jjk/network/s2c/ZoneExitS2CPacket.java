package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ZoneExitS2CPacket(String domainId) implements CustomPayload {

    public static final CustomPayload.Id<ZoneExitS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "zone_exit"));

    public static final PacketCodec<RegistryByteBuf, ZoneExitS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, ZoneExitS2CPacket::domainId, ZoneExitS2CPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
