package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record BossBarUpdateS2CPacket(String characterId, int hp, int hpMax, int ce, int ceMax)
        implements CustomPayload {

    public static final CustomPayload.Id<BossBarUpdateS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "boss_bar_update"));

    public static final PacketCodec<RegistryByteBuf, BossBarUpdateS2CPacket> CODEC =
            PacketCodec.of(
                    (pkt, buf) -> {
                        buf.writeString(pkt.characterId());
                        buf.writeInt(pkt.hp());
                        buf.writeInt(pkt.hpMax());
                        buf.writeInt(pkt.ce());
                        buf.writeInt(pkt.ceMax());
                    },
                    buf -> new BossBarUpdateS2CPacket(
                            buf.readString(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt()
                    )
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
