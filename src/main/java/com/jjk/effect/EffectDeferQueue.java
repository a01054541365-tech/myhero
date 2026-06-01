package com.jjk.effect;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.combat.DamageContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

public class EffectDeferQueue {

    private record DeferredEffect(
        long dueTick,
        BlockPos targetPos,
        float damage,
        UUID attackerUuid
    ) {}

    // dueTick 오름차순. 서버 메인 스레드에서만 접근하므로 synchronized 불필요.
    private final PriorityQueue<DeferredEffect> queue =
            new PriorityQueue<>(Comparator.comparingLong(DeferredEffect::dueTick));

    public void schedule(BlockPos pos, float damage,
            int delayTicks, UUID attackerUuid, long currentTick) {
        queue.add(new DeferredEffect(currentTick + delayTicks, pos, damage, attackerUuid));
    }

    public void tickWorld(long currentTick) {
        while (!queue.isEmpty() && queue.peek().dueTick() <= currentTick) {
            executeEffect(queue.poll(), currentTick);
        }
    }

    private void executeEffect(DeferredEffect effect, long currentTick) {
        MinecraftServer server = JJKMod.getServer();
        if (server == null) return;
        ServerPlayerEntity attacker = server.getPlayerManager().getPlayer(effect.attackerUuid());
        if (attacker == null) return;
        ServerWorld world = attacker.getServerWorld();
        Box box = new Box(effect.targetPos()).expand(3.0);
        List<LivingEntity> targets = world.getEntitiesByClass(
            LivingEntity.class, box,
            e -> e.isAlive() && !e.getUuid().equals(effect.attackerUuid()));
        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(
                attacker, target,
                IDamageSource.NORMAL_TECHNIQUE, effect.damage())
                .skillName("deferred")
                .build();
            JJKMod.getCombatPipeline().process(ctx);
        }
    }
}
