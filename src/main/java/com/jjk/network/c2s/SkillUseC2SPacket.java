package com.jjk.network.c2s;

import com.jjk.JJKMod;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.character.SkillRegistry;
import com.jjk.data.PlayerData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SkillUseC2SPacket(int keyId) implements CustomPayload {

    public static final CustomPayload.Id<SkillUseC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "skill_use"));

    public static final PacketCodec<RegistryByteBuf, SkillUseC2SPacket> CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, SkillUseC2SPacket::keyId, SkillUseC2SPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(SkillUseC2SPacket packet, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            PlayerData data = JJKMod.getPlayerRepository().load(ctx.player().getUuid());
            if (data.characterId == null) return;
            ISkillSet skillSet = SkillRegistry.get(data.characterId);
            if (skillSet == null) return;
            SkillResult result = skillSet.use(ctx.player(), packet.keyId());
            // TODO: send SkillResultS2CPacket back
        });
    }
}
