package com.jjk.server;

import com.jjk.JJKMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public final class AutoAnnouncer {

    private static final List<String> MESSAGES = List.of(
        "흑섬은 스킬 히트 직후 F키 타이밍 입력으로 발동합니다!",
        "/jj guide 로 가이드북을 받을 수 있습니다.",
        "주령 처치 시 경험치와 스쿠나 손가락을 획득할 수 있습니다.",
        "등급이 오를수록 강력한 스킬 슬롯이 해금됩니다.",
        "영역 전개는 특급 등급 이상에서 사용 가능합니다."
    );

    private int msgIndex = 0;
    // Long.MIN_VALUE so first tick-6000 trigger broadcasts immediately
    private long lastBroadcastTick = Long.MIN_VALUE / 2;

    public void tick(ServerPlayerEntity player) {
        long now = player.getWorld().getTime();
        if (now - lastBroadcastTick < 6000L) return;
        lastBroadcastTick = now;

        MinecraftServer server = JJKMod.getServer();
        if (server == null) return;
        String msg = MESSAGES.get(msgIndex);
        msgIndex = (msgIndex + 1) % MESSAGES.size();
        server.getPlayerManager().broadcast(Text.literal("[JJK 공지] " + msg), false);
    }
}
