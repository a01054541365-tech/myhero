package com.jjk.network.c2s;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ShieldToggleC2SPacket(boolean active) implements CustomPayload {

    public static final CustomPayload.Id<ShieldToggleC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "shield_toggle"));

    public static final PacketCodec<RegistryByteBuf, ShieldToggleC2SPacket> CODEC =
            PacketCodec.of(
                    (pkt, buf) -> buf.writeBoolean(pkt.active()),
                    buf -> new ShieldToggleC2SPacket(buf.readBoolean())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(ShieldToggleC2SPacket packet, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            PlayerData data = JJKMod.getPlayerRepository().load(ctx.player().getUuid());
            data.shieldActive = packet.active();
            JJKMod.getPlayerRepository().save(data);
        });
    }
}
