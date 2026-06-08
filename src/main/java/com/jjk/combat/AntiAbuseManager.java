package com.jjk.combat;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AntiAbuseManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-anticheat");
    private static final int SEAL_TICKS = 600;  // 30초 봉인

    private final ConcurrentHashMap<UUID, AtomicInteger> flags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> previousRtt = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicInteger> macroFlags = new ConcurrentHashMap<>();

    /**
     * 의심 행동 플래그 추가. weight 누적에 따라 단계별 자동 제재.
     * 5+: 경고, 15+: 30초 스킬 봉인, 30+: 자동 킥.
     */
    public void flag(UUID playerId, String reason, int weight) {
        int total = flags.computeIfAbsent(playerId, k -> new AtomicInteger(0))
                         .addAndGet(weight);
        LOGGER.warn("[JJK-AC] player={} reason={} weight={} total={}", playerId, reason, weight, total);

        ServerPlayerEntity player = JJKMod.getServer() == null ? null
                : JJKMod.getServer().getPlayerManager().getPlayer(playerId);

        if (total >= 30) {
            if (player != null) {
                player.networkHandler.disconnect(
                        Text.literal("[JJK] 비정상 입력 감지로 연결이 종료되었습니다."));
            }
            LOGGER.warn("[JJK-AC] 자동 킥 실행. player={} reason={} flags={}", playerId, reason, total);
        } else if (total >= 15) {
            if (player != null) {
                player.sendMessage(Text.literal("[JJK] 비정상 입력 감지. 스킬이 30초 봉인됩니다."), false);
                PlayerData data = JJKMod.getPlayerRepository().load(playerId);
                long tick = player.getWorld().getTime();
                CooldownManager.set(data, "skill_seal", tick, SEAL_TICKS);
                JJKMod.getPlayerRepository().save(data);
            }
        } else if (total >= 5) {
            if (player != null) {
                player.sendMessage(
                        Text.literal("[JJK] ⚠ 비정상 입력이 감지되었습니다. 반복 시 제재됩니다."), true);
            }
        }
    }

    /**
     * 흑섬 매크로 의심 누적. 일반 flag(weight=2)도 함께 기록하며,
     * 누적 횟수를 반환해 호출자(BlackFlashHandler)가 강등 여부를 판단하게 한다.
     */
    public int flagMacro(UUID playerId) {
        int total = macroFlags.computeIfAbsent(playerId, k -> new AtomicInteger(0)).incrementAndGet();
        LOGGER.warn("[JJK-AC] 매크로 의심 누적. player={} total={}", playerId, total);
        flag(playerId, "macro_suspected", 2);
        return total;
    }

    public int getMacroFlags(UUID playerId) {
        AtomicInteger c = macroFlags.get(playerId);
        return c == null ? 0 : c.get();
    }

    public int getTotalFlags(UUID playerId) {
        AtomicInteger c = flags.get(playerId);
        return c == null ? 0 : c.get();
    }

    public void clearFlags(UUID playerId) {
        flags.remove(playerId);
    }

    /**
     * RTT 급등 감지 — 이전 RTT 대비 currentRtt가 200ms 이상 증가하면 true.
     * 내부적으로 previousRtt를 currentRtt로 업데이트.
     */
    public boolean checkRttSpike(UUID playerId, int currentRtt) {
        int prev = previousRtt.getOrDefault(playerId, currentRtt);
        previousRtt.put(playerId, currentRtt);
        return (currentRtt - prev) > 200;
    }
}
