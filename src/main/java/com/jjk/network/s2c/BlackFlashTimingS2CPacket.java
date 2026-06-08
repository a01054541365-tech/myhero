package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BlackFlashTimingS2CPacket(boolean show, int windowStartTick, int windowEndTick)
        implements CustomPayload {

    public static final CustomPayload.Id<BlackFlashTimingS2CPacket> ID =
        new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "black_flash_timing"));

    public static final PacketCodec<RegistryByteBuf, BlackFlashTimingS2CPacket> CODEC = PacketCodec.of(
        (pkt, buf) -> {
            buf.writeBoolean(pkt.show());
            buf.writeInt(pkt.windowStartTick());
            buf.writeInt(pkt.windowEndTick());
        },
        buf -> new BlackFlashTimingS2CPacket(buf.readBoolean(), buf.readInt(), buf.readInt())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
