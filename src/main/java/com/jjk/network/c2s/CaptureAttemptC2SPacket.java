package com.jjk.network.c2s;

import com.jjk.JJKMod;
import com.jjk.entity.capture.CursedSpiritCaptureSystem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CaptureAttemptC2SPacket(int targetEntityId)
        implements CustomPayload {

    public static final CustomPayload.Id<CaptureAttemptC2SPacket> ID =
        new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "capture_attempt"));

    public static final PacketCodec<RegistryByteBuf, CaptureAttemptC2SPacket> CODEC = PacketCodec.of(
        (pkt, buf) -> buf.writeInt(pkt.targetEntityId()),
        buf -> new CaptureAttemptC2SPacket(buf.readInt())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(CaptureAttemptC2SPacket pkt, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() ->
            CursedSpiritCaptureSystem.attemptCapture(ctx.player(), pkt.targetEntityId()));
    }
}
