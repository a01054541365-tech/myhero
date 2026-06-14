package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record AchievementUnlockS2CPacket(String achievementId, String displayName) implements CustomPayload {

    public static final CustomPayload.Id<AchievementUnlockS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "achievement_unlock"));

    public static final PacketCodec<RegistryByteBuf, AchievementUnlockS2CPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.STRING, AchievementUnlockS2CPacket::achievementId,
                    PacketCodecs.STRING, AchievementUnlockS2CPacket::displayName,
                    AchievementUnlockS2CPacket::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
