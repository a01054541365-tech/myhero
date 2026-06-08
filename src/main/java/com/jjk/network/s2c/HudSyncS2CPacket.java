package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record HudSyncS2CPacket(
    float cePercent,
    float hpPercent,
    int grade,
    boolean inCombat,
    Map<String, Integer> skillCooldownsRemaining
) implements CustomPayload {

    public static final CustomPayload.Id<HudSyncS2CPacket> ID =
        new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "hud_sync"));

    public static final PacketCodec<RegistryByteBuf, HudSyncS2CPacket> CODEC = PacketCodec.of(
        (pkt, buf) -> {
            buf.writeFloat(pkt.cePercent());
            buf.writeFloat(pkt.hpPercent());
            buf.writeInt(pkt.grade());
            buf.writeBoolean(pkt.inCombat());
            Map<String, Integer> cds = pkt.skillCooldownsRemaining();
            buf.writeInt(cds.size());
            cds.forEach((k, v) -> {
                buf.writeString(k);
                buf.writeInt(v);
            });
        },
        buf -> {
            float ce = buf.readFloat();
            float hp = buf.readFloat();
            int grade = buf.readInt();
            boolean inCombat = buf.readBoolean();
            int size = buf.readInt();
            Map<String, Integer> cds = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                cds.put(buf.readString(), buf.readInt());
            }
            return new HudSyncS2CPacket(ce, hp, grade, inCombat, cds);
        }
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
