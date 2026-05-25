package com.jjk.entity;

import com.jjk.JJKMod;
import com.jjk.api.skill.ISkillSet;
import com.jjk.character.SkillRegistry;
import com.jjk.character.impl.MegumiSkillSet;
import com.jjk.data.PlayerData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.world.World;

import java.util.UUID;

public class ShikigamiEntity extends PathAwareEntity {

    private final UUID ownerUuid;
    private final String shikigamiId;

    public ShikigamiEntity(EntityType<? extends ShikigamiEntity> type, World world,
                            UUID ownerUuid, String shikigamiId) {
        super(type, world);
        this.ownerUuid = ownerUuid;
        this.shikigamiId = shikigamiId;
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!getWorld().isClient) {
            PlayerData data = JJKMod.getPlayerRepository().load(ownerUuid);
            data.deadShikigamiIds.add(shikigamiId);
            JJKMod.getPlayerRepository().saveImmediate(data);
            if ("white_dog".equals(shikigamiId)) {
                ISkillSet skillSet = SkillRegistry.get("megumi");
                if (skillSet instanceof MegumiSkillSet ms) {
                    ms.onWhiteDogDeath();
                }
            }
        }
    }

    @Override
    protected void initGoals() {}

    public UUID getOwnerUuid() { return ownerUuid; }
    public String getShikigamiId() { return shikigamiId; }
}
