package com.jjk.network.c2s;

import com.jjk.JJKMod;
import com.jjk.npc.NpcServiceHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record NpcServiceC2SPacket(String npcId, String action, String param)
        implements CustomPayload {

    public static final CustomPayload.Id<NpcServiceC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "npc_service"));

    public static final PacketCodec<RegistryByteBuf, NpcServiceC2SPacket> CODEC =
            PacketCodec.of(
                    (pkt, buf) -> {
                        buf.writeString(pkt.npcId());
                        buf.writeString(pkt.action());
                        boolean hasParam = pkt.param() != null;
                        buf.writeBoolean(hasParam);
                        if (hasParam) buf.writeString(pkt.param());
                    },
                    buf -> {
                        String npcId  = buf.readString();
                        String action = buf.readString();
                        String param  = buf.readBoolean() ? buf.readString() : null;
                        return new NpcServiceC2SPacket(npcId, action, param);
                    }
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(NpcServiceC2SPacket packet, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> NpcServiceHandler.handle(packet, ctx.player()));
    }
}
