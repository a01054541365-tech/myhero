package com.jjk.mixin;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void jjk$onTick(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        long tickCount = player.getWorld().getTime();
        JJKMod.getTickScheduler().runPlayerTick(player, tickCount);
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.fallingBlossomActive && tickCount >= data.fallingBlossomUntil) {
            data.fallingBlossomActive = false;
            JJKMod.getPlayerRepository().save(data);
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void jjk$onDeath(net.minecraft.entity.damage.DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        JJKMod.handlePlayerDeath(player);
    }

    @Inject(method = "getJumpVelocityMultiplier", at = @At("RETURN"), cancellable = true)
    private void jjk_jumpBoost(CallbackInfoReturnable<Float> cir) {
        PlayerData data = JJKMod.getPlayerRepository()
                .load(((ServerPlayerEntity) (Object) this).getUuid());
        if ("itadori".equals(data.characterId)) {
            cir.setReturnValue(cir.getReturnValue() * 1.30f);
        }
    }
}
