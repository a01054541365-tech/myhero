package com.jjk.trial;

import java.util.UUID;

public class TrialStateMachine {

    public enum State {
        IDLE, ACCUSED, DELIBERATION, VERDICT, END
    }

    private State state = State.IDLE;
    private UUID accusedUuid;
    private UUID prosecutorUuid;
    private int stateTicks = 0;

    public void tick() {
        stateTicks++;
        switch (state) {
            case ACCUSED -> {
                // TODO: transition to DELIBERATION after delay
            }
            case DELIBERATION -> {
                // TODO: run success rate roll, transition to VERDICT
            }
            case VERDICT -> {
                // TODO: apply verdict effect (strip technique, executioner sword), transition to END
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
