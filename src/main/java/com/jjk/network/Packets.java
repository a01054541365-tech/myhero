package com.jjk.network;

import com.jjk.network.c2s.CharacterSelectC2SPacket;
import com.jjk.network.c2s.SkillUseC2SPacket;
import com.jjk.network.s2c.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Packets {

    public static void register() {
        PayloadTypeRegistry.playC2S().register(SkillUseC2SPacket.ID, SkillUseC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CharacterSelectC2SPacket.ID, CharacterSelectC2SPacket.CODEC);

        PayloadTypeRegistry.playS2C().register(SkillResultS2CPacket.ID, SkillResultS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ZoneEnterS2CPacket.ID, ZoneEnterS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ZoneExitS2CPacket.ID, ZoneExitS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(AwakeningS2CPacket.ID, AwakeningS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CharacterInfoS2CPacket.ID, CharacterInfoS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CharacterSelectS2CPacket.ID, CharacterSelectS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CharacterConfirmS2CPacket.ID, CharacterConfirmS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CharacterSelectFailS2CPacket.ID, CharacterSelectFailS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RespawnS2CPacket.ID, RespawnS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimationTriggerS2CPacket.ID, AnimationTriggerS2CPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SkillUseC2SPacket.ID, SkillUseC2SPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(CharacterSelectC2SPacket.ID, CharacterSelectC2SPacket::handle);
    }
}
