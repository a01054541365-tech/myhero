package com.jjk.combat;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.api.skill.SkillResult;
import com.jjk.chant.ChantingHandler;
import com.jjk.entity.CursedSpiritEntity;
import com.jjk.entity.ShikigamiEntity;
import com.jjk.awakening.AwakeningManager;
import com.jjk.ce.CEManager;
import com.jjk.data.PlayerData;
import com.jjk.defense.DefenseHandler;
import com.jjk.domain.DomainInstance;
import com.jjk.grade.GradeManager;
import com.jjk.item.CursedCrystalItem;
import net.minecraft.item.ItemStack;
import com.jjk.item.CursedToolEffect;
import com.jjk.item.CursedToolRegistry;
import com.jjk.network.s2c.SkillResultS2CPacket;
import com.jjk.team.TeamManager;
import com.jjk.zone.ComboTracker;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import com.jjk.advancement.AdvancementTriggerManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import java.util.Optional;

public class CombatPipeline {

    public enum PipelineResult {
        SUCCESS, SAME_TEAM, CE_INSUFFICIENT, COOLDOWN, OUT_OF_RANGE, INFINITY
    }

    private final DamageCalculator calculator = new DamageCalculator();
    private final TickDamageCap damageCap = new TickDamageCap();
    private final com.jjk.performance.TickBudgetMonitor tickBudget = new com.jjk.performance.TickBudgetMonitor();

    /**
     * §E CombatPipeline 9단계 순수 PlayerData 버전. MC 없이 테스트 가능.
     * S2C 패킷 미전송. checkAndActivate는 이 메서드(5단계)에서만 호출.
     */
    public PipelineResult processData(PlayerData attacker, PlayerData target,
                                       int keyId, long currentTick) {
        JjkConfig config = JJKMod.getInstance() != null ? JJKMod.getConfig() : new JjkConfig();
        CEManager ceManager = JJKMod.getInstance() != null
                ? JJKMod.getCEManager() : new CEManager(config);
        AwakeningManager awakeMgr = JJKMod.getInstance() != null
                ? JJKMod.getAwakeningManager() : new AwakeningManager(config);
        TeamManager teamMgr = JJKMod.getInstance() != null
                ? JJKMod.getTeamManager() : new TeamManager();

        // 1단계: 입력 검증
        if (teamMgr.isSameTeam(attacker, target)) return PipelineResult.SAME_TEAM;

        // 2단계: CE·쿨타임 검증
        float ceCost = TechniqueLoader.getCeCost(attacker.characterId, keyId);
        if (!ceManager.consumeCE(attacker, ceCost)) return PipelineResult.CE_INSUFFICIENT;

        String cooldownKey = String.valueOf(keyId);
        if (attacker.cooldowns.getOrDefault(cooldownKey, 0L) > currentTick) {
            ceManager.refundCE(attacker, ceCost);
            return PipelineResult.COOLDOWN;
        }

        // 3단계: 범위/거리 검증 생략 (PlayerData에 위치 정보 없음)

        // 4단계: baseDamage
        float baseDamage = TechniqueLoader.getBaseDamage(attacker.characterId, keyId);

        // 4.5단계: 영창 배율 (Stage 4 이후, 흑섬 이전)
        if (JJKMod.getInstance() != null && attacker.chanting) {
            ChantingHandler chanter = JJKMod.getChantingHandler();
            int chantTicks = chanter.getChantTicks(attacker, currentTick);
            float chantMult = chanter.getChantMultiplier(chantTicks);
            float extraCe = ceCost * chanter.getChantCeDrainRatio(chantTicks);
            if (attacker.ceCurrent >= extraCe) {
                attacker.ceCurrent -= extraCe;
            } else {
                chantMult = 1.0f;
            }
            baseDamage *= chantMult;
            chanter.cancelChant(attacker);
        }

        // 5단계: 흑섬·Zone·각성 — checkAndActivate는 이 메서드(5·9단계)에서만 호출
        // 구현 C-2: 흑섬 연속 발동 쿨다운 — 쿨다운 중이면 확률 계산 자체를 건너뛴다
        boolean justFrame = currentTick >= attacker.blackFlashCooldownUntil
                && ComboTracker.checkBlackFlash(attacker.lastAttackTick, currentTick);
        boolean isBlackFlash = justFrame
                && ComboTracker.rollBlackFlash(attacker, attacker.hpCurrent, attacker.hpMax);
        // §LOCK base × 2.5: calculatePure에서 ctx.isBlackFlash 플래그로 적용 (이중 계산 방지)
        if (isBlackFlash) {
            attacker.blackFlashCooldownUntil = currentTick + config.blackFlashCooldownTicks();
            if (attacker.uuid != null) {
                BlackFlashPerfectTracker.record(attacker.uuid, currentTick);
            }
        }

        awakeMgr.checkAndActivate(attacker, attacker.hpCurrent, attacker.hpMax, currentTick);

        // 6단계: Infinity 판정 생략 (MC 컨텍스트 없음)

        // 7단계: 최종 배율 clamp (DamageCalculator 내 §LOCK ×0.25~×4.0)
        DamageContext ctx;
        if (isBlackFlash) {
            // J-2-2: 흑섬 입력 매크로 누적 5회 시 Perfect 판정 완전 차단(일반 흑섬으로 강등)
            BlackFlashHandler bfHandler = JJKMod.getBlackFlashHandler();
            boolean perfect = !(bfHandler != null && attacker.uuid != null
                    && bfHandler.isDowngraded(attacker.uuid));
            ctx = DamageContext.builder(null, null,
                    com.jjk.api.combat.IDamageSource.BLACK_FLASH, baseDamage)
                    .blackFlash(perfect).build();
        } else {
            ctx = DamageContext.builder(null, null,
                    com.jjk.api.combat.IDamageSource.NORMAL_TECHNIQUE, baseDamage)
                    .build();
        }

        float finalDamage = calculator.calculatePure(ctx, attacker, target, baseDamage, config, currentTick);

        // 8단계: TickDamageCap — §LOCK: config.pvpDamageCapMaxHpRatio (0.40f)
        finalDamage = TickDamageCap.apply(finalDamage, target.hpMax, config.pvpDamageCapMaxHpRatio);

        // 9단계: 반영
        target.hpCurrent = Math.max(0f, target.hpCurrent - finalDamage);
        awakeMgr.checkAndActivate(target, target.hpCurrent, target.hpMax, currentTick);

        long cooldownTicks = TechniqueLoader.getCooldownTicks(attacker.characterId, keyId);
        attacker.cooldowns.put(cooldownKey, currentTick + cooldownTicks);
        attacker.lastCombatTick = currentTick;
        target.lastCombatTick = currentTick;
        attacker.lastAttackTick = currentTick;

        return PipelineResult.SUCCESS;
    }

