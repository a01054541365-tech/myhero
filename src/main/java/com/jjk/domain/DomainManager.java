package com.jjk.domain;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public class DomainManager {

    private final JjkConfig config;
    private final Map<UUID, DomainInstance> activeDomains = new HashMap<>();
    private final DomainPriorityCalculator priorityCalc = new DomainPriorityCalculator();

    public DomainManager(JjkConfig config) {
        this.config = config;
    }

    public void tickDomains(ServerPlayerEntity player) {
        Iterator<Map.Entry<UUID, DomainInstance>> it = activeDomains.entrySet().iterator();
        while (it.hasNext()) {
            DomainInstance domain = it.next().getValue();
            domain.ticksAlive++;
            if (domain.isExpired(config.zoneDurationTicks)) {
                it.remove();
                // TODO: send ZoneExitS2CPacket to players inside
            }
        }
    }

    public boolean deployDomain(ServerPlayerEntity owner, String domainId, BlockPos center) {
        // TODO: check CE cost, cooldown, banned chunks
        // TODO: conflict resolution with existing domains
        return false;
    }

    public boolean deployDomain(String domainId, ServerPlayerEntity caster) {
        return deployDomain(caster, domainId, caster.getBlockPos());
    }

    public Optional<DomainInstance> getDomainAt(BlockPos pos) {
        return activeDomains.values().stream()
                .filter(d -> d.center.isWithinDistance(pos, d.currentRadius))
                .findFirst();
    }
}
