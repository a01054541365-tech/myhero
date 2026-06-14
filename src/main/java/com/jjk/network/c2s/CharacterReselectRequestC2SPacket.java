package com.jjk.network.c2s;

import com.jjk.JJKMod;
import com.jjk.character.CharacterRegistry;
import com.jjk.network.s2c.CharacterSelectFailS2CPacket;
import com.jjk.network.s2c.CharacterSelectS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

public record CharacterReselectRequestC2SPacket() implements CustomPayload {

    public static final CustomPayload.Id<CharacterReselectRequestC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "character_reselect_request"));

    public static final PacketCodec<RegistryByteBuf, CharacterReselectRequestC2SPacket> CODEC =
            PacketCodec.unit(new CharacterReselectRequestC2SPacket());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(CharacterReselectRequestC2SPacket packet, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            if (!JJKMod.getConfig().allowCharacterReselect) {
                ServerPlayNetworking.send(ctx.player(),
                    new CharacterSelectFailS2CPacket("reselect_disabled"));
                return;
            }
            ServerPlayNetworking.send(ctx.player(),
                new CharacterSelectS2CPacket(new ArrayList<>(CharacterRegistry.ids())));
        });
    }
}
