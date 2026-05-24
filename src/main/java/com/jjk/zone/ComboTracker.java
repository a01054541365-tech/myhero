package com.jjk.zone;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ComboTracker {

    private static final int JUST_FRAME_MIN = 8;
    private static final int JUST_FRAME_MAX = 12;
    private static final int RESET_TICKS = 30;

    private final Map<UUID, ComboState> states = new HashMap<>();

    public boolean isJustFrame(UUID playerUuid, int ticksSinceLastHit) {
        return ticksSinceLastHit >= JUST_FRAME_MIN && ticksSinceLastHit <= JUST_FRAME_MAX;
    }

    public void recordHit(UUID playerUuid, long currentTick) {
        states.put(playerUuid, new ComboState(currentTick));
    }

    public void tick(UUID playerUuid, long currentTick) {
        ComboState state = states.get(playerUuid);
        if (state != null && currentTick - state.lastHitTick > RESET_TICKS) {
            states.remove(playerUuid);
        }
    }

    private record ComboState(long lastHitTick) {}
}
