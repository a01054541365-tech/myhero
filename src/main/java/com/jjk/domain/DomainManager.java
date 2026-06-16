package com.jjk.domain;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jjk.JJKMod;
import com.jjk.advancement.AdvancementTriggerManager;
import com.jjk.JjkConfig;
import com.jjk.api.combat.IDamageSource;
import com.jjk.combat.DamageContext;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.DomainDeployFailS2CPacket;
import com.jjk.network.s2c.ZoneExitS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DomainManager {

    private static final Gson GSON = new Gson();
    private static final Set<String> CURSED_SPIRITS =
            Set.of("mahito", "jogo", "hanami", "dagon", "sukuna", "choso");
    private static final Set<String> SORCERERS =
            Set.of("gojo", "itadori", "megumi", "okkotsu", "nanami", "inumaki", "hakari", "higuruma");

    private final JjkConfig config;
    private final Map<UUID, DomainInstance> activeDomains = new HashMap<>();
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

    /** /jj reload — domains.json 정의를 다시 읽는다 (활성 영역은 유지). */
    public void reloadDefs() {
        domainDefs.clear();
        loadDomainDefs();
    }

    public void tickDomains(ServerWorld world) {
        long currentTick = world.getTime();

        // 매 틱 효과 처리 — autoTargetAll(마히토) + sukuna_malevolent_shrine(스쿠나)
        for (DomainInstance domain : new ArrayList<>(activeDomains.values())) {
            if (domain.npcOwned) continue; // NPC 영역은 DomainDeployGoal.tick()이 직접 처리
            if ("sukuna_malevolent_shrine".equals(domain.domainId)) {
                // 복마어주자: 짝수 틱 = 절단(slash), 홀수 틱 = 화살(arrow)
                ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(domain.ownerUuid);
                if (owner == null) { collapseDomain(domain.ownerUuid, currentTick); continue; }
                boolean isSlash = (currentTick % 2 == 0);
                float rawDamage = isSlash ? 15f : 10f;
                float defenseMultiplier = isSlash ? 0.70f : 1.0f; // slash: 방어 30% 관통
                for (ServerPlayerEntity p : world.getPlayers()) {
                    if (p.getUuid().equals(domain.ownerUuid)) continue;
                    if (!domain.center.isWithinDistance(p.getBlockPos(), domain.currentRadius)) continue;
                    JJKMod.getCombatPipeline().applyDomainDamage(owner, p, rawDamage, defenseMultiplier, world);
                }
            } else if ("okkotsu_true_mutual_love".equals(domain.domainId)) {
                // 진판상애절단: 전개 20틱 후 복사 술식 필중 발동 → 즉시 종료
                long elapsed = currentTick - domain.deployedAtTick;
                if (elapsed < 20) continue;

                ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(domain.ownerUuid);
                if (owner == null) { collapseDomain(domain.ownerUuid, currentTick); continue; }

                PlayerData ownerData = JJKMod.getPlayerRepository().load(domain.ownerUuid);
                if (ownerData.lastReceivedSkillId != null) {
                    for (ServerPlayerEntity p : world.getPlayers()) {
                        if (p.getUuid().equals(domain.ownerUuid)) continue;
                        if (!domain.center.isWithinDistance(p.getBlockPos(), domain.currentRadius)) continue;
                        DamageContext ctx = DamageContext.builder(owner, p,
                                        IDamageSource.NORMAL_TECHNIQUE,
                                        (float) ownerData.lastReceivedBaseDamage)
                                .keyId(3)
                                .skillName(ownerData.lastReceivedSkillId)
                                .build();
                        JJKMod.getCombatPipeline().process(ctx);
                    }
                    ownerData.lastReceivedSkillId = null;
                    JJKMod.getPlayerRepository().save(ownerData);
                }

                // ZoneExit 전송 후 영역 즉시 종료
                world.getPlayers().stream()
                        .filter(p -> domain.center.isWithinDistance(p.getBlockPos(), domain.currentRadius))
                        .forEach(p -> ServerPlayNetworking.send(p, new ZoneExitS2CPacket(domain.domainId)));
                collapseDomain(domain.ownerUuid, currentTick);

            } else if ("gojo_unlimited_void".equals(domain.domainId) && domain.sureHitActive) {
                // 무량공처: 내부 플레이어(소유자 제외) 매 틱 CE 강제 소진, 고갈 시 1초 기절
                for (ServerPlayerEntity p : world.getPlayers()) {
                    if (p.getUuid().equals(domain.ownerUuid)) continue;
                    if (!domain.center.isWithinDistance(p.getBlockPos(), domain.currentRadius)) continue;
                    PlayerData pData = JJKMod.getPlayerRepository().load(p.getUuid());
                    float beforeCe = pData.ceCurrent;
                    pData.ceCurrent -= pData.ceMax * config.unlimitedVoidCeDrainRatio();
                    pData.ceCurrent = Math.max(0f, pData.ceCurrent);
                    if (pData.ceCurrent <= 0f) {
                        pData.cooldowns.put("status_stun", currentTick + 20L);
                    }
                    if (pData.ceCurrent != beforeCe) {
                        JJKMod.getPlayerRepository().save(pData);
                    }
                }
            } else if (domain.autoTargetAll) {
                // autoTargetAll 매 틱 영혼 데미지 (마히토 자폐원돈과)
                ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(domain.ownerUuid);
                if (owner == null) { collapseDomain(domain.ownerUuid, currentTick); continue; }
                PlayerData ownerData = JJKMod.getPlayerRepository().load(domain.ownerUuid);
                if (ownerData.hpCurrent <= ownerData.hpMax * 0.50f
                        || currentTick - domain.deployedAtTick >= 200) {
                    collapseDomain(domain.ownerUuid, currentTick);
                    continue;
                }
                float baseDot = 10f;
                for (ServerPlayerEntity p : world.getPlayers()) {
                    if (!domain.center.isWithinDistance(p.getBlockPos(), domain.currentRadius)) continue;
                    PlayerData pData = JJKMod.getPlayerRepository().load(p.getUuid());
                    float dmg = p.getUuid().equals(domain.ownerUuid)
                            ? baseDot * (1f - domain.ownerDamageReduction)
                            : baseDot;
                    pData.hpCurrent = Math.max(0f, pData.hpCurrent - dmg);
                    JJKMod.getPlayerRepository().save(pData);
                }
            }
        }

        Iterator<Map.Entry<UUID, DomainInstance>> it = activeDomains.entrySet().iterator();
        while (it.hasNext()) {
            DomainInstance domain = it.next().getValue();
            if (!domain.isExpired(currentTick)) continue;
            it.remove();
            // 만료 영역 블록 복구 예약
            com.jjk.domain.DomainBlockQueue bq0 = JJKMod.getDomainBlockQueue();
            if (bq0 != null) {
                bq0.enqueueRestore(domain.instanceId.toString(), world);
            }
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

        // Step 0: 비술사(천여주박)는 영역 전개 불가
        PlayerData ownerData0 = JJKMod.getPlayerRepository().load(owner.getUuid());
        if ("todo".equals(ownerData0.characterId)) return false;

        // Step 0b: 이미 본인 영역 활성 중이면 중복 전개 불가
        if (hasActiveDomain(owner.getUuid())) {
            ServerPlayNetworking.send(owner, new DomainDeployFailS2CPacket(DomainDeployFailS2CPacket.FailReason.DOMAIN_ALREADY_ACTIVE));
            return false;
        }

        // Step 1: banned chunk check
        ChunkPos chunkPos = new ChunkPos(center);
        String worldKey = owner.getWorld().getRegistryKey().getValue().toString();
        String chunkKey = worldKey + ":" + chunkPos.x + "," + chunkPos.z;
        if (config.domainBannedChunks.contains(chunkKey)) {
            ServerPlayNetworking.send(owner, new DomainDeployFailS2CPacket(DomainDeployFailS2CPacket.FailReason.BANNED_CHUNK));
            return false;
        }

        // Step 2: global cap §LOCK 4
        if (activeDomains.size() >= 4) return false;

        // Step 3: team cap §LOCK 2
        // applyTeamLimit — DomainManagerTest 검증 대상. DomainInstance 분리 미결정.
        String ownerTeam = getTeamName(owner.getUuid());
        long teamCount = activeDomains.values().stream()
                .filter(d -> ownerTeam.equals(getTeamName(d.ownerUuid)))
                .count();
        if (teamCount >= 2) {
            ServerPlayNetworking.send(owner, new DomainDeployFailS2CPacket(DomainDeployFailS2CPacket.FailReason.DOMAIN_ALREADY_ACTIVE));
            return false;
        }

        // Step 4: cooldown check §LOCK domainCooldownUntil
        PlayerData data = JJKMod.getPlayerRepository().load(owner.getUuid());
        long currentTick = owner.getWorld().getTime();
        if (currentTick < data.domainCooldownUntil) {
            ServerPlayNetworking.send(owner, new DomainDeployFailS2CPacket(DomainDeployFailS2CPacket.FailReason.ON_COOLDOWN));
            return false;
        }

        // Step 5: domain definition
        DomainDefinition def = domainDefs.get(domainId);
        if (def == null) return false;

        // Step 6: CE check (isOpen ×2)
        float ceCost = def.isOpen ? def.ceCost * 2f : def.ceCost;
        if (data.ceCurrent < ceCost) {
            ServerPlayNetworking.send(owner, new DomainDeployFailS2CPacket(DomainDeployFailS2CPacket.FailReason.CE_INSUFFICIENT));
            return false;
        }

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
            float newPriority = DomainPriorityCalculator.calculate(data, tempNew);
            PlayerData existOwnerData = JJKMod.getPlayerRepository().load(existing.ownerUuid);
            float existPriority = DomainPriorityCalculator.calculate(existOwnerData, existing);
            // §8-2 동점: UUID hashCode로 결정적 처리
            if (Math.abs(newPriority - existPriority) < 0.001f) {
                if (owner.getUuid().hashCode() <= existing.ownerUuid.hashCode()) return false;
            } else if (newPriority < existPriority) {
                return false;
            }
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
            // 충돌 패배 영역 블록 복구
            com.jjk.domain.DomainBlockQueue bq = JJKMod.getDomainBlockQueue();
            if (bq != null) {
                bq.enqueueRestore(removed.instanceId.toString(), owner.getServerWorld());
            }
            owner.getServerWorld().getPlayers().stream()
                    .filter(p -> removed.center.isWithinDistance(p.getBlockPos(), removed.currentRadius))
                    .forEach(p -> ServerPlayNetworking.send(p,
                            new ZoneExitS2CPacket(removed.domainId)));
            conflictingOwnerData.cooldowns.put("skill_seal", currentTick + 120);
            JJKMod.getPlayerRepository().saveImmediate(conflictingOwnerData);
            AdvancementTriggerManager.onDomainCollisionWin(owner);
        }

        // Register new domain
        // restoreRadius — DomainManagerTest 검증 대상. DomainInstance 분리 미결정.
        DomainInstance instance = new DomainInstance(
                owner.getUuid(), domainId, center,
                def.wallHp, def.radius, def.isOpen, def.isIncomplete,
                def.sureHitActive, def.autoTargetAll
        );
        instance.expireAtTick = currentTick + def.cooldownTicks;
        instance.ownerDamageReduction = def.ownerDamageReduction;
        instance.deployedAtTick = currentTick;
        instance.worldKey = owner.getWorld().getRegistryKey().getValue().toString();
        activeDomains.put(instance.instanceId, instance);
        // 구체 블록 생성 예약 (개방형은 SphereBuilder 내부에서 즉시 반환)
        com.jjk.domain.DomainBlockQueue blockQueue = JJKMod.getDomainBlockQueue();
        if (blockQueue != null) {
            SphereBuilder.enqueueSphere(instance, owner.getServerWorld(), blockQueue, def.blockTheme);
        }
        AdvancementTriggerManager.onDomainDeploy(owner);
        if (JJKMod.getAchievementManager() != null) {
            JJKMod.getAchievementManager().unlock(owner, "first_domain");
        }

        // Set cooldown and save
        data.domainCooldownUntil = currentTick + def.cooldownTicks;
        JJKMod.getPlayerRepository().saveImmediate(data);

        return true;
    }

    public boolean deployDomain(String domainId, ServerPlayerEntity caster) {
        return deployDomain(caster, domainId, caster.getBlockPos());
    }

    /**
     * 순수 PlayerData 경로 — MC 없이 테스트 가능.
     * CE 차감, domainCooldownUntil 세팅, DomainInstance 등록.
     * S2C 패킷·AuditLogger·saveImmediate 없음 (MC 의존 제거).
     */
    public boolean deployDomainData(PlayerData attacker, DomainDefinition def, long tick) {
        if ("todo".equals(attacker.characterId)) return false; // 비술사는 영역 전개 불가
        if (tick < attacker.domainCooldownUntil) return false;
        if (def == null) return false;
        float ceCost = def.isOpen ? def.ceCost * 2f : def.ceCost; // decisions §3-4
        if (attacker.ceCurrent < ceCost) return false;

        attacker.ceCurrent -= ceCost;
        DomainInstance inst = new DomainInstance(
                attacker.uuid, def.domainId != null ? def.domainId : "test",
                BlockPos.ORIGIN, def.wallHp, def.radius,
                def.isOpen, def.isIncomplete, def.sureHitActive, def.autoTargetAll);
        inst.team = com.jjk.team.TeamManager.getTeam(attacker.characterId);
        inst.ownerDamageReduction = def.ownerDamageReduction;
        inst.deployedAtTick = tick;
        activeDomains.put(inst.instanceId, inst);
        attacker.domainCooldownUntil = tick + def.cooldownTicks;
        return true;
    }

    /** §8-4: 개방형 시전자만 결계형 wallHp에 데미지 가능. */
    public void applyWallDamage(UUID attackerUuid, UUID domainOwnerUuid, float damage, long currentTick) {
        DomainInstance domain = activeDomains.values().stream()
                .filter(d -> d.ownerUuid.equals(domainOwnerUuid) && !d.isOpen)
                .findFirst().orElse(null);
        if (domain == null) return;
        // 개방형 시전자만 공격 가능 체크
        DomainInstance attackerDomain = activeDomains.values().stream()
                .filter(d -> d.ownerUuid.equals(attackerUuid) && d.isOpen)
                .findFirst().orElse(null);
        if (attackerDomain == null) return;
        domain.wallHp -= damage;
        if (domain.wallHp <= 0) collapseDomain(domainOwnerUuid, currentTick);
    }

    /** 영역 강제 종료. 같은 팀 영역이 1개로 줄면 restoreRadius() 호출. */
    public void collapseDomain(UUID ownerUuid, long tick) {
        DomainInstance removed = null;
        for (var it = activeDomains.entrySet().iterator(); it.hasNext();) {
            DomainInstance d = it.next().getValue();
            if (d.ownerUuid.equals(ownerUuid)) { removed = d; it.remove(); break; }
        }
        if (removed == null) return;
        // 종료된 영역 블록 복구 예약
        com.jjk.domain.DomainBlockQueue bq = JJKMod.getDomainBlockQueue();
        if (bq != null && removed.worldKey != null && JJKMod.getServer() != null) {
            ServerWorld restoreWorld = null;
            for (ServerWorld w : JJKMod.getServer().getWorlds()) {
                if (w.getRegistryKey().getValue().toString().equals(removed.worldKey)) {
                    restoreWorld = w;
                    break;
                }
            }
            if (restoreWorld != null) {
                bq.enqueueRestore(removed.instanceId.toString(), restoreWorld);
            }
        }
        com.jjk.team.TeamManager.Team team = removed.team;
        long remaining = activeDomains.values().stream().filter(d -> team == d.team).count();
        if (remaining == 1) {
            activeDomains.values().stream()
                    .filter(d -> team == d.team).findFirst()
                    .ifPresent(DomainInstance::restoreRadius);
        }
    }

    public Map<UUID, DomainInstance> getActiveDomains() { return activeDomains; }

    public Optional<DomainInstance> getDomainAt(BlockPos pos) {
        return activeDomains.values().stream()
                .filter(d -> d.center.isWithinDistance(pos, d.currentRadius))
                .findFirst();
    }

    public boolean hasActiveDomain(UUID ownerUuid) {
        return activeDomains.values().stream()
                .anyMatch(d -> d.ownerUuid.equals(ownerUuid));
    }

    /** NPC 포함 모든 소유자의 활성 영역 반환 (null = 없음). */
    public DomainInstance getActiveDomain(UUID ownerUuid) {
        return activeDomains.values().stream()
            .filter(d -> d.ownerUuid.equals(ownerUuid))
            .findFirst().orElse(null);
    }

    /**
     * NPC(주령 등) 전용 영역 전개.
     * PlayerData 없이 UUID + 위치 기준으로 DomainInstance 생성.
     * CE 소모 없음 — 주령은 CE 시스템 외부.
     */
    public boolean deployNpcDomain(UUID npcUuid, Vec3d center,
                                    String domainId, long currentTick, String worldKey) {
        // 이미 전개 중이면 중복 방지
        boolean alreadyActive = activeDomains.values().stream()
            .anyMatch(d -> d.ownerUuid.equals(npcUuid));
        if (alreadyActive) return false;

        DomainDefinition def = domainDefs.get(domainId);
        if (def == null) return false;

        // 금지 청크 체크 (center가 null이면 ORIGIN 사용)
        BlockPos blockCenter = center != null ? BlockPos.ofFloored(center) : BlockPos.ORIGIN;
        ChunkPos chunk = new ChunkPos(blockCenter);
        String chunkKey = worldKey + ":" + chunk.x + "," + chunk.z;
        if (config.domainBannedChunks.contains(chunkKey)) return false;

        DomainInstance instance = new DomainInstance(
            npcUuid, domainId, blockCenter,
            def.wallHp, def.radius, def.isOpen,
            false, def.sureHitActive, def.autoTargetAll);
        instance.npcOwned = true;
        instance.expireAtTick = currentTick + def.cooldownTicks;
        instance.deployedAtTick = currentTick;
        instance.worldKey = worldKey;
        activeDomains.put(instance.instanceId, instance);
        // 구체 블록 생성 예약 (개방형은 SphereBuilder 내부에서 즉시 반환)
        com.jjk.domain.DomainBlockQueue npcBq = JJKMod.getDomainBlockQueue();
        if (npcBq != null && JJKMod.getServer() != null) {
            ServerWorld npcWorld = null;
            for (ServerWorld w : JJKMod.getServer().getWorlds()) {
                if (w.getRegistryKey().getValue().toString().equals(worldKey)) {
                    npcWorld = w;
                    break;
                }
            }
            if (npcWorld != null) {
                SphereBuilder.enqueueSphere(instance, npcWorld, npcBq, def.blockTheme);
            }
        }

        if (JJKMod.getInstance() != null && JJKMod.getAuditLogger() != null) {
            JJKMod.getAuditLogger().logEvent("domain_start", npcUuid,
                String.format("{\"domainId\":\"%s\",\"type\":\"npc\"}", domainId), currentTick);
        }
        return true;
    }

    /** 테스트용: DomainDefinition을 직접 등록. */
    public void addDomainDefForTest(String id, DomainDefinition def) {
        domainDefs.put(id, def);
    }

    public void clearAll() {
        activeDomains.clear();
    }

    /** 스폰 검증용: 해당 위치가 활성 영역 내부인지 확인. */
    public boolean isInsideAnyDomain(Vec3d pos) {
        BlockPos bp = BlockPos.ofFloored(pos);
        for (DomainInstance domain : activeDomains.values()) {
            if (domain.center.isWithinDistance(bp, domain.currentRadius)) return true;
        }
        return false;
    }

    private String getTeamName(UUID uuid) {
        PlayerData d = JJKMod.getPlayerRepository().load(uuid);
        if (d.characterId == null) return "none";
        if (CURSED_SPIRITS.contains(d.characterId)) return "cursed_spirit";
        if (SORCERERS.contains(d.characterId)) return "sorcerer";
        return "none";
    }
}
