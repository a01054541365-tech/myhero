package com.jjk.network.c2s;

import com.jjk.JJKMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** 클라이언트가 흑섬 입력 시점(클라이언트 타임스탬프 ms)을 보고 — 매크로 탐지용. */
public record BlackFlashInputC2SPacket(long clientTimestampMs) implements CustomPayload {

    public static final CustomPayload.Id<BlackFlashInputC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "black_flash_input"));

    public static final PacketCodec<RegistryByteBuf, BlackFlashInputC2SPacket> CODEC = PacketCodec.of(
            (pkt, buf) -> buf.writeLong(pkt.clientTimestampMs()),
            buf -> new BlackFlashInputC2SPacket(buf.readLong())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(BlackFlashInputC2SPacket packet, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() ->
                JJKMod.getBlackFlashHandler().onTimingReport(ctx.player().getUuid(), packet.clientTimestampMs()));
    }
}
