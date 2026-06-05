package com.jjk.domain;

import net.minecraft.util.math.BlockPos;
import java.util.UUID;

public class DomainInstance {

    public final UUID instanceId;
    public final UUID ownerUuid;
    public final String domainId;
    public final BlockPos center;
    public final boolean isOpen;
    public final boolean isIncomplete;
    public final float maxRadius;
    public final boolean sureHitActive;
    public final boolean autoTargetAll;

    public float wallHp;
    public float currentRadius;
    public int ticksAlive;
    public long expireAtTick;
    public float ownerDamageReduction = 0f;
    public long deployedAtTick = 0L;
    // NPC 영역 여부 — true면 tickDomains에서 플레이어 소유자 조회 스킵
    public boolean npcOwned = false;

    // 소유자 진영 — deployDomain 시점에 TeamManager.getTeam()으로 세팅
    public com.jjk.team.TeamManager.Team team;

    public DomainInstance(UUID ownerUuid, String domainId, BlockPos center,
                          float wallHp, float radius, boolean isOpen, boolean isIncomplete,
                          boolean sureHitActive, boolean autoTargetAll) {
        this.instanceId = UUID.randomUUID();
        this.ownerUuid = ownerUuid;
        this.domainId = domainId;
        this.center = center;
        // §8: 개방형은 wallHp = 0, 결계형은 domains.json 값 사용
        this.wallHp = isOpen ? 0 : wallHp;
        this.maxRadius = radius;
        this.currentRadius = radius;
        this.isOpen = isOpen;
        this.isIncomplete = isIncomplete;
        this.sureHitActive = sureHitActive;
        this.autoTargetAll = autoTargetAll;
        this.ticksAlive = 0;
    }

    // §8-2: 같은 팀 영역 2개 이상 → 반경 절반으로 제한
    public void applyTeamLimit(int teamDomainCount) {
        if (teamDomainCount >= 2) this.currentRadius = this.maxRadius / 2;
    }

    // 팀 영역 수 감소 시 반경 복원
    public void restoreRadius() {
        this.currentRadius = this.maxRadius;
    }

    private static final int STARTUP_TICKS = 20;

    /**
     * 영역 필중(sureHit) 활성 여부 — deployedAtTick 기준 20틱 startup 후 활성.
     * sureHitActive=false(정의 기준)인 영역은 항상 false.
     */
    public boolean isSureHitReady(long currentTick) {
        return sureHitActive && currentTick >= deployedAtTick + STARTUP_TICKS;
    }

    public boolean isExpired(long currentTick) {
        if (expireAtTick > 0 && currentTick >= expireAtTick) return true;
        if (wallHp <= 0 && !isOpen) return true;
        return false;
    }
}
