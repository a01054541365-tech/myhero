package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record CEAuraSyncS2CPacket(Map<Integer, Float> entityCePercent)
        implements CustomPayload {

    public static final CustomPayload.Id<CEAuraSyncS2CPacket> ID =
        new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "ce_aura_sync"));

    public static final PacketCodec<RegistryByteBuf, CEAuraSyncS2CPacket> CODEC = PacketCodec.of(
        (pkt, buf) -> {
            Map<Integer, Float> map = pkt.entityCePercent();
            buf.writeInt(map.size());
            map.forEach((k, v) -> {
                buf.writeInt(k);
                buf.writeFloat(v);
            });
        },
        buf -> {
            int size = buf.readInt();
            Map<Integer, Float> map = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                map.put(buf.readInt(), buf.readFloat());
            }
            return new CEAuraSyncS2CPacket(map);
        }
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
