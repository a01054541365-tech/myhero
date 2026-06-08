package com.jjk.combat;

import com.jjk.JJKMod;
import com.jjk.advancement.AdvancementTriggerManager;
import com.jjk.data.PlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 속박의 서약 파훼·타임아웃 처리.
 * 선언 및 bindingVowDeclaredTick 세팅은 호출자(스킬셋·커맨드) 책임.
 * 대상(targetId)은 비영속 in-memory Map에 보관 (속박은 단기 상태이므로 재시작 시 자동 해제).
 */
public class BindingVowSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-vow");
    private static final int PARRY_WINDOW_TICKS = 200;  // 10초 — 파훼 판정 창

    // 선언자 → 대상 (비영속, 서버 재시작 시 소멸)
    private final ConcurrentHashMap<UUID, UUID> vowTargets = new ConcurrentHashMap<>();

    /**
     * 속박 선언 등록. PlayerData.bindingVowDeclaredTick 세팅 + save는 호출자가 처리.
     */
    public void declareVow(UUID declarerId, UUID targetId) {
        vowTargets.put(declarerId, targetId);
    }

    /**
     * 속박 선언 + 양방향 패널티용 skillId 기록.
     * pendingBindingVowSkillId 미선언 시 vowSkillUsedThisVow 체크 생략.
     */
    public void declareVow(UUID declarerId, UUID targetId, String skillId, long currentTick) {
        vowTargets.put(declarerId, targetId);
        PlayerData data = JJKMod.getPlayerRepository().load(declarerId);
        data.pendingBindingVowSkillId   = skillId;
        data.pendingBindingVowStartTick = currentTick;
        data.vowSkillUsedThisVow        = false;
        JJKMod.getPlayerRepository().save(data);
    }

    /**
     * 서약한 스킬 사용 완료 시 호출. pendingBindingVowSkillId 초기화.
     */
    public void onVowSkillUsed(UUID playerId) {
        PlayerData data = JJKMod.getPlayerRepository().load(playerId);
        if (data.pendingBindingVowSkillId != null) {
            data.pendingBindingVowSkillId = null;
            data.vowSkillUsedThisVow      = true;
            JJKMod.getPlayerRepository().save(data);
        }
    }

    /**
     * 선언자(attackerId)가 대상(targetId)에게 피해를 입혔을 때 파훼 체크.
     * CombatPipeline.process() 내 Stage 8 (damage 적용) 이후에 호출.
     * 파훼 성공 시: CE 20% 역반동 + 속박 해제.
     */
    public void checkParry(UUID attackerId, UUID targetId, long currentTick) {
        PlayerData data = JJKMod.getPlayerRepository().load(attackerId);
        if (data.bindingVowDeclaredTick < 0) return;
        if (currentTick > data.bindingVowDeclaredTick + PARRY_WINDOW_TICKS) return;

        UUID storedTarget = vowTargets.get(attackerId);
        if (storedTarget == null || !storedTarget.equals(targetId)) return;

        float penalty = data.ceMax * 0.20f;
        data.ceCurrent = Math.max(0f, data.ceCurrent - penalty);
        data.bindingVowDeclaredTick = -1L;
        vowTargets.remove(attackerId);
        JJKMod.getPlayerRepository().save(data);
        LOGGER.info("[JJK] 속박 파훼 성공. attacker={} target={}", attackerId, targetId);
        MinecraftServer server = JJKMod.getServer();
        if (server != null) {
            ServerPlayerEntity attackerPlayer = server.getPlayerManager().getPlayer(attackerId);
            if (attackerPlayer != null) AdvancementTriggerManager.onBindingVowBreak(attackerPlayer);
        }
    }

    /**
     * 속박 타임아웃 체크 (TickScheduler per-player 1틱 주기 호출).
     * PARRY_WINDOW_TICKS 초과 후 미파훼: CE 30% 소모 + skill_seal 300틱.
     * G-4: pendingBindingVowSkillId 타임아웃 → 서약 스킬 봉인.
     */
    public void tickPlayer(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long currentTick = player.getWorld().getTime();

        // 기존 파훼 타임아웃 체크
        if (data.bindingVowDeclaredTick >= 0) {
            if (currentTick > data.bindingVowDeclaredTick + PARRY_WINDOW_TICKS) {
                data.ceCurrent *= 0.70f;
                CooldownManager.set(data, "skill_seal", currentTick, 300);
                data.bindingVowDeclaredTick = -1L;
                vowTargets.remove(player.getUuid());
                JJKMod.getPlayerRepository().save(data);
                player.sendMessage(Text.literal("[JJK] 속박 서약 시간 초과. CE 30% 소모 및 스킬 봉인."), false);
                LOGGER.info("[JJK] 속박 타임아웃. player={}", player.getUuid());
            }
        }

        // G-4: 양방향 패널티 — 서약 스킬 미사용 시 봉인
        if (data.pendingBindingVowSkillId != null) {
            long elapsed = currentTick - data.pendingBindingVowStartTick;
            long timeout = JJKMod.getConfig().bindingVowTimeoutTicks; // §LOCK 300
            if (elapsed > timeout && !data.vowSkillUsedThisVow) {
                CooldownManager.set(data, "skill_seal", currentTick, (int) timeout);
                data.pendingBindingVowSkillId = null;
                JJKMod.getPlayerRepository().save(data);
                player.sendMessage(
                    Text.literal("§c[속박] 서약을 이행하지 못했습니다. 술식이 봉인됩니다."), false);
                LOGGER.info("[JJK] 속박 양방향 패널티 발동. player={}", player.getUuid());
            }
        }
    }

    public void removeVow(UUID playerId) {
        vowTargets.remove(playerId);
    }
}
