package com.jjk.server;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.text.Text;

public final class WelcomeHandler {

    private WelcomeHandler() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            player.sendMessage(Text.literal("=== JJK 서버에 오신 것을 환영합니다! ==="), false);
            player.sendMessage(Text.literal("/jj guide — 가이드북 수령"), false);
            player.sendMessage(Text.literal("/jj status — 내 상태 확인"), false);
            player.sendMessage(Text.literal("/jj select <이름> — 캐릭터 선택"), false);

            PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
            if (data.characterId == null) {
                player.sendMessage(Text.literal("[신규] 캐릭터를 먼저 선택하세요!"), false);
            }
        });
    }
}
