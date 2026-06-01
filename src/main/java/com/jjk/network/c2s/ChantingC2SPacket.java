package com.jjk.network.c2s;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ChantingC2SPacket(String action) implements CustomPayload {

    public static final CustomPayload.Id<ChantingC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "chanting"));

    public static final PacketCodec<RegistryByteBuf, ChantingC2SPacket> CODEC =
            PacketCodec.of(
                    (pkt, buf) -> buf.writeString(pkt.action()),
                    buf -> new ChantingC2SPacket(buf.readString())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(ChantingC2SPacket packet, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            PlayerData data = JJKMod.getPlayerRepository().load(ctx.player().getUuid());
            long tick = ctx.player().getWorld().getTime();
            switch (packet.action()) {
                case "START" -> {
                    if (!ctx.player().isOnGround()) return;
                    JJKMod.getChantingHandler().startChant(data, tick);
                }
                case "CANCEL" -> JJKMod.getChantingHandler().cancelChant(data);
            }
            JJKMod.getPlayerRepository().save(data);
        });
    }
}
