package com.jjk.achievement;

import com.jjk.JJKMod;
import com.jjk.audit.AuditLogger;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AchievementUnlockS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AchievementManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AchievementManager.class);

    public void unlock(ServerPlayerEntity player, String achievementId) {
        JJKAchievement achievement = AchievementRegistry.get(achievementId);
        if (achievement == null) {
            LOGGER.warn("[Achievement] 미등록 ID: {}", achievementId);
            return;
        }

        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.unlockedAchievements.contains(achievementId)) return;

        data.unlockedAchievements.add(achievementId);
        JJKMod.getPlayerRepository().saveImmediate(data);

        if (JJKMod.getServer() != null) {
            ServerPlayNetworking.send(player,
                    new AchievementUnlockS2CPacket(achievementId, achievement.displayName()));
        }

        AuditLogger auditLogger = JJKMod.getAuditLogger();
        if (auditLogger != null) {
            auditLogger.logEvent("achievement_unlock", player.getUuid(),
                    "{\"id\":\"" + achievementId + "\"}", 0L);
        }
        LOGGER.info("[Achievement] {} → {}", player.getNameForScoreboard(), achievementId);
    }

    public boolean check(ServerPlayerEntity player, String achievementId) {
        return check(player.getUuid(), achievementId);
    }

    public boolean check(UUID playerUuid, String achievementId) {
        PlayerData data = JJKMod.getPlayerRepository().load(playerUuid);
        return data.unlockedAchievements.contains(achievementId);
    }

    /** 테스트용 — MC 서버 없이 PlayerData 레벨에서 unlock 처리. 패킷 미전송. */
    boolean unlockData(UUID playerUuid, String achievementId) {
        if (AchievementRegistry.get(achievementId) == null) return false;
        PlayerData data = JJKMod.getPlayerRepository().load(playerUuid);
        if (data.unlockedAchievements.contains(achievementId)) return false;
        data.unlockedAchievements.add(achievementId);
        JJKMod.getPlayerRepository().saveImmediate(data);
        return true;
    }
}
