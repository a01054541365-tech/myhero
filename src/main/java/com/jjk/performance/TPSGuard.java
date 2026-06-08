package com.jjk.performance;

import com.jjk.discord.DiscordWebhook;
import com.jjk.domain.DomainBlockQueue;
import com.jjk.effect.EffectDeferQueue;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 서버 TPS를 감시해 EffectDeferQueue·DomainBlockQueue의 부하를 단계적으로 줄인다.
 * 15~16 사이는 히스테리시스 구간으로 직전 단계를 유지해 빈번한 전환을 막는다.
 */
public class TPSGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-tps-guard");

    private enum Level { NORMAL, WARNING, CRITICAL }

    private static final float TPS_CRITICAL = 10f;
    private static final float TPS_WARNING  = 15f;
    private static final float TPS_RECOVERY = 16f;

    private static final int CRITICAL_VIEW_DISTANCE = 4;
    private static final int DEFAULT_MAX_BLOCKS_PER_TICK = 200;
    private static final int HALVED_MAX_BLOCKS_PER_TICK  = 100;

    private final EffectDeferQueue effectDeferQueue;
    private final DomainBlockQueue domainBlockQueue;

    private Level currentLevel = Level.NORMAL;
    private int savedViewDistance = -1;

    public TPSGuard(EffectDeferQueue effectDeferQueue, DomainBlockQueue domainBlockQueue) {
        this.effectDeferQueue = effectDeferQueue;
        this.domainBlockQueue = domainBlockQueue;
    }

    /** TickScheduler.registerServerTask 등록용 — 매 틱(또는 주기적으로) 호출. */
    public void tick(MinecraftServer server) {
        float mspt = server.getAverageTickTime();
        float tps = mspt > 0 ? Math.min(20f, 1000f / mspt) : 20f;

        Level target = resolveLevel(tps);
        if (target == currentLevel) return;

        switch (target) {
            case WARNING -> applyWarning(server);
            case CRITICAL -> applyCritical(server, tps);
            case NORMAL -> applyRecovery(server);
        }
        currentLevel = target;
    }

    private Level resolveLevel(float tps) {
        if (tps < TPS_CRITICAL) return Level.CRITICAL;
        if (tps < TPS_WARNING) return Level.WARNING;
        if (tps >= TPS_RECOVERY) return Level.NORMAL;
        return currentLevel; // 15.0 ~ 16.0 — 직전 단계 유지 (히스테리시스)
    }

    private void applyWarning(MinecraftServer server) {
        LOGGER.warn("[JJK] TPS 저하 감지 — 이펙트/블록 처리량 절반으로 축소");
        effectDeferQueue.setParticleRate(0.5f);
        domainBlockQueue.setMaxBlocksPerTick(HALVED_MAX_BLOCKS_PER_TICK);

        if (currentLevel == Level.CRITICAL) {
            domainBlockQueue.blockNewDeployments(false);
            restoreViewDistance(server);
        }
    }

    private void applyCritical(MinecraftServer server, float tps) {
        LOGGER.warn("[JJK] TPS 위험 수준 — 파티클 중단, 신규 영역 전개 차단, 렌더 거리 축소");
        effectDeferQueue.setParticleRate(0.0f);
        domainBlockQueue.setMaxBlocksPerTick(HALVED_MAX_BLOCKS_PER_TICK);
        domainBlockQueue.blockNewDeployments(true);

        if (savedViewDistance < 0) {
            savedViewDistance = server.getPlayerManager().getViewDistance();
        }
        server.getPlayerManager().setViewDistance(CRITICAL_VIEW_DISTANCE);

        DiscordWebhook.sendAsync(String.format("[TPS ALERT] 현재 TPS: %.1f", tps));
    }

    private void applyRecovery(MinecraftServer server) {
        LOGGER.info("[JJK] TPS 회복 — 정상 처리량으로 복원");
        effectDeferQueue.setParticleRate(1.0f);
        domainBlockQueue.setMaxBlocksPerTick(DEFAULT_MAX_BLOCKS_PER_TICK);
        domainBlockQueue.blockNewDeployments(false);
        restoreViewDistance(server);
    }

    private void restoreViewDistance(MinecraftServer server) {
        if (savedViewDistance >= 0) {
            server.getPlayerManager().setViewDistance(savedViewDistance);
            savedViewDistance = -1;
        }
    }
}
