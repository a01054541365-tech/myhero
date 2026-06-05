package com.jjk.economy;

import com.jjk.JJKMod;
import com.jjk.audit.AuditLogger;
import com.jjk.data.PlayerData;
import com.jjk.data.PlayerRepository;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class CursedStoneManager {

    private final PlayerRepository repository;

    public CursedStoneManager(PlayerRepository repository) {
        this.repository = repository;
    }

    /** 주력석 지급. 음수 불가. */
    public void give(PlayerData data, long amount,
                     String reason, ServerPlayerEntity player) {
        if (amount <= 0) return;
        data.cursedStones += amount;
        repository.save(data);
        if (JJKMod.getInstance() != null) {
            AuditLogger auditLogger = JJKMod.getAuditLogger();
            if (auditLogger != null) {
                auditLogger.logEvent("stones_give", data.uuid,
                    String.format("{\"amount\":%d,\"reason\":\"%s\"}", amount, reason), 0L);
            }
        }
        if (player != null) {
            player.sendMessage(
                Text.literal("§e+주력석 " + amount + " (" + reason + ")"), true);
        }
    }

    /** 주력석 차감. 잔액 부족 시 false 반환. */
    public boolean spend(PlayerData data, long amount,
                         String reason, ServerPlayerEntity player) {
        if (data.cursedStones < amount) return false;
        data.cursedStones -= amount;
        repository.save(data);
        if (JJKMod.getInstance() != null) {
            AuditLogger auditLogger = JJKMod.getAuditLogger();
            if (auditLogger != null) {
                auditLogger.logEvent("stones_spend", data.uuid,
                    String.format("{\"amount\":%d,\"reason\":\"%s\"}", amount, reason), 0L);
            }
        }
        return true;
    }

    /** 현상금 누적 (주령 진영 플레이어가 주술사 처치 시). */
    public void addBounty(PlayerData target, long amount) {
        target.bounty += amount;
        repository.save(target);
    }

    /** 현상금 정산 → 주력석으로 변환. */
    public long settleBounty(PlayerData data, ServerPlayerEntity player) {
        long settled = data.bounty;
        if (settled <= 0) return 0;
        data.bounty = 0;
        data.cursedStones += settled;
        repository.saveImmediate(data);
        if (JJKMod.getInstance() != null) {
            AuditLogger auditLogger = JJKMod.getAuditLogger();
            if (auditLogger != null) {
                auditLogger.logEvent("bounty_settled", data.uuid,
                    String.format("{\"amount\":%d}", settled), 0L);
            }
        }
        return settled;
    }
}
