package com.jjk.npc;

import com.jjk.JJKMod;
import com.jjk.api.skill.SkillResult;
import com.jjk.data.PlayerData;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import com.jjk.network.s2c.SkillResultS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class NpcServiceHandler {

    public static void handle(NpcServiceC2SPacket packet, ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();

        SkillResult result = switch (packet.npcId()) {
            case "zenin_storage" -> ZeninService.handle(packet, data, player, tick);
            case "kusakabe"      -> KusakabeService.handle(packet, data, player, tick);
            case "shoko"         -> ShokoService.handle(packet, data, player, tick);
            case "gojo_shiyu"    -> GojoShiyuService.handle(packet, data, player, tick);
            case "ijichi"        -> IjichiService.handle(packet, data, player, tick);
            case "yaga"          -> YagaService.handle(packet, data, player, tick);
            case "nahobino"      -> NahovinoService.handle(packet, data, player, tick);
            default              -> SkillResult.FAIL;
        };

        ServerPlayNetworking.send(player,
            new SkillResultS2CPacket(0,
                "npc_" + packet.npcId() + "_" + packet.action() + "_" + result.name(),
                0f));
    }

    private NpcServiceHandler() {}
}
