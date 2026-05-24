package com.jjk.tick;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TickScheduler {

    private record PlayerTask(Consumer<ServerPlayerEntity> task, int period) {}

    private final List<PlayerTask> playerTasks = new ArrayList<>();

    public void register(Consumer<ServerPlayerEntity> task, int periodTicks) {
        playerTasks.add(new PlayerTask(task, periodTicks));
    }

    public void runPlayerTick(ServerPlayerEntity player, long tickCount) {
        for (PlayerTask entry : playerTasks) {
            if (tickCount % entry.period() == 0) {
                entry.task().accept(player);
            }
        }
    }
}
