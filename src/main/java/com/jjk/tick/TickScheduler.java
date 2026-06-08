package com.jjk.tick;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TickScheduler {

    private record PlayerTask(Consumer<ServerPlayerEntity> task, int period) {}
    private record ServerTask(Consumer<MinecraftServer> task, int period) {}

    private final List<PlayerTask> playerTasks = new ArrayList<>();
    private final List<ServerTask> serverTasks = new ArrayList<>();

    public void register(Consumer<ServerPlayerEntity> task, int periodTicks) {
        playerTasks.add(new PlayerTask(task, periodTicks));
    }

    public void registerServerTask(Consumer<MinecraftServer> task, int periodTicks) {
        serverTasks.add(new ServerTask(task, periodTicks));
    }

    public void runPlayerTick(ServerPlayerEntity player, long tickCount) {
        for (PlayerTask entry : playerTasks) {
            if (tickCount % entry.period() == 0) {
                entry.task().accept(player);
            }
        }
    }

    public void runServerTick(MinecraftServer server, long tickCount) {
        for (ServerTask entry : serverTasks) {
            if (tickCount % entry.period() == 0) {
                entry.task().accept(server);
            }
        }
    }
}
