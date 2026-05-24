package com.jjk.mixin;

import com.jjk.JJKMod;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void jjk$onWorldTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        JJKMod.getRespawnManager().tick();
        JJKMod.getTrialManager().tick();
    }
}
