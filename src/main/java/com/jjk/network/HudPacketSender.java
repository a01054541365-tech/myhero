package com.jjk.network;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.entity.cursed.HomurakuiEntity;
import com.jjk.entity.cursed.HannamiNpcEntity;
import com.jjk.entity.cursed.JogoNpcEntity;
import com.jjk.entity.cursed.JuugoNpcEntity;
import com.jjk.entity.cursed.KotsibakuEntity;
import com.jjk.entity.cursed.MukiEntity;
import com.jjk.network.s2c.CEAuraSyncS2CPacket;
import com.jjk.network.s2c.EntityHealthSyncS2CPacket;
import com.jjk.network.s2c.HudSyncS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 서버 → 클라이언트 HUD 동기화 패킷 전송자.
// JJKMod.onInitialize()에서 TickScheduler에 등록.
public final class HudPacketSender {
    private HudPacketSender() {}

    // period=5 — CE/HP/등급/전투 상태 일괄 전송
    public static void sendHudSync(ServerPlayerEntity player) {
        if (JJKMod.getInstance() == null) return;
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data == null) return;

        float cePercent = data.ceMax > 0f ? Math.min(1f, data.ceCurrent / data.ceMax) : 0f;
        float hpPercent = data.hpMax > 0f ? Math.min(1f, data.hpCurrent / data.hpMax) : 0f;
        int gradeInt = gradeToInt(data.grade);

        long currentTick = player.getWorld().getTime();
        boolean inCombat = (currentTick - data.lastCombatTick) < 100L;

        Map<String, Integer> cooldowns = new HashMap<>();
        if (data.cooldowns != null) {
            data.cooldowns.forEach((key, expiry) -> {
                int remaining = (int) Math.max(0L, expiry - currentTick);
                if (remaining > 0) cooldowns.put(key, remaining);
            });
        }

        ServerPlayNetworking.send(player,
            new HudSyncS2CPacket(cePercent, hpPercent, gradeInt, inCombat, cooldowns));
    }

    // period=10 — 반경 32블록 이내 JJK 주령 체력 전송 (최대 20마리)
    public static void sendEntityHealthSync(ServerPlayerEntity player) {
        if (JJKMod.getInstance() == null) return;
        if (!(player.getWorld() instanceof ServerWorld sw)) return;

        List<LivingEntity> nearby = sw.getEntitiesByClass(
            LivingEntity.class,
            player.getBoundingBox().expand(32.0),
            e -> e.isAlive() && isJjkEntity(e));

        int count = 0;
        for (LivingEntity entity : nearby) {
            if (count >= 20) break;
            boolean isBoss = entity instanceof JogoNpcEntity || entity instanceof HannamiNpcEntity;
            ServerPlayNetworking.send(player, new EntityHealthSyncS2CPacket(
                entity.getId(),
                entity.getHealth(),
                entity.getMaxHealth(),
                entity.getName().getString(),
                isBoss));
            count++;
        }
    }

    // period=10 — 반경 30블록 이내 다른 플레이어의 CE 비율 전송 (G-1 CE 오라)
    public static void sendCEAuraSync(ServerPlayerEntity player) {
        if (JJKMod.getInstance() == null) return;
        if (!(player.getWorld() instanceof ServerWorld sw)) return;

        java.util.Map<Integer, Float> ceMap = new java.util.HashMap<>();
        for (ServerPlayerEntity other : sw.getPlayers()) {
            if (other == player) continue; // 자기 자신 제외
            if (other.squaredDistanceTo(player) > 900.0) continue; // 30블록²
            PlayerData otherData = JJKMod.getPlayerRepository().load(other.getUuid());
            if (otherData.characterId == null) continue; // NON_SORCERER 제외
            // 비술사 팀(NON_SORCERER)도 제외
            if (com.jjk.team.TeamManager.getTeam(otherData.characterId)
                    == com.jjk.team.TeamManager.Team.NON_SORCERER) continue;
            float cePercent = otherData.ceMax > 0f
                ? Math.min(1f, otherData.ceCurrent / otherData.ceMax) : 0f;
            ceMap.put(other.getId(), cePercent);
        }
        if (!ceMap.isEmpty()) {
            ServerPlayNetworking.send(player, new CEAuraSyncS2CPacket(ceMap));
        }
    }

    private static boolean isJjkEntity(LivingEntity e) {
        return e instanceof MukiEntity
            || e instanceof KotsibakuEntity
            || e instanceof HomurakuiEntity
            || e instanceof JogoNpcEntity
            || e instanceof HannamiNpcEntity
            || e instanceof JuugoNpcEntity;
    }

    private static int gradeToInt(String grade) {
        if (grade == null) return 5;
        return switch (grade) {
            case "special_grade" -> 0;
            case "semi_grade_1"  -> 1;
            case "grade_1"       -> 2;
            case "grade_2"       -> 3;
            case "grade_3"       -> 4;
            default              -> 5;
        };
    }
}
