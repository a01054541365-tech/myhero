package com.jjk.finger;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
            addFinger(data, victim.getServer());
            return true;
        } finally {
            fingerLock.set(false);
        }
    }

    public void addFinger(PlayerData data, MinecraftServer server) {
        if (data.fingerCount >= config.fingerMaxCount) return;
        data.fingerCount++;
        if (data.fingerCount >= config.fingerMaxCount) {
            server.getPlayerManager().broadcast(
                Text.literal("[JJK] 료멘 스쿠나 완전부활!"), false);
            data.ceMax += 2000f;
            data.ceCurrent = data.ceMax;
            JJKMod.getPlayerRepository().saveImmediate(data);
        } else {
            JJKMod.getPlayerRepository().save(data);
        }
    }

    public float getFingerCeBonus(int count) {
        if (count >= 20) return 2000f;
        if (count >= 16) return 1400f;
        if (count >= 11) return 1000f;
        if (count >= 6)  return 600f;
        if (count >= 1)  return 300f;
        return 0f;
    }

    public float getFingerAtkBonus(int count) {
        if (count >= 20) return 0.30f;
        if (count >= 16) return 0.20f;
        if (count >= 11) return 0.15f;
        if (count >= 6)  return 0.10f;
        if (count >= 1)  return 0.05f;
        return 0f;
    }

    public float getFingerSkillDmgBonus(int count) {
        if (count >= 20) return 0.25f;
        if (count >= 16) return 0.15f;
        if (count >= 11) return 0.10f;
        if (count >= 6)  return 0.06f;
        if (count >= 1)  return 0.03f;
        return 0f;
    }

    private java.util.UUID getItadoriUuid() {
        // TODO: look up the current Itadori player UUID
        return null;
    }
}
