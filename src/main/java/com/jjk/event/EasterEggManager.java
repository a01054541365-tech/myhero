package com.jjk.event;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.FingerRadarPulseS2CPacket;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * 이스터에그 시스템:
 *  1) "연어" 채팅 이누마키 리액션
 *  2) 쿠사카베 NPC 공격 업적
 *  3) 스쿠나 손가락 레이더 (20틱 주기)
 */
public class EasterEggManager {

    private static final int FINGER_RADAR_MIN    = 10;
    private static final double RADAR_RANGE_SQ   = 100.0 * 100.0;

    /** onInitialize() 에서 1회 호출 — 이벤트 리스너 등록. */
    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            if (message.getContent().getString().contains("연어")) {
                MinecraftServer server = sender.getServer();
                if (server != null) {
                    server.execute(() -> {
                        ServerPlayerEntity inumaki = findInumakiPlayer(server);
                        String name = inumaki != null
                                ? inumaki.getName().getString()
                                : "이누마키";
                        Text response = Text.literal("§b[" + name + "] §f연어연어 🐟");
                        server.getPlayerManager().broadcast(response, false);
                    });
                }
            }
            return true;
        });
    }

    /** NpcEntity.damage()에서 호출 — 쿠사카베 NPC 공격 업적. */
    public static void onNpcAttacked(String npcId, ServerPlayerEntity attacker) {
        if ("kusakabe".equals(npcId) && JJKMod.getAchievementManager() != null) {
            JJKMod.getAchievementManager().unlock(attacker, "attack_npc");
        }
    }

    /** TickScheduler.registerServerTask(EasterEggManager::tickFingerRadar, 20) 으로 등록. */
    public static void tickFingerRadar(MinecraftServer server) {
        for (ServerPlayerEntity sukunaPlayer : server.getPlayerManager().getPlayerList()) {
            PlayerData data = JJKMod.getPlayerRepository().load(sukunaPlayer.getUuid());
            if (!"sukuna".equals(data.characterId)) continue;
            if (data.fingerCount < FINGER_RADAR_MIN) continue;

            BlockPos sukunaPos = sukunaPlayer.getBlockPos();
            FingerRadarPulseS2CPacket pkt = new FingerRadarPulseS2CPacket(
                    sukunaPos.getX(), sukunaPos.getY(), sukunaPos.getZ());

            for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
                if (other.getUuid().equals(sukunaPlayer.getUuid())) continue;
                if (sukunaPlayer.squaredDistanceTo(other) <= RADAR_RANGE_SQ) {
                    ServerPlayNetworking.send(other, pkt);
                }
            }
        }
    }

    private static ServerPlayerEntity findInumakiPlayer(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            PlayerData data = JJKMod.getPlayerRepository().load(p.getUuid());
            if ("inumaki".equals(data.characterId)) return p;
        }
        return null;
    }
}
