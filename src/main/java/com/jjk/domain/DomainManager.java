package com.jjk.domain;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.ZoneExitS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DomainManager {

    private static final Gson GSON = new Gson();
    private static final Set<String> CURSED_SPIRITS =
            Set.of("mahito", "jogo", "hanami", "dagon", "sukuna");
    private static final Set<String> SORCERERS =
            Set.of("gojo", "itadori", "megumi", "okkotsu", "nanami", "inumaki", "hakari", "higuruma");

    private final JjkConfig config;
    private final Map<UUID, DomainInstance> activeDomains = new HashMap<>();
    private final DomainPriorityCalculator priorityCalc = new DomainPriorityCalculator();
    private final Map<String, DomainDefinition> domainDefs = new HashMap<>();

    public DomainManager(JjkConfig config) {
        this.config = config;
        loadDomainDefs();
    }

    private void loadDomainDefs() {
        try (Reader reader = Files.newBufferedReader(Path.of("config/jjk/domains.json"))) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            for (JsonElement elem : root.getAsJsonArray("domains")) {
                DomainDefinition def = GSON.fromJson(elem, DomainDefinition.class);
                domainDefs.put(def.domainId, def);
            }
        } catch (IOException e) {
            // domains.json not found — deployDomain returns false for unknown domainIds
        }
    }

    public void tickDomains(ServerWorld world) {
        long currentTick = world.getTime();
        Iterator<Map.Entry<UUID, DomainInstance>> it = activeDomains.entrySet().iterator();
        while (it.hasNext()) {
            DomainInstance domain = it.next().getValue();
            if (!domain.isExpired(currentTick)) continue;
            it.remove();
            world.getPlayers().stream()
                    .filter(p -> domain.center.isWithinDistance(p.getBlockPos(), domain.currentRadius))
                    .forEach(p -> ServerPlayNetworking.send(p,
                            new ZoneExitS2CPacket(domain.domainId)));
            ServerPlayerEntity owner = world.getServer()
                    .getPlayerManager().getPlayer(domain.ownerUuid);
            if (owner != null) {
                PlayerData ownerData = JJKMod.getPlayerRepository()
                        .load(domain.ownerUuid);
                ownerData.cooldowns.put("skill_seal", currentTick + 120);
                JJKMod.getPlayerRepository().saveImmediate(ownerData);
            }
        }
    }

    public boolean deployDomain(ServerPlayerEntity owner, String domainId, BlockPos center) {
        // === Phase 1: All validations (no side effects) ===

        // Step 1: banned chunk check
        ChunkPos chunkPos = new ChunkPos(center);
        String worldKey = owner.getWorld().getRegistryKey().getValue().toString();
        String chunkKey = worldKey + ":" + chunkPos.x + "," + chunkPos.z;
        if (config.domainBannedChunks.contains(chunkKey)) return false;

        // Step 2: global cap §LOCK 4
        if (activeDomains.size() >= 4) return false;

        // Step 3: team cap §LOCK 2
        String ownerTeam = getTeamName(owner.getUuid());
        long teamCount = activeDomains.values().stream()
                .filter(d -> ownerTeam.equals(getTeamName(d.ownerUuid)))
                .count();
        if (teamCount >= 2) return false;

        // Step 4: cooldown check §LOCK domainCooldownUntil
        PlayerData data = JJKMod.getPlayerRepository().load(owner.getUuid());
        long currentTick = owner.getWorld().getTime();
        if (currentTick < data.domainCooldownUntil) return false;

        // Step 5: domain definition
        DomainDefinition def = domainDefs.get(domainId);
        if (def == null) return false;

        // Step 6: CE check (isOpen ×2)
        float ceCost = def.isOpen ? def.ceCost * 2f : def.ceCost;
        if (data.ceCurrent < ceCost) return false;

        // Step 7: collision check — validate priority before any mutation
        DomainInstance conflictingDomain = null;
        PlayerData conflictingOwnerData = null;
        for (DomainInstance existing : activeDomains.values()) {
            double dist = existing.center.getSquaredDistance(center);
            double combinedRadius = existing.currentRadius + def.radius;
            if (dist > combinedRadius * combinedRadius) continue;
            DomainInstance tempNew = new DomainInstance(
                    owner.getUuid(), domainId, center,
                    def.wallHp, def.radius, def.isOpen, def.isIncomplete,
                    def.sureHitActive, def.autoTargetAll);
            float newPriority = priorityCalc.calculate(tempNew, data);
            PlayerData existOwnerData = JJKMod.getPlayerRepository().load(existing.ownerUuid);
            float existPriority = priorityCalc.calculate(existing, existOwnerData);
            if (newPriority <= existPriority) return false;
            conflictingDomain = existing;
            conflictingOwnerData = existOwnerData;
            break;
        }

        // === Phase 2: Commit (all validations passed — side effects begin here) ===

        // CE deduction
        data.ceCurrent -= ceCost;

        // Remove conflicting domain and notify
        if (conflictingDomain != null) {
            activeDomains.remove(conflictingDomain.instanceId);
            final DomainInstance removed = conflictingDomain;
            owner.getServerWorld().getPlayers().stream()
                    .filter(p -> removed.center.isWithinDistance(p.getBlockPos(), removed.currentRadius))
                    .forEach(p -> ServerPlayNetworking.send(p,
                            new ZoneExitS2CPacket(removed.domainId)));
            conflictingOwnerData.cooldowns.put("skill_seal", currentTick + 120);
            JJKMod.getPlayerRepository().saveImmediate(conflictingOwnerData);
        }

        // Register new domain
        DomainInstance instance = new DomainInstance(
                owner.getUuid(), domainId, center,
                def.wallHp, def.radius, def.isOpen, def.isIncomplete,
                def.sureHitActive, def.autoTargetAll
        );
        instance.expireAtTick = currentTick + def.cooldownTicks;
        activeDomains.put(instance.instanceId, instance);

        // Set cooldown and save
        data.domainCooldownUntil = currentTick + def.cooldownTicks;
        JJKMod.getPlayerRepository().saveImmediate(data);

        return true;
    }

    public boolean deployDomain(String domainId, ServerPlayerEntity caster) {
        return deployDomain(caster, domainId, caster.getBlockPos());
    }

    public Optional<DomainInstance> getDomainAt(BlockPos pos) {
        return activeDomains.values().stream()
                .filter(d -> d.center.isWithinDistance(pos, d.currentRadius))
                .findFirst();
    }

    public boolean hasActiveDomain(UUID ownerUuid) {
        return activeDomains.values().stream()
                .anyMatch(d -> d.ownerUuid.equals(ownerUuid));
    }

    public void clearAll() {
        activeDomains.clear();
    }

    private String getTeamName(UUID uuid) {
        PlayerData d = JJKMod.getPlayerRepository().load(uuid);
        if (d.characterId == null) return "none";
        if (CURSED_SPIRITS.contains(d.characterId)) return "cursed_spirit";
        if (SORCERERS.contains(d.characterId)) return "sorcerer";
        return "none";
    }
}
