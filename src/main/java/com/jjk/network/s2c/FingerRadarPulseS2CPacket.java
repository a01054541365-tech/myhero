package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record FingerRadarPulseS2CPacket(int posX, int posY, int posZ) implements CustomPayload {

    public static final CustomPayload.Id<FingerRadarPulseS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "finger_radar_pulse"));

    public static final PacketCodec<RegistryByteBuf, FingerRadarPulseS2CPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, FingerRadarPulseS2CPacket::posX,
                    PacketCodecs.VAR_INT, FingerRadarPulseS2CPacket::posY,
                    PacketCodecs.VAR_INT, FingerRadarPulseS2CPacket::posZ,
                    FingerRadarPulseS2CPacket::new);

    public BlockPos sourcePos() {
        return new BlockPos(posX, posY, posZ);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
