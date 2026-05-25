package com.jjk.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

public final class HitValidator {

    private HitValidator() {}

    public static List<LivingEntity> getNearby(ServerPlayerEntity caster, double radius) {
        Vec3d center = caster.getPos();
        Box box = new Box(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );
        return caster.getServerWorld()
                .getEntitiesByClass(LivingEntity.class, box,
                        e -> !e.getUuid().equals(caster.getUuid()) && e.isAlive());
    }

    public static List<LivingEntity> getNearbyArc(ServerPlayerEntity caster,
                                                   double radius, float arcDegrees) {
        Vec3d facing = caster.getRotationVec(1.0f);
        double cosHalf = Math.cos(Math.toRadians(arcDegrees / 2.0));
        double r2 = radius * radius;
        Vec3d casterPos = caster.getPos();

        // ServerWorld 전체 LivingEntity 중 필터링
        List<LivingEntity> result = caster.getServerWorld().getEntitiesByType(
                net.minecraft.entity.EntityType.ZOMBIE,  // 임시: 좀비 전용 테스트
                new Box(casterPos.x - radius, casterPos.y - radius, casterPos.z - radius,
                        casterPos.x + radius, casterPos.y + radius, casterPos.z + radius),
                e -> e.isAlive()
        ).stream()
        .map(e -> (LivingEntity) e)
        .collect(Collectors.toList());
        return result;
    }
}
