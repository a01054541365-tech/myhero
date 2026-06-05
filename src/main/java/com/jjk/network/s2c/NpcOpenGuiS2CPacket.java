package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record NpcOpenGuiS2CPacket(String npcId, String payload) implements CustomPayload {

    public static final CustomPayload.Id<NpcOpenGuiS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "npc_open_gui"));

    public static final PacketCodec<RegistryByteBuf, NpcOpenGuiS2CPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, NpcOpenGuiS2CPacket::npcId,
                    PacketCodecs.STRING, NpcOpenGuiS2CPacket::payload,
                    NpcOpenGuiS2CPacket::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
