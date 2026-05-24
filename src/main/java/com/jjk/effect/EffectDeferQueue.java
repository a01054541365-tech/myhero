package com.jjk.effect;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class EffectDeferQueue {

    private final List<DeferredEffect> queue = new ArrayList<>();

    public void schedule(BlockPos pos, float damage, int delayTicks, UUID attackerUuid) {
        queue.add(new DeferredEffect(pos, damage, delayTicks, attackerUuid));
    }

    public void tick(ServerPlayerEntity ignored) {
        Iterator<DeferredEffect> it = queue.iterator();
        while (it.hasNext()) {
            DeferredEffect effect = it.next();
            effect.ticksRemaining--;
            if (effect.ticksRemaining <= 0) {
                effect.fire();
                it.remove();
            }
        }
    }

    private static class DeferredEffect {
        final BlockPos pos;
        final float damage;
        int ticksRemaining;
        final UUID attackerUuid;

        DeferredEffect(BlockPos pos, float damage, int delayTicks, UUID attackerUuid) {
            this.pos = pos;
            this.damage = damage;
            this.ticksRemaining = delayTicks;
            this.attackerUuid = attackerUuid;
        }

        void fire() {
            // TODO: apply damage to players at pos
        }
    }
}
