package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record CostumeSyncS2CPacket(UUID targetUuid, String costumeId)
        implements CustomPayload {

    public static final CustomPayload.Id<CostumeSyncS2CPacket> ID =
        new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "costume_sync"));

    public static final PacketCodec<RegistryByteBuf, CostumeSyncS2CPacket> CODEC =
        PacketCodec.tuple(
            PacketCodecs.STRING.xmap(UUID::fromString, UUID::toString),
            CostumeSyncS2CPacket::targetUuid,
            PacketCodecs.STRING,
            CostumeSyncS2CPacket::costumeId,
            CostumeSyncS2CPacket::new
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
