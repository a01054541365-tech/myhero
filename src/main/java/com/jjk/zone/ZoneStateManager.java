package com.jjk.zone;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.domain.DomainInstance;
import com.jjk.network.s2c.ZoneEnterS2CPacket;
import com.jjk.network.s2c.ZoneExitS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ZoneStateManager {

    private final Set<UUID> playersInZone = new HashSet<>();
    private final Map<UUID, String> activeDomainIds = new HashMap<>();
    private final Map<UUID, List<Long>> recentBlackFlashTicks = new HashMap<>();

    public void tick(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Optional<DomainInstance> domain = JJKMod.getDomainManager()
                .getDomainAt(player.getBlockPos());
        boolean insideNow = domain.isPresent();

        if (insideNow && !playersInZone.contains(uuid)) {
            playersInZone.add(uuid);
            activeDomainIds.put(uuid, domain.get().domainId);
            onEnter(player, domain.get());
        } else if (!insideNow && playersInZone.contains(uuid)) {
            String domainId = activeDomainIds.remove(uuid);
            playersInZone.remove(uuid);
            onExit(player, domainId != null ? domainId : "");
        }
    }

    public boolean isInZone(UUID uuid) {
        return playersInZone.contains(uuid);
    }

    // §3-3 갱신: 흑섬 1회 즉시 존 진입. config.zoneStackable=false → 활성 중 연장 없음.
    // 호출자: CombatPipeline 5단계에서 isBlackFlash=true 확정 후 호출.
    public void onBlackFlash(ServerPlayerEntity player, long currentTick) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.zoneActive) return;  // zoneStackable=false: 이미 활성이면 무시
        int duration = JJKMod.getInstance() != null
                ? JJKMod.getConfig().zoneDurationTicks : 300;
        data.zoneActive = true;
        data.zoneEndTick = currentTick + duration;
        data.zoneEntryTick = currentTick;
        JJKMod.getPlayerRepository().save(data);
        ServerPlayNetworking.send(player, new ZoneEnterS2CPacket("black_flash_zone"));

        if (JJKMod.getAchievementManager() != null) {
            JJKMod.getAchievementManager().unlock(player, "zone_entry");
            checkTripleBlackFlash(player, currentTick);
        }
    }

    private void checkTripleBlackFlash(ServerPlayerEntity player, long currentTick) {
        List<Long> ticks = recentBlackFlashTicks.computeIfAbsent(player.getUuid(), k -> new ArrayList<>());
        ticks.add(currentTick);
        ticks.removeIf(t -> currentTick - t > 200L);
        if (ticks.size() >= 3) {
            JJKMod.getAchievementManager().unlock(player, "triple_black_flash");
        }
    }

    // ─── Pure-data methods (CombatPipeline.processData 및 death reset용) ──────

    public void enterZone(PlayerData data, long tick) {
        int duration = (com.jjk.JJKMod.getInstance() != null)
                ? com.jjk.JJKMod.getConfig().zoneDurationTicks : 300; // decisions §2-3
        data.zoneActive = true;
        data.zoneEndTick = tick + duration;
        data.zoneEntryTick = tick;
    }

    public void exitZone(PlayerData data, long tick) {
        data.zoneActive = false;
        data.zoneEndTick = 0;
        data.zonePenaltyUntilTick = tick + 100;
    }

    public void reset(PlayerData data) {
        data.zoneActive = false;
        data.zoneEndTick = 0;
        data.zonePenaltyUntilTick = 0;
        data.zoneEntryTick = -1L;
    }

    // §3-3: set zoneActive/zoneEndTick, send ZoneEnterS2CPacket
    private void onEnter(ServerPlayerEntity player, DomainInstance domain) {
        long currentTick = player.getWorld().getTime();
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        data.zoneActive = true;
        data.zoneEndTick = domain.expireAtTick > 0
                ? domain.expireAtTick
                : currentTick + JJKMod.getConfig().zoneDurationTicks;
        JJKMod.getPlayerRepository().save(data);
        ServerPlayNetworking.send(player, new ZoneEnterS2CPacket(domain.domainId));
    }

    // §3-3: clear zone state, set zonePenaltyUntilTick (×0.5 damage penalty window),
    //        send ZoneExitS2CPacket
    private void onExit(ServerPlayerEntity player, String domainId) {
        long currentTick = player.getWorld().getTime();
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        data.zoneActive = false;
        data.zoneEndTick = 0;
        data.zonePenaltyUntilTick = currentTick + JJKMod.getConfig().zoneDurationTicks;
        JJKMod.getPlayerRepository().save(data);
        ServerPlayNetworking.send(player, new ZoneExitS2CPacket(domainId));
    }
}
