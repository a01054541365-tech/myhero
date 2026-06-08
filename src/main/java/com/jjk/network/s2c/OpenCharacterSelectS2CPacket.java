package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.List;

public record OpenCharacterSelectS2CPacket(List<String> availableCharacterIds, String currentCharacterId)
        implements CustomPayload {

    public static final CustomPayload.Id<OpenCharacterSelectS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "open_character_select"));

    public static final PacketCodec<RegistryByteBuf, OpenCharacterSelectS2CPacket> CODEC =
            PacketCodec.tuple(
                PacketCodecs.STRING.collect(PacketCodecs.toList()),
                OpenCharacterSelectS2CPacket::availableCharacterIds,
                PacketCodecs.STRING,
                OpenCharacterSelectS2CPacket::currentCharacterId,
                OpenCharacterSelectS2CPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
