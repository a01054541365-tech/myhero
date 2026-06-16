package com.jjk.network.c2s;

import com.jjk.JJKMod;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.character.SkillRegistry;
import com.jjk.combat.CCManager;
import com.jjk.data.Grade;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.SkillCooldownSyncS2CPacket;
import com.jjk.network.s2c.SkillResultS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

    private static final double BINDING_RANGE = 6.0;

    public static void handle(SkillUseC2SPacket packet, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            int keyIdInt = packet.keyId() & 0xFF;
            if (keyIdInt > 6) {
                if (JJKMod.getAuditLogger() != null) {
                    JJKMod.getAuditLogger().logEvent("INVALID_PACKET", ctx.player().getUuid(),
                            "{\"keyId\":" + keyIdInt + "}", 0L);
                }
                return;
            }
            PlayerData data = JJKMod.getPlayerRepository().load(ctx.player().getUuid());
            if (data.characterId == null) return;
            // J-2-3: 격리된 플레이어는 스킬 발동 전면 차단
            if (data.quarantined) {
                if (JJKMod.getAuditLogger() != null) {
                    JJKMod.getAuditLogger().logEvent("QUARANTINE_BLOCK",
                            ctx.player().getUuid(), "{\"keyId\":" + keyIdInt + "}", 0L);
                }
                ServerPlayNetworking.send(ctx.player(),
                        new SkillResultS2CPacket(keyIdInt, SkillResult.FAIL_QUARANTINED.name(), 0f));
                return;
            }
            // 천여주박: CE 0 시 스킬 사용 불가
            if (!JJKMod.getCEManager().canUseSkill(data)) {
                ServerPlayNetworking.send(ctx.player(),
                        new SkillResultS2CPacket(keyIdInt, SkillResult.CE_INSUFFICIENT.name(), 0f));
                return;
            }

            long tick = ctx.player().getWorld().getTime();

            // keyId=6 (T키): 모든 캐릭터 공통 속박
            if (keyIdInt == 6) {
                handleBinding(ctx.player(), data, tick);
                return;
            }

            ISkillSet skillSet = SkillRegistry.get(data.characterId);
            if (skillSet == null) return;
            String skillName = skillSet.getSkillName(keyIdInt);
            if (data.sealExpireTick > tick && data.sealedSkills.contains(skillName)) {
                ServerPlayNetworking.send(ctx.player(),
                        new SkillResultS2CPacket(keyIdInt, SkillResult.FAIL_SKILL_SEALED.name(), 0f));
                return;
            }
            SkillResult result = skillSet.use(ctx.player(), keyIdInt);

            // keyId=5 (C키): 캐릭터 전용 스킬 없으면 간이영역으로 폴백
            if (keyIdInt == 5 && result == SkillResult.NOT_IMPLEMENTED) {
                handleSimpleBarrier(ctx.player(), data, tick);
                return;
            }

            if (result == SkillResult.SUCCESS) {
                PlayerData fresh = JJKMod.getPlayerRepository().load(ctx.player().getUuid());
                fresh.lastUsedSkillId = skillName;
                JJKMod.getPlayerRepository().save(fresh);

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

    private static void handleSimpleBarrier(ServerPlayerEntity player, PlayerData data, long tick) {
        if (data.simpleBarrierActive) {
            // 비활성화
            data.simpleBarrierActive = false;
            data.fallingBlossomActive = false;
        } else {
            // 활성화: 초기 CE 소모
            float activateCost = data.ceMax * JJKMod.getConfig().simpleBarrierCostActivate();
            if (data.ceCurrent < activateCost) {
                ServerPlayNetworking.send(player,
                        new SkillResultS2CPacket(5, SkillResult.CE_INSUFFICIENT.name(), 0f));
                return;
            }
            data.ceCurrent -= activateCost;
            data.simpleBarrierActive = true;
            // 낙화의 정: 1급 이상은 간이영역 활성화 시 자동으로 활성
            if (data.grade != null && data.grade.ordinal() >= Grade.GRADE_1.ordinal()) {
                data.fallingBlossomActive = true;
                data.fallingBlossomUntil = Long.MAX_VALUE;
            }
        }
        JJKMod.getPlayerRepository().save(data);
    }

    private static void handleBinding(ServerPlayerEntity player, PlayerData data, long tick) {
        ServerWorld world = (ServerWorld) player.getWorld();
        ServerPlayerEntity target = null;
        double minDistSq = BINDING_RANGE * BINDING_RANGE;
        for (ServerPlayerEntity other : world.getPlayers()) {
            if (other.getUuid().equals(player.getUuid())) continue;
            PlayerData otherData = JJKMod.getPlayerRepository().load(other.getUuid());
            if (JJKMod.getTeamManager().isSameTeam(data, otherData)) continue;
            double distSq = player.squaredDistanceTo(other);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                target = other;
            }
        }
        if (target == null) {
            ServerPlayNetworking.send(player,
                    new SkillResultS2CPacket(6, SkillResult.FAIL.name(), 0f));
            return;
        }
        PlayerData targetData = JJKMod.getPlayerRepository().load(target.getUuid());
        int sealTicks = JJKMod.getConfig().sealDurationTicks;
        CCManager.tryApplyCC(targetData, "bind", sealTicks, tick);
        JJKMod.getPlayerRepository().save(targetData);

        data.bindingVowDeclaredTick = tick;
        JJKMod.getBindingVowSystem().declareVow(player.getUuid(), target.getUuid());
        JJKMod.getPlayerRepository().save(data);

        ServerPlayNetworking.send(player,
                new SkillResultS2CPacket(6, SkillResult.SUCCESS.name(), 0f));
    }
}
