package com.jjk.network.c2s;

import com.jjk.JJKMod;
import com.jjk.character.CharacterCommandService;
import com.jjk.network.s2c.CharacterConfirmS2CPacket;
import com.jjk.network.s2c.CharacterSelectFailS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CharacterSelectC2SPacket(String characterId) implements CustomPayload {

    public static final CustomPayload.Id<CharacterSelectC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "character_select"));

    public static final PacketCodec<RegistryByteBuf, CharacterSelectC2SPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, CharacterSelectC2SPacket::characterId,
                    CharacterSelectC2SPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(CharacterSelectC2SPacket packet, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            CharacterCommandService service = new CharacterCommandService();
            CharacterCommandService.SelectResult result = service.select(ctx.player(), packet.characterId());
            switch (result) {
                case OK ->
                    ServerPlayNetworking.send(ctx.player(),
                        new CharacterConfirmS2CPacket(packet.characterId()));
                case DUPLICATE_BLOCKED ->
                    ServerPlayNetworking.send(ctx.player(),
                        new CharacterSelectFailS2CPacket("duplicate"));
                case GRADE_INSUFFICIENT ->
                    ServerPlayNetworking.send(ctx.player(),
                        new CharacterSelectFailS2CPacket("grade_insufficient"));
                default ->
                    ServerPlayNetworking.send(ctx.player(),
                        new CharacterSelectFailS2CPacket("unknown"));
            }
        });
    }
}
