package com.jjk.chant;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChantingHandler {

    private final Map<UUID, Vec3d> previousPositions = new ConcurrentHashMap<>();

    public void startChant(PlayerData data, long currentTick) {
        if (data.chanting) return;
        data.chanting = true;
        data.chantStartTick = currentTick;
    }

    public void cancelChant(PlayerData data) {
        data.chanting = false;
        data.chantStartTick = 0;
    }

    public int getChantTicks(PlayerData data, long currentTick) {
        if (!data.chanting || data.chantStartTick == 0) return 0;
        int ticks = (int)(currentTick - data.chantStartTick);
        return Math.min(ticks, JJKMod.getConfig().chantMaxTicks());
    }

    public float getChantMultiplier(int chantTicks) {
        if (chantTicks <= 10) return 1.0f;
        if (chantTicks <= 20) return 1.2f;
        if (chantTicks <= 40) return 1.5f;
        return JJKMod.getConfig().chantMaxMultiplier(); // 2.0
    }

    public float getChantCeDrainRatio(int chantTicks) {
        if (chantTicks <= 0)  return 0.0f;
        if (chantTicks <= 10) return 0.01f;
        if (chantTicks <= 20) return 0.05f;
        if (chantTicks <= 40) return 0.10f;
        return JJKMod.getConfig().chantCeDrainRatio(); // 0.20
    }

    /** 이동 감지: 수평 이동 0.05블록 초과 시 영창 해제. */
    public void tickMovementCheck(PlayerData data, UUID uuid, Vec3d currentPos) {
        if (!data.chanting) {
            previousPositions.remove(uuid);
            return;
        }
        Vec3d prev = previousPositions.get(uuid);
        if (prev != null) {
            double dx = Math.abs(currentPos.x - prev.x);
            double dz = Math.abs(currentPos.z - prev.z);
            if (dx > 0.05 || dz > 0.05) {
                cancelChant(data);
                previousPositions.remove(uuid);
                return;
            }
        }
        previousPositions.put(uuid, currentPos);
    }
}