    public void process(DamageContext ctx) {
        tickBudget.startTick();
        try {
            processInternal(ctx);
        } finally {
            tickBudget.usedMs("CombatPipeline.process");
        }
    }

    private void processInternal(DamageContext ctx) {
        // §E 1단계: 입력 검증 — 팀 검증 및 characterId 소유 확인
        if (ctx.attacker != null && ctx.target instanceof ServerPlayerEntity tgt) {
            PlayerData atkData = JJKMod.getPlayerRepository().load(ctx.attacker.getUuid());
            PlayerData tgtData = JJKMod.getPlayerRepository().load(tgt.getUuid());
            if (JJKMod.getTeamManager().isSameTeam(atkData, tgtData)) return;
            if (atkData.characterId == null) return;
        }

        // §E 2단계: CE·쿨타임 검증 — alreadyConsumedCE=false인 경우에만 실행
        // alreadyConsumedCE 기본값=true → 기존 SkillSet 호출부는 영향 없음
        if (!ctx.alreadyConsumedCE && ctx.attacker != null) {
            long t2 = ctx.attacker.getWorld().getTime();
            PlayerData atkData2 = JJKMod.getPlayerRepository().load(ctx.attacker.getUuid());
            String skillCdKey = "skill_" + ctx.keyId;
            if (!CooldownManager.isReady(atkData2, skillCdKey, t2)) {
                if (ctx.attacker instanceof ServerPlayerEntity ap) {
                    ServerPlayNetworking.send(ap,
                        new SkillResultS2CPacket(ctx.keyId, SkillResult.ON_COOLDOWN.name(), 0f));
                }
                return;
            }
            if (ctx.ceCost > 0 && !JJKMod.getCEManager().consumeCE(atkData2, ctx.ceCost)) {
                if (ctx.attacker instanceof ServerPlayerEntity ap) {
                    ServerPlayNetworking.send(ap,
                        new SkillResultS2CPacket(ctx.keyId, SkillResult.CE_INSUFFICIENT.name(), 0f));
                }
                return;
            }
            CooldownManager.set(atkData2, skillCdKey, t2, ctx.cooldownTicks);
            JJKMod.getPlayerRepository().save(atkData2);
        }

        // Stage 1: validate context
        if (ctx.target == null) return;

        // Stage 2: check immunity (infinity)
        if (ctx.target instanceof ServerPlayerEntity targetPlayer) {
            PlayerData targetData2 = JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
            if (targetData2.infinityActive) {
                boolean nullified = false;
                // 조건 1: isSoulDirect=true
                if (ctx.isSoulDirect) nullified = true;
                // 조건 2: isBlackFlash=true (흑섬 직격)
                if (ctx.isBlackFlash) nullified = true;
                // 조건 3: nKeyApplied=true (영역전연)
                if (ctx.nKeyApplied) nullified = true;
                // 조건 4: 공격자가 필중 활성 영역 내부에 있음 (startup 20틱 후)
                if (ctx.attacker != null) {
                    long domainTick = ctx.attacker.getWorld().getTime();
                    Optional<DomainInstance> domain =
                        JJKMod.getDomainManager()
                            .getDomainAt(ctx.attacker.getBlockPos());
                    if (domain.isPresent() && domain.get().isSureHitReady(domainTick)) nullified = true;
                }
                if (!nullified) return;
            }
        }

        // Stage 2b: 도메인 필중 → nKeyApplied 세팅 (simpleBarrier/fallingBlossom 트리거용)
        if (!ctx.nKeyApplied && ctx.attacker != null) {
            long domainSetTick = ctx.attacker.getWorld().getTime();
            Optional<DomainInstance> attackerDomain =
                JJKMod.getDomainManager().getDomainAt(ctx.attacker.getBlockPos());
            if (attackerDomain.isPresent() && attackerDomain.get().isSureHitReady(domainSetTick)) {
                ctx.nKeyApplied = true;
            }
        }
        // 영역전연 활성 수비자: 필중 속성 무효화
        if (ctx.nKeyApplied && ctx.target instanceof ServerPlayerEntity tgtAmp) {
            PlayerData tgtAmpData = JJKMod.getPlayerRepository().load(tgtAmp.getUuid());
            if (tgtAmpData.domainAmplificationActive) ctx.nKeyApplied = false;
        }

        // Stage 3: calculate base damage
        float damage = calculator.calculate(ctx);
        if (damage <= 0f) return;

        // Stage 3.5: 영창 배율 (baseDamage 이후, Zone 이전)
        if (ctx.attacker != null) {
            PlayerData atkChantData = JJKMod.getPlayerRepository().load(ctx.attacker.getUuid());
            if (atkChantData.chanting) {
                long chantTick = ctx.attacker.getWorld().getTime();
                ChantingHandler chanter = JJKMod.getChantingHandler();
                int chantTicks = chanter.getChantTicks(atkChantData, chantTick);
                float chantMult = chanter.getChantMultiplier(chantTicks);
                float extraCe = ctx.ceCost * chanter.getChantCeDrainRatio(chantTicks);
                if (atkChantData.ceCurrent >= extraCe) {
                    atkChantData.ceCurrent -= extraCe;
                } else {
                    chantMult = 1.0f;
                }
                damage *= chantMult;
                chanter.cancelChant(atkChantData);
                JJKMod.getPlayerRepository().save(atkChantData);
            }
        }

        // Stage 4b: 주구(呪具) 효과 적용 — 공격력, CE 환급, 범위, 방어 관통, 흑섬, isSoulDirect
        if (ctx.attacker != null) {
            PlayerData toolAtkData = JJKMod.getPlayerRepository().load(ctx.attacker.getUuid());
            CursedToolEffect tool = CursedToolRegistry.getActiveEffect(ctx.attacker, toolAtkData);
            if (tool != CursedToolEffect.NONE) {
                damage *= (1.0f + tool.attackBonus());
                if (tool.ceReduction() > 0 && ctx.ceCost > 0) {
                    float refund = ctx.ceCost * tool.ceReduction();
                    toolAtkData.ceCurrent = Math.min(toolAtkData.ceCurrent + refund, toolAtkData.ceMax);
                    JJKMod.getPlayerRepository().save(toolAtkData);
                }
                if (tool.rangeBonus() > 0) {
                    ctx.rangeMultiplier = 1.0f + tool.rangeBonus();
                }
                if (tool.defPenetration() > 0) {
                    ctx.defenseMultiplier = Math.max(0f, ctx.defenseMultiplier - tool.defPenetration());
                }
                if (tool.blackFlashBonus() > 0) {
                    ctx.extraBlackFlashBonus += tool.blackFlashBonus();
                }
                if (tool.forceSoulDirect()) {
                    ctx.isSoulDirect = true;
                }
                // 천역모: 술식 무효화 — 대상 쿨타임 연장
                if (tool.sealCooldownTicks() > 0
                        && ctx.target instanceof ServerPlayerEntity sealTarget) {
                    long sealTick = ctx.attacker.getWorld().getTime();
                    String sealKey = "inverted_spear_used";
                    long lastUsed = toolAtkData.cooldowns.getOrDefault(sealKey, 0L);
                    if (sealTick - lastUsed >= tool.sealCooldownTicks()) {
                        PlayerData sealTgtData =
                            JJKMod.getPlayerRepository().load(sealTarget.getUuid());
                        sealTgtData.cooldowns.replaceAll(
                            (k, v) -> v + Math.max(0L, v - sealTick));
                        toolAtkData.cooldowns.put(sealKey, sealTick);
                        JJKMod.getPlayerRepository().save(sealTgtData);
                        JJKMod.getPlayerRepository().save(toolAtkData);
                    }
                }
            }
        }

        // Stage 4: zone bonus / penalty  (attacker-only; null-safe)
        if (ctx.attacker != null) {
            PlayerData attackerData = JJKMod.getPlayerRepository().load(ctx.attacker.getUuid());
            long currentTick = ctx.attacker.getWorld().getTime();
            if (attackerData.zoneActive && currentTick < attackerData.zoneEndTick) {
                JjkConfig cfg = JJKMod.getConfig();
                // 공격력 보너스 +15% (zoneAtkBonus)
                damage *= (1.0f + cfg.blackFlashZoneAtkBonus() / 100.0f);
                // 흑섬 발동 스킬에 추가 스킬 데미지 보너스 +10% (zoneSkillBonus)
                if (ctx.isBlackFlash) {
                    damage *= (1.0f + cfg.blackFlashZoneSkillBonus() / 100.0f);
                }
            }
            if (currentTick < attackerData.zonePenaltyUntilTick) {
                damage *= 0.5f;
            }

            // Stage 5: grade scaling (§LOCK multipliers — see Grade.multiplier)
            float gradeMultiplier = attackerData.grade != null ? attackerData.grade.multiplier : 1.00f;
            damage *= gradeMultiplier;
        }

        // 속박 +30%: 대상이 status_bind 상태이면 데미지 보너스
        if (ctx.target instanceof ServerPlayerEntity boundTarget) {
            PlayerData boundData = JJKMod.getPlayerRepository().load(boundTarget.getUuid());
            long bindTick = ctx.target.getWorld().getTime();
            if (boundData.cooldowns.getOrDefault("status_bind", 0L) > bindTick) {
                damage *= 1.30f;
            }
        }

        // Stage 5: check awakening trigger on target after absorbing damage (§3-7)
        if (ctx.target instanceof ServerPlayerEntity targetPlayer) {
            JJKMod.getAwakeningManager().checkAndActivate(targetPlayer);
        }

        // Stage 6: RCT reduction
        PlayerData targetData = JJKMod.getPlayerRepository().load(ctx.target.getUuid());
        if (targetData.healingActive && !ctx.bypassRCT) {
            damage *= 0.85f;
        }

        // Stage 6b: 방어 시스템 3종 (DefenseHandler)
        damage = DefenseHandler.applySimpleBarrier(targetData, ctx, damage);
        damage = DefenseHandler.applyFallingBlossom(targetData, ctx, damage);
        damage = DefenseHandler.applyShield(targetData, damage);
        if (damage <= 0f) return;

        // Stage 6c: 기본 방어 스탯 감쇠 (비율 감쇠)
        // 몹이 아닌 플레이어 대상에만 적용. isSoulDirect=true(수파 등)는 applyDefenseStat 내부에서 통과.
        if (ctx.target instanceof ServerPlayerEntity) {
            damage = applyDefenseStat(damage, targetData.defenseStat, ctx.defenseMultiplier, ctx.isSoulDirect);
        }

        // Stage 7: §LOCK PvP cap max_hp×0.40 (player targets only)
        if (ctx.target instanceof ServerPlayerEntity p) {
            damage = damageCap.apply(damage, p);
            if (damage <= 0f) return;
        }

        // Stage 7b: 잭팟 불사 처리 (하카리)
        if ("hakari".equals(targetData.characterId) && targetData.jackpotActive) {
            float overflow = damage - targetData.hpCurrent + 1f;
            if (overflow > 0f) {
                damage = Math.max(0f, targetData.hpCurrent - 1f);
                targetData.ceCurrent -= overflow * 2.0f;
                if (targetData.ceCurrent <= 0f) {
                    targetData.ceCurrent  = 0f;
                    targetData.jackpotActive = false;
                    targetData.jackpotEndTick = 0L;
                }
            }
            targetData.awakeningActive = false;
        }

        // Stage 7c: 비술사 불굴 (피해 ×0.70 + 1회 사망 방지)
        long shieldTick = ctx.target.getWorld().getTime();
        if (shieldTick <= targetData.nsShieldExpireTick) {
            damage *= 0.70f;
            if (!targetData.nsDeathPreventUsed && damage >= ctx.target.getHealth()) {
                damage = ctx.target.getHealth() - 1.0f;
                targetData.nsDeathPreventUsed = true;
                JJKMod.getPlayerRepository().save(targetData);
            }
        }

        // Stage 8: apply damage to target
        DamageSource source = ctx.attacker != null
                ? ctx.target.getDamageSources().playerAttack(ctx.attacker)
                : ctx.target.getDamageSources().magic();
        ctx.target.damage(source, damage);

        // P2-1: 속박 파훼 체크 — 선언자가 대상에게 피해를 입힌 경우
        if (ctx.attacker != null) {
            JJKMod.getBindingVowSystem().checkParry(
                    ctx.attacker.getUuid(), ctx.target.getUuid(),
                    ctx.attacker.getWorld().getTime());
        }

        // Stage 8b: 마허라가 피격 카운트 (ShikigamiEntity "mahoraga" 대상)
        if (ctx.target instanceof ShikigamiEntity shikigami
                && "mahoraga".equals(shikigami.getShikigamiId())
                && shikigami.getOwnerUuid() != null) {
            PlayerData megumiData = JJKMod.getPlayerRepository().load(shikigami.getOwnerUuid());
            long count = megumiData.cooldowns.getOrDefault("mahoraga_hit_count", 0L);
            megumiData.cooldowns.put("mahoraga_hit_count", count + 1);
            JJKMod.getPlayerRepository().save(megumiData);
        }

        // §13-1: 피격 스킬 정보 기록 — 옷코츠 복사 술식 지원
        // baseDamage > 0이고 skillName이 있는 전투 스킬만 기록
        if (ctx.baseDamage > 0 && ctx.skillName != null
                && ctx.target instanceof ServerPlayerEntity) {
            targetData.lastReceivedSkillId       = ctx.skillName;
            targetData.lastReceivedBaseDamage    = (int) ctx.baseDamage;
            targetData.lastReceivedCeCost        = ctx.ceCost;
            targetData.lastReceivedCooldownTicks = ctx.cooldownTicks;
            targetData.lastReceivedIsDomain      = ctx.isDomainSkill;
            JJKMod.getPlayerRepository().save(targetData);
        }

        // Stage 9: audit log + notify attacker
        String attackerStr = ctx.attacker != null ? ctx.attacker.getUuid().toString() : "environment";
        if (JJKMod.getAuditLogger() != null) {
            JJKMod.getAuditLogger().logEvent("DAMAGE", ctx.target.getUuid(),
                    String.format("attacker=%s dmg=%.1f src=%s", attackerStr, damage, ctx.sourceType), 0L);
        }
        if (ctx.attacker instanceof ServerPlayerEntity attackerPlayer) {
            String resultStr = ctx.isBlackFlash
                    ? (ctx.blackFlashPerfect ? "BLACK_FLASH_PERFECT" : "BLACK_FLASH_GREAT")
                    : "HIT";
            ServerPlayNetworking.send(attackerPlayer,
                    new SkillResultS2CPacket(ctx.keyId, resultStr, damage));

            // P2-2: RTT 급등 후 흑섬 Perfect 감지 — §100ms 클램프
            if (ctx.isBlackFlash) {
                int currentRtt = attackerPlayer.networkHandler.getLatency();
                @SuppressWarnings("unused")
                long compensatedMs = Math.min(currentRtt / 2L, 100L);
                AntiAbuseManager aam = JJKMod.getAntiAbuseManager();
                if (aam != null && ctx.blackFlashPerfect
                        && aam.checkRttSpike(attackerPlayer.getUuid(), currentRtt)) {
                    aam.flag(attackerPlayer.getUuid(), "black_flash_rtt_spike", 1);
                }
            }

            // Stage 9b: 콤보 onHit 갱신
            if (ctx.attacker != null) {
                PlayerData atkData = JJKMod.getPlayerRepository().load(ctx.attacker.getUuid());
                long tick = ctx.attacker.getWorld().getTime();
                JJKMod.getComboTracker().onHit(atkData, ctx.target.getUuid(), tick);
                JJKMod.getPlayerRepository().save(atkData);
            }

            // Stage 9c: XP 이벤트
            if (JJKMod.getInstance() != null && ctx.attacker != null) {
                GradeManager gm = JJKMod.getGradeManager();
                PlayerData atkXpData = JJKMod.getPlayerRepository().load(ctx.attacker.getUuid());
                long xpTick = ctx.attacker.getWorld().getTime();
                int defGradeRank = targetData.grade != null ? targetData.grade.ordinal() : 0;
                gm.onDamageHit(atkXpData, attackerPlayer, xpTick, defGradeRank);
                if (ctx.isBlackFlash) {
                    gm.onBlackFlash(atkXpData, attackerPlayer);
                    AdvancementTriggerManager.onBlackFlash(attackerPlayer, ctx.blackFlashPerfect);
                    if (JJKMod.getZoneStateManager() != null) {
                        JJKMod.getZoneStateManager().onBlackFlash(attackerPlayer, xpTick);
                    }
                }
                if (ctx.target.getHealth() <= 0f) {
                    gm.onKill(atkXpData, targetData, attackerPlayer, xpTick);

                    // Stage 9d: 주력석 지급
                    if (JJKMod.getCursedStoneManager() != null) {
                        long stonesReward = 0L;
                        if (ctx.target instanceof ServerPlayerEntity) {
                            stonesReward = 30L;
                            if (TeamManager.getTeam(atkXpData.characterId) == TeamManager.Team.CURSED_SPIRIT
                                    && TeamManager.getTeam(targetData.characterId) == TeamManager.Team.JUJUTSU_SORCERER) {
                                JJKMod.getCursedStoneManager().addBounty(atkXpData, 50L);
                            }
                        } else if (ctx.target instanceof CursedSpiritEntity spirit) {
                            if (JJKMod.getAchievementManager() != null) {
                                JJKMod.getAchievementManager().unlock(attackerPlayer, "first_kill");
                            }
                            stonesReward = switch (spirit.getGrade()) {
                                case GRADE_4      -> 40L;
                                case GRADE_3      -> 60L;
                                case GRADE_2      -> 80L;
                                case GRADE_1      -> 100L;
                                case SEMI_SPECIAL -> 150L;
                                case SPECIAL      -> 200L;
                            };
                            // C-5: 주령 처치 퀘스트 진행 (kill_spirit)
                            if (JJKMod.getQuestManager() != null) {
                                JJKMod.getQuestManager().progress(
                                    atkXpData, "kill_spirit", attackerPlayer, xpTick);
                            }
                            // 특급·준특급 처치 업적
                            if (stonesReward >= 150L) {
                                AdvancementTriggerManager.onSpecialGradeKill(attackerPlayer);
                                if ("inumaki".equals(atkXpData.characterId)) {
                                    AdvancementTriggerManager.onInumakiKillSpecialGrade(attackerPlayer);
                                }
                            }
                        }
                        if (stonesReward > 0L) {
                            JJKMod.getCursedStoneManager()
                                .give(atkXpData, stonesReward, "combat_kill", attackerPlayer);
                        }
                        if (ctx.isBlackFlash) {
                            JJKMod.getCursedStoneManager()
                                .give(atkXpData, 10L, "black_flash", attackerPlayer);
                            // P3-1: 흑섬 결정체 드롭 — Perfect +3개, Great +1개
                            if (CursedCrystalItem.INSTANCE != null) {
                                int crystals = ctx.blackFlashPerfect ? 3 : 1;
                                ItemStack crystal = new ItemStack(CursedCrystalItem.INSTANCE, crystals);
                                if (!attackerPlayer.getInventory().insertStack(crystal)) {
                                    attackerPlayer.dropItem(crystal, false);
                                }
                            }
                        }
                        if (atkXpData.lastDamageTakenTick < atkXpData.lastAttackTick
                                && atkXpData.lastAttackTick > 0) {
                            JJKMod.getCursedStoneManager()
                                .give(atkXpData, 20L, "perfect_kill", attackerPlayer);
                        }
                    }

                    // Stage 9b: 스쿠나 플레이어 사망 시 손가락 드롭 판정
                    if (ctx.target instanceof ServerPlayerEntity deadPlayer) {
                        PlayerData deadData = JJKMod.getPlayerRepository().load(deadPlayer.getUuid());
                        if ("sukuna".equals(deadData.characterId)) {
                            JJKMod.getFingerSystem().tryDropFromKill(
                                deadPlayer.getUuid(),
                                ctx.attacker.getUuid(),
                                "player_sukuna"
                            );
                        }
                    }
                }
            }
        }
    }

