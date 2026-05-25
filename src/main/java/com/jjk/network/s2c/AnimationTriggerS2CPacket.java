package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record AnimationTriggerS2CPacket(UUID targetUuid, byte animId) implements CustomPayload {

    public static final CustomPayload.Id<AnimationTriggerS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "anim_trigger"));

    public static final PacketCodec<RegistryByteBuf, AnimationTriggerS2CPacket> CODEC =
            PacketCodec.tuple(
                    Uuids.PACKET_CODEC, AnimationTriggerS2CPacket::targetUuid,
                    PacketCodecs.BYTE,   AnimationTriggerS2CPacket::animId,
                    AnimationTriggerS2CPacket::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
