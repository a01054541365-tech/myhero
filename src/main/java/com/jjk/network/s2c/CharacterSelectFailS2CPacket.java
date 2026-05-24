package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CharacterSelectFailS2CPacket(String reason) implements CustomPayload {

    public static final CustomPayload.Id<CharacterSelectFailS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "character_select_fail"));

    public static final PacketCodec<RegistryByteBuf, CharacterSelectFailS2CPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, CharacterSelectFailS2CPacket::reason,
                    CharacterSelectFailS2CPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