    /**
     * 비율 감쇠 방어 공식: damage × (100 / (100 + effectiveDefense))
     * isSoulDirect=true → 방어 무시, damage 그대로 반환.
     * 테스트 접근용으로 public. 호출 위치: process() Stage 6c 전용.
     */
    public static float applyDefenseStat(float damage, int defenseStat,
                                          float defenseMultiplier, boolean isSoulDirect) {
        if (isSoulDirect) return damage;
        float effectiveDefense = defenseStat * defenseMultiplier;
        return damage * (100f / (100f + effectiveDefense));
    }

    /**
     * 영역 DoT 데미지 적용 — CE/쿨타임 검증 없이 방어·PvP 캡만 적용.
     * defenseMultiplier: 1.0 = 일반, 0.70 = slash (방어 30% 관통)
     */
    public void applyDomainDamage(ServerPlayerEntity owner, LivingEntity target,
                                   float rawDamage, float defenseMultiplier,
                                   ServerWorld world) {
        if (owner == null || target == null) return;
        if (owner.getUuid().equals(target.getUuid())) return;

        float damage = rawDamage;
        if (target instanceof ServerPlayerEntity targetPlayer) {
            PlayerData targetData = JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
            float effectiveDefense = targetData.defenseStat * defenseMultiplier;
            damage = Math.max(0f, damage - effectiveDefense);
            damage = TickDamageCap.apply(damage, targetData.hpMax,
                    JJKMod.getConfig().pvpDamageCapMaxHpRatio);
        }
        if (damage <= 0f) return;

        DamageSource src = target.getDamageSources().playerAttack(owner);
        target.damage(src, damage);
    }
}
