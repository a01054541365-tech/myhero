package com.jjk.entity.cursed;

import com.jjk.JJKMod;
import com.jjk.combat.CCManager;
import com.jjk.data.PlayerData;
import com.jjk.entity.CeProjectileEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

// 骨縛 투사체 — 명중 시 BIND 3초(60틱) 적용
public class KotsibakuProjectileEntity extends CeProjectileEntity {

    public KotsibakuProjectileEntity(EntityType<? extends KotsibakuProjectileEntity> type,
                                      World world) {
        super(type, world);
    }

    @Override
    protected void onEntityHit(EntityHitResult hitResult) {
        super.onEntityHit(hitResult); // 데미지 적용 + discard
        if (hitResult.getEntity() instanceof ServerPlayerEntity sp
                && JJKMod.getInstance() != null) {
            long tick = sp.getWorld().getTime();
            PlayerData data = JJKMod.getPlayerRepository().load(sp.getUuid());
            CCManager.tryApplyCC(data, "bind", 60, tick);
            JJKMod.getPlayerRepository().save(data);
        }
    }
}
