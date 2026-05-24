package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CharacterInfoS2CPacket(String characterId, String grade, float ceMax, float ceCurrent) implements CustomPayload {

    public static final CustomPayload.Id<CharacterInfoS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "character_info"));

    public static final PacketCodec<RegistryByteBuf, CharacterInfoS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, CharacterInfoS2CPacket::characterId,
                    PacketCodecs.STRING, CharacterInfoS2CPacket::grade,
                    PacketCodecs.FLOAT, CharacterInfoS2CPacket::ceMax,
                    PacketCodecs.FLOAT, CharacterInfoS2CPacket::ceCurrent,
                    CharacterInfoS2CPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
