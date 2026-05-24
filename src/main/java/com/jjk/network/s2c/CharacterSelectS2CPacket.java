package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.List;

public record CharacterSelectS2CPacket(List<String> availableCharacters) implements CustomPayload {

    public static final CustomPayload.Id<CharacterSelectS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "character_select_list"));

    public static final PacketCodec<RegistryByteBuf, CharacterSelectS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING.collect(PacketCodecs.toList()),
                    CharacterSelectS2CPacket::availableCharacters, CharacterSelectS2CPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
