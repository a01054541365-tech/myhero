package com.jjk.entity;

import com.jjk.JJKMod;
import com.jjk.api.skill.ISkillSet;
import com.jjk.character.SkillRegistry;
import com.jjk.character.impl.MegumiSkillSet;
import com.jjk.data.PlayerData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class ShikigamiEntity extends PathAwareEntity implements GeoEntity {

    private UUID ownerUuid;
    private String shikigamiId;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ShikigamiEntity(EntityType<? extends ShikigamiEntity> type, World world,
                            UUID ownerUuid, String shikigamiId) {
        super(type, world);
        this.ownerUuid = ownerUuid;
        this.shikigamiId = shikigamiId;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (ownerUuid != null) {
            nbt.putString("OwnerUuid", ownerUuid.toString());
        }
        if (shikigamiId != null) {
            nbt.putString("ShikigamiId", shikigamiId);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("OwnerUuid")) {
            ownerUuid = UUID.fromString(nbt.getString("OwnerUuid"));
        }
        if (nbt.contains("ShikigamiId")) {
            shikigamiId = nbt.getString("ShikigamiId");
        }
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!getWorld().isClient) {
            if (ownerUuid == null) return;
            PlayerData data = JJKMod.getPlayerRepository().load(ownerUuid);
            data.deadShikigamiIds.add(shikigamiId);
            JJKMod.getPlayerRepository().saveImmediate(data);
            if ("white_dog".equals(shikigamiId)) {
                ISkillSet skillSet = SkillRegistry.get("megumi");
                if (skillSet instanceof MegumiSkillSet ms) {
                    ms.onWhiteDogDeath(ownerUuid);
                }
            }
        }
    }

    @Override
    protected void initGoals() {}

    public UUID getOwnerUuid() { return ownerUuid; }
    public String getShikigamiId() { return shikigamiId; }

    // 애니메이션 제어는 클라이언트 렌더러에서 처리 — 서버 엔티티는 no-op
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
