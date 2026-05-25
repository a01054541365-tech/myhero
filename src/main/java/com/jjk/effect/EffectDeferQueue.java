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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class EffectDeferQueue {

    private record DeferredEffect(
        long dueTick,
        BlockPos targetPos,
        float damage,
        UUID attackerUuid
    ) {}

    private final List<DeferredEffect> queue =
        Collections.synchronizedList(new ArrayList<>());

    public void schedule(BlockPos pos, float damage,
            int delayTicks, UUID attackerUuid, long currentTick) {
        long dueTick = currentTick + delayTicks;
        queue.add(new DeferredEffect(dueTick, pos, damage, attackerUuid));
    }

    public void tickWorld(long currentTick) {
        if (queue.isEmpty()) return;
        List<DeferredEffect> ready = queue.stream()
            .filter(e -> currentTick >= e.dueTick())
            .collect(Collectors.toList());
        queue.removeAll(ready);
        for (DeferredEffect effect : ready) {
            executeEffect(effect, currentTick);
        }
    }

    private void executeEffect(DeferredEffect effect, long currentTick) {
        MinecraftServer server = JJKMod.getServer();
        if (server == null) return;
        ServerPlayerEntity attacker = server.getPlayerManager()
            .getPlayer(effect.attackerUuid());
        if (attacker == null) return;
        ServerWorld world = attacker.getServerWorld();
        Box box = new Box(effect.targetPos()).expand(3.0);
        List<LivingEntity> targets = world.getEntitiesByClass(
            LivingEntity.class, box,
            e -> e.isAlive()
                && !e.getUuid().equals(effect.attackerUuid()));
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
