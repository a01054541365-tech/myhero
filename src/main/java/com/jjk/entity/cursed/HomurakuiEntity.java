package com.jjk.entity.cursed;

import com.jjk.item.CECrystalItem;
import com.jjk.network.s2c.SkillEffectS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.List;

// 炎喰 (호무라쿠이) — 3급 주령. 화염 도트 + HP 20% 이하 자폭
public class HomurakuiEntity extends HostileEntity {

    private boolean exploding = false;
    private int explodeCountdown = 0;

    public HomurakuiEntity(EntityType<? extends HomurakuiEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH,     80.0)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,  10.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED,  0.27)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,   20.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new ExplosionGoal());
        goalSelector.add(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(7, new WanderAroundFarGoal(this, 0.8f));
        goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(9, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        if (!(getWorld() instanceof ServerWorld sw)) return;

        // 화염 도트: 반경 4블록, 매 20틱, baseDamage=5 (바닐라 Explosion API 미사용)
        if (age % 20 == 0) {
            List<LivingEntity> nearby = sw.getEntitiesByClass(
                LivingEntity.class, getBoundingBox().expand(4.0),
                e -> e.isAlive() && !e.equals(this));
            for (LivingEntity target : nearby) {
                target.damage(getDamageSources().mobAttack(this), 5f);
            }
        }

        // 자폭 조건: HP 20% 이하
        if (!exploding && getHealth() / getMaxHealth() <= 0.20f) {
            startExplosionCountdown(sw);
        }
    }

    private void startExplosionCountdown(ServerWorld sw) {
        exploding = true;
        explodeCountdown = 60;
        // 경고 S2C 패킷 전송 (반경 20블록 이내 플레이어)
        SkillEffectS2CPacket warning = SkillEffectS2CPacket.of(
            "homuraku_explode_warning", getUuid(), getX(), getY(), getZ());
        for (ServerPlayerEntity player : sw.getPlayers()) {
            if (squaredDistanceTo(player) <= 400.0) { // 20블록²
                ServerPlayNetworking.send(player, warning);
            }
        }
    }

    private void doExplosion(ServerWorld sw) {
        // 반경 6블록 폭발 데미지 40 — 바닐라 Explosion API 미사용
        List<LivingEntity> targets = sw.getEntitiesByClass(
            LivingEntity.class, getBoundingBox().expand(6.0),
            e -> e.isAlive() && !e.equals(this));
        for (LivingEntity target : targets) {
            target.damage(getDamageSources().mobAttack(this), 40f);
        }
        discard();
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!(getWorld() instanceof ServerWorld sw)) return;
        if (!(source.getAttacker() instanceof ServerPlayerEntity killer)) return;
        if (sw.getRandom().nextFloat() < 0.50f && CECrystalItem.INSTANCE != null) {
            ItemStack drop = new ItemStack(CECrystalItem.INSTANCE, 1);
            if (!killer.getInventory().insertStack(drop)) {
                killer.dropItem(drop, false);
            }
        }
        // 주구 조각 5% — 해당 아이템 미정의로 미구현 (Phase D에서 추가 예정)
    }

    // 자폭 Goal — HP 20% 이하 감지 후 카운트다운 처리
    private class ExplosionGoal extends Goal {

        ExplosionGoal() {
            setControls(EnumSet.allOf(Control.class));
        }

        @Override
        public boolean canStart() { return exploding; }

        @Override
        public boolean shouldContinue() { return exploding; }

        @Override
        public void tick() {
            if (!(HomurakuiEntity.this.getWorld() instanceof ServerWorld sw)) return;
            if (--explodeCountdown <= 0) {
                doExplosion(sw);
            }
        }
    }
}
