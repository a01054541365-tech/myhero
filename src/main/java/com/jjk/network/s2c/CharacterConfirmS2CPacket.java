package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CharacterConfirmS2CPacket(String characterId) implements CustomPayload {

    public static final CustomPayload.Id<CharacterConfirmS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "character_confirm"));

    public static final PacketCodec<RegistryByteBuf, CharacterConfirmS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, CharacterConfirmS2CPacket::characterId,
                    CharacterConfirmS2CPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
