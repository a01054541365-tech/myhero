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

    public DomainInstance(UUID ownerUuid, String domainId, BlockPos center,
                          float wallHp, float radius, boolean isOpen, boolean isIncomplete,
                          boolean sureHitActive, boolean autoTargetAll) {
        this.instanceId = UUID.randomUUID();
        this.ownerUuid = ownerUuid;
        this.domainId = domainId;
        this.center = center;
        this.wallHp = wallHp;
        this.maxRadius = radius;
        this.currentRadius = radius;
        this.isOpen = isOpen;
        this.isIncomplete = isIncomplete;
        this.sureHitActive = sureHitActive;
        this.autoTargetAll = autoTargetAll;
        this.ticksAlive = 0;
    }

    public boolean isExpired(long currentTick) {
        if (expireAtTick > 0 && currentTick >= expireAtTick) return true;
        if (wallHp <= 0 && !isOpen) return true;
        return false;
    }
}
