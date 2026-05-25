package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SkillResultS2CPacket(int keyId, String result, float finalDamage) implements CustomPayload {

    public static final CustomPayload.Id<SkillResultS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "skill_result"));

    public static final PacketCodec<RegistryByteBuf, SkillResultS2CPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER,   SkillResultS2CPacket::keyId,
                    PacketCodecs.STRING,    SkillResultS2CPacket::result,
                    PacketCodecs.FLOAT,     SkillResultS2CPacket::finalDamage,
                    SkillResultS2CPacket::new
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
