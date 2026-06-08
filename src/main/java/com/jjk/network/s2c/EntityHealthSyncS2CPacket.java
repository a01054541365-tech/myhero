package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EntityHealthSyncS2CPacket(int entityId, float hpCurrent, float hpMax, String displayName, boolean isBoss)
        implements CustomPayload {

    public static final CustomPayload.Id<EntityHealthSyncS2CPacket> ID =
        new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "entity_health_sync"));

    public static final PacketCodec<RegistryByteBuf, EntityHealthSyncS2CPacket> CODEC = PacketCodec.of(
        (pkt, buf) -> {
            buf.writeInt(pkt.entityId());
            buf.writeFloat(pkt.hpCurrent());
            buf.writeFloat(pkt.hpMax());
            buf.writeString(pkt.displayName());
            buf.writeBoolean(pkt.isBoss());
        },
        buf -> new EntityHealthSyncS2CPacket(
            buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readString(), buf.readBoolean())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
