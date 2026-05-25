package com.jjk.trial;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrialManager {

    private final JjkConfig config;
    private final Map<UUID, TrialStateMachine> activeTrials = new HashMap<>();
    private final Map<UUID, List<UUID>> evidenceMap = new HashMap<>();

    public TrialManager(JjkConfig config) {
        this.config = config;
    }

    public void addEvidence(ServerPlayerEntity prosecutor, ServerPlayerEntity target) {
        evidenceMap.computeIfAbsent(prosecutor.getUuid(), k -> new ArrayList<>())
                   .add(target.getUuid());
    }

    public void startTrial(ServerPlayerEntity higuruma, ServerPlayerEntity target) {
        UUID trialKey = higuruma.getUuid();
        if (activeTrials.containsKey(trialKey)) return;
        TrialStateMachine sm = new TrialStateMachine(JJKMod.getServer());
        sm.accuse(higuruma.getUuid(), target.getUuid());
        activeTrials.put(trialKey, sm);
    }

    public void tick() {
        activeTrials.values().forEach(TrialStateMachine::tick);
        activeTrials.entrySet().removeIf(e -> e.getValue().getState() == TrialStateMachine.State.END);
    }
}
