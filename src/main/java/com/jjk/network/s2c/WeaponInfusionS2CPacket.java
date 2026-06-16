package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record WeaponInfusionS2CPacket(int durationTicks, float multValue) implements CustomPayload {

    public static final CustomPayload.Id<WeaponInfusionS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "weapon_infusion"));

    public static final PacketCodec<RegistryByteBuf, WeaponInfusionS2CPacket> CODEC =
            PacketCodec.of(
                    (pkt, buf) -> { buf.writeInt(pkt.durationTicks()); buf.writeFloat(pkt.multValue()); },
                    buf -> new WeaponInfusionS2CPacket(buf.readInt(), buf.readFloat())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
