package com.jjk.entity.cursed;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.item.CECrystalItem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

// 죠고 (Jogo) — 특급 주령 NPC. 스킬 스텁 (추후 SkillSet 연동 예정)
public class JogoNpcEntity extends HostileEntity {

    public JogoNpcEntity(EntityType<? extends JogoNpcEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     500.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,   25.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,   0.32)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,    30.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 1.0));
        goalSelector.add(9, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!(getWorld() instanceof ServerWorld sw)) return;
        if (!(source.getAttacker() instanceof ServerPlayerEntity killer)) return;

        // 스쿠나 손가락 드롭 (config.fingerDropRate)
        float fingerRate = JJKMod.getInstance() != null
            ? JJKMod.getConfig().fingerDropRate : 0.10f;
        if (sw.getRandom().nextFloat() < fingerRate && JJKMod.getInstance() != null) {
            PlayerData data = JJKMod.getPlayerRepository().load(killer.getUuid());
            int max = JJKMod.getConfig().fingerMaxCount;
            if (data.fingerCount < max) {
                data.fingerCount++;
                JJKMod.getPlayerRepository().saveImmediate(data);
                int count = data.fingerCount;
                killer.sendMessage(Text.literal(
                    "[JJK] 스쿠나의 손가락을 획득했습니다! (" + count + "/" + max + ")"), false);
            }
        }

        // CE 결정체 1~3개
        if (CECrystalItem.INSTANCE != null) {
            int crystals = 1 + sw.getRandom().nextInt(3);
            ItemStack drop = new ItemStack(CECrystalItem.INSTANCE, crystals);
            if (!killer.getInventory().insertStack(drop)) {
                killer.dropItem(drop, false);
            }
        }
    }
}
