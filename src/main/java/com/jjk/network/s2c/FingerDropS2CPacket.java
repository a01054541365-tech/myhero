package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FingerDropS2CPacket(int newFingerCount, boolean isMaxReached)
        implements CustomPayload {

    public static final CustomPayload.Id<FingerDropS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "finger_drop"));

    public static final PacketCodec<RegistryByteBuf, FingerDropS2CPacket> CODEC =
            PacketCodec.of(
                    (pkt, buf) -> {
                        buf.writeInt(pkt.newFingerCount());
                        buf.writeBoolean(pkt.isMaxReached());
                    },
                    buf -> new FingerDropS2CPacket(buf.readInt(), buf.readBoolean())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
