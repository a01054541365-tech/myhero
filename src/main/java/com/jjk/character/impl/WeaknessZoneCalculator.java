package com.jjk.character.impl;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * 나나미 십획주법 7:3 약점 판정.
 * target 등 뒤 ±30도(= 전방과의 각도 > 150도) 범위에서 공격 시 약점 명중.
 * PlayerData에 pos/yaw 없으므로 ServerPlayerEntity 기반 구현.
 */
public final class WeaknessZoneCalculator {
    private WeaknessZoneCalculator() {}

    private static final double COS_150 = Math.cos(Math.toRadians(150.0));  // ≈ -0.866

    /**
     * attacker가 target의 등 뒤 60도 범위에 있으면 true.
     * target이 null이거나 ServerPlayerEntity가 아니면 false.
     */
    public static boolean isWeaknessHit(ServerPlayerEntity attacker, LivingEntity target) {
        if (target == null) return false;

        Vec3d attackerPos = attacker.getPos();
        Vec3d targetPos = target.getPos();

        // attacker → target 방향 벡터 (XZ 평면)
        double dx = attackerPos.x - targetPos.x;
        double dz = attackerPos.z - targetPos.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.01) return false;
        dx /= len;
        dz /= len;

        // target의 진행 방향 (yaw 기준, Minecraft: 0=south, +90=west, ±180=north, -90=east)
        float yaw = target.getYaw();
        double facingX = -Math.sin(Math.toRadians(yaw));
        double facingZ =  Math.cos(Math.toRadians(yaw));

        // dot(target_facing, attacker_direction) = cos(angle)
        // angle > 150° → attacker가 target 등 뒤 → 약점
        double dot = facingX * dx + facingZ * dz;
        return dot < COS_150;
    }
}
