package com.jjk.finger;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.concurrent.atomic.AtomicBoolean;

public class FingerSystem {

    private final JjkConfig config;
    private final AtomicBoolean fingerLock = new AtomicBoolean(false);

    public FingerSystem(JjkConfig config) {
        this.config = config;
    }

    public boolean tryDrop(ServerPlayerEntity victim) {
        if (!fingerLock.compareAndSet(false, true)) return false;
        try {
            if (Math.random() >= config.fingerDropRate) return false;
            PlayerData data = JJKMod.getPlayerRepository().load(getItadoriUuid());
            if (data == null || data.fingerCount >= config.fingerMaxCount) return false;
            data.fingerCount++;
            JJKMod.getPlayerRepository().save(data);
            return true;
        } finally {
            fingerLock.set(false);
        }
    }

    private java.util.UUID getItadoriUuid() {
        // TODO: look up the current Itadori player UUID
        return null;
    }
}
