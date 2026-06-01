package com.jjk.mixin;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    // 무한(Infinity)이 활성화된 플레이어에게 가해지는 바닐라 피해를 차단.
    // JJK 스킬 피해(CombatPipeline 경유)는 InfinityHandler 에서 별도 처리.
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void jjk$onDamage(DamageSource source, float amount,
                               CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity target)) return;
        if (JJKMod.getInstance() == null) return;

        PlayerData data = JJKMod.getPlayerRepository().load(target.getUuid());
        // 플레이어 공격자가 없는 바닐라 피해(낙하, 불, 환경) 차단
        if (data.infinityActive && !(source.getAttacker() instanceof ServerPlayerEntity)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    // 피격 시 콤보 리셋 + lastDamageTakenTick 갱신
    @Inject(method = "damage", at = @At("TAIL"))
    private void jjk$onDamageTail(DamageSource source, float amount,
                                   CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) return;
        if (JJKMod.getInstance() == null) return;

        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        JJKMod.getComboTracker().onHurt(data);
        data.lastDamageTakenTick = player.getWorld().getTime();
        if (data.chanting) JJKMod.getChantingHandler().cancelChant(data);
        JJKMod.getPlayerRepository().save(data);
    }
}
