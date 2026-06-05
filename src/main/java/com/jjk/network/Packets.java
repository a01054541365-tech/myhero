package com.jjk.network;

import com.jjk.network.c2s.ChantingC2SPacket;
import com.jjk.network.c2s.CharacterSelectC2SPacket;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import com.jjk.network.c2s.ShieldToggleC2SPacket;
import com.jjk.network.c2s.SkillUseC2SPacket;
import com.jjk.network.s2c.*;
import com.jjk.network.s2c.BossBarUpdateS2CPacket;
import com.jjk.network.s2c.CostumeSyncS2CPacket;
import com.jjk.network.s2c.FingerDropS2CPacket;
import com.jjk.network.s2c.SkillEffectS2CPacket;
import com.jjk.network.s2c.SkillCooldownSyncS2CPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class Packets {

    public static void register() {
        PayloadTypeRegistry.playC2S().register(SkillUseC2SPacket.ID, SkillUseC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(CharacterSelectC2SPacket.ID, CharacterSelectC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ShieldToggleC2SPacket.ID, ShieldToggleC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ChantingC2SPacket.ID, ChantingC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(NpcServiceC2SPacket.ID, NpcServiceC2SPacket.CODEC);

        PayloadTypeRegistry.playS2C().register(SkillResultS2CPacket.ID, SkillResultS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NpcOpenGuiS2CPacket.ID, NpcOpenGuiS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ZoneEnterS2CPacket.ID, ZoneEnterS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ZoneExitS2CPacket.ID, ZoneExitS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(AwakeningS2CPacket.ID, AwakeningS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CharacterInfoS2CPacket.ID, CharacterInfoS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CharacterSelectS2CPacket.ID, CharacterSelectS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CharacterConfirmS2CPacket.ID, CharacterConfirmS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CharacterSelectFailS2CPacket.ID, CharacterSelectFailS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(RespawnS2CPacket.ID, RespawnS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimationTriggerS2CPacket.ID, AnimationTriggerS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CurtainEnterS2CPacket.ID, CurtainEnterS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CurtainExitS2CPacket.ID, CurtainExitS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(ChantingStateS2CPacket.ID, ChantingStateS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(FingerDropS2CPacket.ID, FingerDropS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(BossBarUpdateS2CPacket.ID, BossBarUpdateS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SkillCooldownSyncS2CPacket.ID, SkillCooldownSyncS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CostumeSyncS2CPacket.ID, CostumeSyncS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SkillEffectS2CPacket.ID, SkillEffectS2CPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SkillUseC2SPacket.ID, SkillUseC2SPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(CharacterSelectC2SPacket.ID, CharacterSelectC2SPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(ShieldToggleC2SPacket.ID, ShieldToggleC2SPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(ChantingC2SPacket.ID, ChantingC2SPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(NpcServiceC2SPacket.ID, NpcServiceC2SPacket::handle);
    }
}
