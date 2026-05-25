package com.jjk.trial;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TrialStateMachine {

    public enum State {
        IDLE, ACCUSED, DELIBERATION, VERDICT, END
    }

    private State state = State.IDLE;
    private UUID accusedUuid;
    private UUID prosecutorUuid;
    private int stateTicks = 0;
    private boolean guilty;
    private final MinecraftServer server;

    public TrialStateMachine(MinecraftServer server) {
        this.server = server;
    }

    public void tick() {
        stateTicks++;
        switch (state) {
            case ACCUSED -> {
                if (stateTicks >= 20) {
                    state = State.DELIBERATION;
                    stateTicks = 0;
                }
            }
            case DELIBERATION -> {
                if (stateTicks >= 60) {
                    guilty = ThreadLocalRandom.current().nextFloat() < JJKMod.getConfig().trialSuccessRate;
                    state = State.VERDICT;
                    stateTicks = 0;
                }
            }
            case VERDICT -> {
                if (stateTicks >= 1) {
                    if (guilty) {
                        long currentTick = server.getOverworld().getTime();
                        int sealDuration = JJKMod.getConfig().sealDurationTicks;

                        ServerPlayerEntity accusedPlayer = server.getPlayerManager().getPlayer(accusedUuid);
                        if (accusedPlayer != null) {
                            PlayerData accusedData = JJKMod.getPlayerRepository().load(accusedUuid);
                            accusedData.cooldowns.put("skill_seal", currentTick + sealDuration);
                            JJKMod.getPlayerRepository().saveImmediate(accusedData);
                        }

                        ServerPlayerEntity prosecutorPlayer = server.getPlayerManager().getPlayer(prosecutorUuid);
                        if (prosecutorPlayer != null) {
                            PlayerData prosecutorData = JJKMod.getPlayerRepository().load(prosecutorUuid);
                            prosecutorData.hasExecutionSword = true;
                            JJKMod.getPlayerRepository().saveImmediate(prosecutorData);
                        }
                    }
                    state = State.END;
                }
            }
            default -> {}
        }
    }

    public void accuse(UUID prosecutor, UUID accused) {
        if (state != State.IDLE) return;
        this.prosecutorUuid = prosecutor;
        this.accusedUuid = accused;
        this.state = State.ACCUSED;
        this.stateTicks = 0;
    }

    public State getState() { return state; }
    public UUID getAccusedUuid() { return accusedUuid; }
}
