package com.jjk.event;

import com.jjk.JJKMod;
import com.jjk.audit.AuditLogger;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.FullRevivalS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

/** 스쿠나 손가락 20개 만재 달성 시 발동. */
public final class FullRevivalEvents {
    private FullRevivalEvents() {}

    // mangaExpEnabled 시 해금되는 스쿠나 완전체 스킬 ID 목록
    private static final List<String> MANGA_UNLOCK_SKILLS =
            List.of("sukuna_sekai_kirisaki", "sukuna_fuga");

    public static void trigger(UUID sukunaPlayerUuid, MinecraftServer server) {
        AuditLogger auditLogger = JJKMod.getAuditLogger();
        if (auditLogger != null) {
            auditLogger.logEvent("finger_max_reached", sukunaPlayerUuid,
                    "{\"fingerCount\":20}", 0L);
        }

        ServerPlayerEntity player = server.getPlayerManager().getPlayer(sukunaPlayerUuid);
        if (player != null) {
            player.sendMessage(Text.literal("§4§l[스쿠나] 20개의 손가락이 모였다..."), false);
        }

        // mangaExpEnabled: 만화 원작 초월 스킬 해금
        if (JJKMod.getConfig().mangaExpEnabled) {
            PlayerData data = JJKMod.getPlayerRepository().load(sukunaPlayerUuid);
            for (String skillId : MANGA_UNLOCK_SKILLS) {
                if (!data.unlockedSkills.contains(skillId)) {
                    data.unlockedSkills.add(skillId);
                }
            }
            JJKMod.getPlayerRepository().saveImmediate(data);
        }

        // 전원에게 완전부활 S2C 알림
        var pkt = new FullRevivalS2CPacket(sukunaPlayerUuid);
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(p, pkt);
        }
    }
}
