package com.jjk.mixin;

import com.jjk.JJKMod;
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
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    private void jjk$onDeath(net.minecraft.entity.damage.DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        JJKMod.getRespawnManager().scheduleRespawn(player);
        JJKMod.getPlayerRepository().evict(player.getUuid());
    }
}
