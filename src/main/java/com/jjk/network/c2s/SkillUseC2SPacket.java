package com.jjk.network.c2s;

import com.jjk.JJKMod;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.character.SkillRegistry;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.SkillCooldownSyncS2CPacket;
import com.jjk.network.s2c.SkillResultS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record SkillUseC2SPacket(UUID playerUuid, byte keyId, UUID targetUuid)
        implements CustomPayload {

    public static final CustomPayload.Id<SkillUseC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "skill_use"));

    public static final PacketCodec<RegistryByteBuf, SkillUseC2SPacket> CODEC =
            PacketCodec.of(
                    // ValueFirstEncoder: (value, buf)
                    (SkillUseC2SPacket pkt, RegistryByteBuf buf) -> {
                        buf.writeString(pkt.playerUuid().toString());
                        buf.writeByte(pkt.keyId());
                        boolean hasTarget = pkt.targetUuid() != null;
                        buf.writeBoolean(hasTarget);
                        if (hasTarget) buf.writeString(pkt.targetUuid().toString());
                    },
                    (RegistryByteBuf buf) -> {
                        UUID playerUuid = UUID.fromString(buf.readString());
                        byte keyId = buf.readByte();
                        UUID targetUuid = buf.readBoolean() ? UUID.fromString(buf.readString()) : null;
                        return new SkillUseC2SPacket(playerUuid, keyId, targetUuid);
                    }
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(SkillUseC2SPacket packet, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            int keyIdInt = packet.keyId() & 0xFF;
            if (keyIdInt > 5) {
                if (JJKMod.getAuditLogger() != null) {
                    JJKMod.getAuditLogger().logEvent("INVALID_PACKET", ctx.player().getUuid(),
                            "{\"keyId\":" + keyIdInt + "}", 0L);
                }
                return;
            }
            PlayerData data = JJKMod.getPlayerRepository().load(ctx.player().getUuid());
            if (data.characterId == null) return;
            // 천여주박: CE 0 시 스킬 사용 불가
            if (!JJKMod.getCEManager().canUseSkill(data)) {
                ServerPlayNetworking.send(ctx.player(),
                        new SkillResultS2CPacket(keyIdInt, SkillResult.CE_INSUFFICIENT.name(), 0f));
                return;
            }
            ISkillSet skillSet = SkillRegistry.get(data.characterId);
            if (skillSet == null) return;
            SkillResult result = skillSet.use(ctx.player(), keyIdInt);
            if (result == SkillResult.SUCCESS) {
                int cdTicks = skillSet.getCooldownTicks(keyIdInt);
                if (cdTicks > 0) {
                    ServerPlayNetworking.send(ctx.player(),
                            new SkillCooldownSyncS2CPacket(keyIdInt, cdTicks));
                }
            } else {
                ServerPlayNetworking.send(ctx.player(),
                        new SkillResultS2CPacket(keyIdInt, result.name(), 0f));
            }
        });
    }
}
