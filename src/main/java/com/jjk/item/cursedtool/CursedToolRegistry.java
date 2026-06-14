package com.jjk.item.cursedtool;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.combat.DamageCalculator;
import com.jjk.combat.DamageContext;
import com.jjk.data.PlayerData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

public final class CursedToolRegistry {

    private static final CursedToolEffect DEFAULT = CursedToolEffect.builder().build();

    private CursedToolRegistry() {}

    public static void register() {
        // ── 특급 주구 ──────────────────────────────────────────────────────────

        // 천역모: 피격 대상의 모든 술식 방어 무력화
        reg("chokoku", new CursedToolItem(CursedToolGrade.SPECIAL_GRADE,
                CursedToolEffect.builder().nullifyTechnique().build(), "chokoku"));

        // 유운: 물리 데미지 ×2.0
        reg("playful_cloud", new CursedToolItem(CursedToolGrade.SPECIAL_GRADE,
                CursedToolEffect.builder().damageMultiplier(2.0f).build(), "playful_cloud"));

        // 가미토케: F키 발동형 — CE 100 소모, 반경 5블록 800 고정 데미지 (DamageCalculator 통과)
        reg("kamutoke", new CursedToolItem(CursedToolGrade.SPECIAL_GRADE, DEFAULT, "kamutoke") {
            @Override
            public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
                if (hand != Hand.MAIN_HAND) {
                    return TypedActionResult.pass(user.getStackInHand(hand));
                }
                if (world.isClient()) {
                    return TypedActionResult.success(user.getStackInHand(hand));
                }
                if (JJKMod.getInstance() == null) {
                    return TypedActionResult.fail(user.getStackInHand(hand));
                }
                ServerPlayerEntity player = (ServerPlayerEntity) user;
                if (!JJKMod.getCEManager().consume(player, 100f)) {
                    return TypedActionResult.fail(user.getStackInHand(hand));
                }
                ServerWorld sw = (ServerWorld) world;
                List<LivingEntity> targets = world.getEntitiesByClass(
                        LivingEntity.class, player.getBoundingBox().expand(5.0),
                        e -> e != player && e.isAlive());
                DamageCalculator calc = new DamageCalculator();
                for (LivingEntity target : targets) {
                    DamageContext ctx = DamageContext
                            .builder(player, target, IDamageSource.NORMAL_TECHNIQUE, 800f)
                            .build();
                    float dmg = calc.calculate(ctx);
                    target.damage(sw.getDamageSources().generic(), dmg);
                }
                return TypedActionResult.success(user.getStackInHand(hand));
            }
        });

        // 마이: 영혼 직격 + RCT 우회
        reg("split_soul_katana", new CursedToolItem(CursedToolGrade.SPECIAL_GRADE,
                CursedToolEffect.builder().soulDirect().bypassRCT().build(), "split_soul_katana"));

        // 드래곤본: 3회 적중 후 4번째 타격 ×3.0
        reg("dragon_bone", new CursedToolItem(CursedToolGrade.SPECIAL_GRADE,
                CursedToolEffect.builder().resonanceCharges(3).build(), "dragon_bone"));

        // ── 1급 주구 ──────────────────────────────────────────────────────────

        // 나나미 둔도: 치명타 보너스는 NanamiSkillSet에서 처리
        reg("nanami_blunt", new CursedToolItem(CursedToolGrade.GRADE_1, DEFAULT, "nanami_blunt"));

        // 주력 강화 카타나
        reg("technique_sword", new CursedToolItem(CursedToolGrade.GRADE_1,
                CursedToolEffect.builder().damageMultiplier(1.5f).build(), "technique_sword"));

        // 표창형 주구: use() 에서 ArrowEntity 스폰
        reg("throwing_tool", new CursedToolItem(CursedToolGrade.GRADE_1, DEFAULT, "throwing_tool") {
            @Override
            public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
                if (hand != Hand.MAIN_HAND) {
                    return TypedActionResult.pass(user.getStackInHand(hand));
                }
                if (world.isClient()) {
                    return TypedActionResult.success(user.getStackInHand(hand));
                }
                ItemStack stack = user.getStackInHand(hand);
                stack.decrement(1);
                ArrowEntity arrow = new ArrowEntity(
                        world,
                        user.getX(), user.getEyeY() - 0.1, user.getZ(),
                        new ItemStack(Items.ARROW), null);
                arrow.setOwner(user);
                Vec3d dir = user.getRotationVec(1.0f).multiply(2.5);
                arrow.setVelocity(dir.x, dir.y, dir.z);
                world.spawnEntity(arrow);
                return TypedActionResult.success(stack);
            }
        });

        // ── 2~3급 주구 ────────────────────────────────────────────────────────

        // 주력 강화 단검
        reg("cursed_dagger", new CursedToolItem(CursedToolGrade.GRADE_3,
                CursedToolEffect.builder().damageMultiplier(1.2f).build(), "cursed_dagger"));

        // CE 결정 지팡이: CE 재생 보너스
        reg("ce_staff", new CursedToolItem(CursedToolGrade.GRADE_2,
                CursedToolEffect.builder().ceRegenBonus(0.5f).build(), "ce_staff"));

        // 봉인 부적: 소모성 — 최근접 적 플레이어에게 sealDurationTicks 적용
        reg("seal_talisman", new CursedToolItem(CursedToolGrade.GRADE_3, DEFAULT, "seal_talisman") {
            @Override
            public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
                if (hand != Hand.MAIN_HAND) {
                    return TypedActionResult.pass(user.getStackInHand(hand));
                }
                if (world.isClient()) {
                    return TypedActionResult.success(user.getStackInHand(hand));
                }
                if (JJKMod.getInstance() == null) {
                    return TypedActionResult.fail(user.getStackInHand(hand));
                }
                ServerPlayerEntity self = (ServerPlayerEntity) user;
                List<ServerPlayerEntity> targets = world.getEntitiesByClass(
                        ServerPlayerEntity.class, self.getBoundingBox().expand(8.0),
                        e -> e != self && e.isAlive());
                if (targets.isEmpty()) {
                    return TypedActionResult.fail(user.getStackInHand(hand));
                }
                targets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(self)));
                ServerPlayerEntity target = targets.get(0);
                PlayerData targetData = JJKMod.getPlayerRepository().load(target.getUuid());
                long expiry = world.getTime() + JJKMod.getConfig().sealDurationTicks;
                targetData.cooldowns.merge("skill_seal", expiry, Math::max);
                JJKMod.getPlayerRepository().save(targetData);
                user.getStackInHand(hand).decrement(1);
                return TypedActionResult.success(user.getStackInHand(hand));
            }
        });

        // 주력 감지 구슬: 보유 시 PassiveEffect 활성 (PlayerData.equippedToolId 로 판별)
        reg("cursed_orb", new CursedToolItem(CursedToolGrade.GRADE_2, DEFAULT, "cursed_orb"));
    }

    private static void reg(String id, CursedToolItem item) {
        Registry.register(Registries.ITEM, Identifier.of("jjk", id), item);
    }
}
