package com.jjk.event;

import com.jjk.JJKMod;
import com.jjk.audit.AuditLogger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.UUID;

/** 스쿠나 손가락 20개 만재 달성 시 발동. */
public final class FullRevivalEvents {
    private FullRevivalEvents() {}

    public static void trigger(UUID skunaPlayerUuid, MinecraftServer server) {
        AuditLogger auditLogger = JJKMod.getAuditLogger();
        if (auditLogger != null) {
            auditLogger.logEvent("finger_max_reached", skunaPlayerUuid,
                    "{\"fingerCount\":20}", 0L);
        }

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(skunaPlayerUuid);
        if (player != null) {
            player.sendMessage(Text.literal("§4§l[스쿠나] 20개의 손가락이 모였다..."), false);
        }

        // TODO Phase 3: SukunaSkillSet 완전체 해금 연동
        // JJKMod.getSkillRegistry().unlockFullSukuna(skunaPlayerUuid);
    }
}
