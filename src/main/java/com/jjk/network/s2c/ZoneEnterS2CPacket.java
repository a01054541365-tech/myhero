package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ZoneEnterS2CPacket(String domainId) implements CustomPayload {

    public static final CustomPayload.Id<ZoneEnterS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "zone_enter"));

    public static final PacketCodec<RegistryByteBuf, ZoneEnterS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, ZoneEnterS2CPacket::domainId, ZoneEnterS2CPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
