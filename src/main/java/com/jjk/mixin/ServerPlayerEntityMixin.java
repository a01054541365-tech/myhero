package com.jjk.mixin;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.grade.GradeManager;
import net.minecraft.util.math.Vec3d;
import com.jjk.network.s2c.AwakeningS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void jjk$onTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        long tickCount = player.getWorld().getTime();
        JJKMod.getTickScheduler().runPlayerTick(player, tickCount);
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        // 장막 경계 차단
        JJKMod.getCurtainManager().enforceOnPlayer(player);

        // 영창 이동 감지
        if (data.chanting) {
            JJKMod.getChantingHandler().tickMovementCheck(data, player.getUuid(), player.getPos());
            JJKMod.getPlayerRepository().save(data);
        }

        if (data.fallingBlossomActive && tickCount >= data.fallingBlossomUntil) {
            data.fallingBlossomActive = false;
            JJKMod.getPlayerRepository().save(data);
        }
        // 주력해방 만료 (§6-5: 200틱)
        if (data.burstActive && tickCount >= data.burstEndTick) {
            data.burstActive = false;
            ServerPlayNetworking.send(player, new AwakeningS2CPacket(false));
            JJKMod.getPlayerRepository().save(data);
        }
        // 반전술식 자기 치유: 1급 이상 또는 숙련도 7 이상 조건
        if (data.healingActive && ("okkotsu".equals(data.characterId)
                || "itadori".equals(data.characterId))) {
            boolean eligible = GradeManager.Grade.fromLabel(data.grade).rank
                    >= GradeManager.Grade.GRADE_1.rank
                    || data.mastery >= 7;
            if (!eligible) {
                data.healingActive = false;
                JJKMod.getPlayerRepository().save(data);
            } else {
                float healPerTick = JJKMod.getConfig().reverseHealSelfPerTick();   // 0.3
                float ceDrain     = data.ceMax * JJKMod.getConfig().reverseCeDrainSelfRatio(); // × 0.008
                if (!JJKMod.getCEManager().consumeCE(data, ceDrain)) {
                    data.healingActive = false;
                } else {
                    data.hpCurrent = Math.min(data.hpCurrent + healPerTick, data.hpMax);
                }
                JJKMod.getPlayerRepository().save(data);
            }
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void jjk$onDeath(net.minecraft.entity.damage.DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        JJKMod.handlePlayerDeath(player);
    }

}
