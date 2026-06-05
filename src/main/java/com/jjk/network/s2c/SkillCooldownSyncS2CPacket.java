package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SkillCooldownSyncS2CPacket(int keyId, int cooldownTicks) implements CustomPayload {

    public static final CustomPayload.Id<SkillCooldownSyncS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "skill_cd_sync"));

    public static final PacketCodec<RegistryByteBuf, SkillCooldownSyncS2CPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, SkillCooldownSyncS2CPacket::keyId,
                    PacketCodecs.INTEGER, SkillCooldownSyncS2CPacket::cooldownTicks,
                    SkillCooldownSyncS2CPacket::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
