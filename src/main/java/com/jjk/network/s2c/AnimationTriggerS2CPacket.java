package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record AnimationTriggerS2CPacket(UUID targetUuid, byte animId) implements CustomPayload {

    public static final CustomPayload.Id<AnimationTriggerS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "anim_trigger"));

    public static final PacketCodec<RegistryByteBuf, AnimationTriggerS2CPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING,  pkt -> pkt.targetUuid().toString(),
                    PacketCodecs.INTEGER, pkt -> (int) pkt.animId(),
                    (str, i) -> new AnimationTriggerS2CPacket(UUID.fromString(str), (byte) i.intValue())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
