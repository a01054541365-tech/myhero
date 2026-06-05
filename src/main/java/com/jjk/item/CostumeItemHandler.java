package com.jjk.item;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.CostumeSyncS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class CostumeItemHandler {
    private CostumeItemHandler() {}

    public static void apply(ServerPlayerEntity player, String costumeId) {
        if (!CostumeRegistry.isValid(costumeId)) {
            player.sendMessage(Text.literal("[JJK] 유효하지 않은 의상입니다."), false);
            return;
        }
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (!CostumeRegistry.isCompatible(costumeId, data.characterId)) {
            player.sendMessage(Text.literal("[JJK] 현재 캐릭터와 맞지 않는 의상입니다."), false);
            return;
        }
        data.costumeId = costumeId;
        JJKMod.getPlayerRepository().saveImmediate(data);

        CostumeSyncS2CPacket packet = new CostumeSyncS2CPacket(player.getUuid(), costumeId);
        for (ServerPlayerEntity p : player.getServerWorld().getPlayers()) {
            ServerPlayNetworking.send(p, packet);
        }
        player.sendMessage(Text.literal("[JJK] 의상이 변경되었습니다: " + costumeId), false);
    }
}
