package com.jjk.entity.capture;

import com.jjk.JJKMod;
import com.jjk.advancement.AdvancementTriggerManager;
import com.jjk.data.PlayerData;
import com.jjk.entity.CursedSpiritEntity;
import com.jjk.grade.GradeManager;
import com.jjk.item.CursedCrystalItem;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public final class CursedSpiritCaptureSystem {

    private CursedSpiritCaptureSystem() {}

    /**
     * 포획 시도 진입점 — CaptureAttemptC2SPacket.handle()에서 서버 스레드로 호출.
     * 조건:
     *   1. 플레이어가 간이영역(SimpleDomain) 활성 상태
     *   2. 주령 엔티티 HP가 최대 HP의 10% 이하
     *   3. 대상이 CursedSpiritEntity
     */
    public static void attemptCapture(ServerPlayerEntity player, int targetEntityId) {
        if (JJKMod.getInstance() == null) return;
        if (!(player.getWorld() instanceof ServerWorld sw)) return;

        Entity target = sw.getEntityById(targetEntityId);
        if (!(target instanceof CursedSpiritEntity spirit)) {
            player.sendMessage(Text.literal("§c[포획] 대상이 주령 엔티티가 아닙니다."), false);
            return;
        }

        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());

        // 조건 1: 간이영역 활성
        if (!data.simpleBarrierActive) {
            player.sendMessage(Text.literal("§c[포획] 간이영역이 활성화되어 있어야 합니다."), false);
            return;
        }

        // 조건 2: 주령 HP 10% 이하
        if (spirit.getHealth() > spirit.getMaxHealth() * 0.10f) {
            player.sendMessage(Text.literal("§c[포획] 주령의 체력이 10% 이하여야 합니다."), false);
            return;
        }

        // 성공 확률 계산
        int gradeRank = GradeManager.Grade.fromLabel(data.grade).rank;
        int mastery = data.mastery;
        int chance = gradeRank * 15 + mastery * 5;  // %
        chance = Math.max(5, Math.min(95, chance));

        float roll = sw.getRandom().nextFloat() * 100f;
        if (roll < chance) {
            onCaptureSuccess(player, spirit, data, sw);
        } else {
            onCaptureFail(player, spirit);
        }
    }

    private static void onCaptureSuccess(ServerPlayerEntity player, CursedSpiritEntity spirit,
                                          PlayerData data, ServerWorld sw) {
        // CE 결정체 1~3개 드롭
        int crystalCount = 1 + sw.getRandom().nextBetween(0, 2);
        if (CursedCrystalItem.INSTANCE != null) {
            ItemStack crystals = new ItemStack(CursedCrystalItem.INSTANCE, crystalCount);
            player.getInventory().insertStack(crystals);
            if (!crystals.isEmpty()) player.dropItem(crystals, false);
        }

        data.capturedSpiritCount++;
        JJKMod.getPlayerRepository().saveImmediate(data);

        AdvancementTriggerManager.onCaptureSpirit(player, data.capturedSpiritCount);
        spirit.discard();

        player.sendMessage(
            Text.literal("§a[포획] 주령 포획 성공! CE 결정체 " + crystalCount + "개 획득."), false);
    }

    private static void onCaptureFail(ServerPlayerEntity player, CursedSpiritEntity spirit) {
        // 실패: 주령 HP 5 회복 + 분노 (10초 = 200틱 공격력 1.5배)
        spirit.setHealth(Math.min(spirit.getHealth() + 5f, spirit.getMaxHealth()));
        // 분노 상태: cooldowns map에 rage_bonus_until 저장 (CombatPipeline에서 참조 가능)
        // CursedSpiritEntity 자체는 rage 플래그를 내부적으로 관리
        spirit.setEnraged(200);

        player.sendMessage(Text.literal("§c[포획] 포획 실패. 주령이 분노했습니다!"), false);
    }
}
