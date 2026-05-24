package com.jjk.combat;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.stream.Collectors;

public final class HitValidator {

    private HitValidator() {}

    public static List<ServerPlayerEntity> getNearby(ServerPlayerEntity caster, double radius) {
        double r2 = radius * radius;
        return caster.getServerWorld().getPlayers().stream()
                .filter(p -> !p.equals(caster) && p.squaredDistanceTo(caster) <= r2)
                .collect(Collectors.toList());
    }

    // Front-arc detection, ignores Y component (horizontal angle only)
    public static List<ServerPlayerEntity> getNearbyArc(ServerPlayerEntity caster, double radius, float arcDegrees) {
        Vec3d facing = caster.getRotationVec(1.0f);
        double cosHalf = Math.cos(Math.toRadians(arcDegrees / 2.0));
        double r2 = radius * radius;
        return caster.getServerWorld().getPlayers().stream()
                .filter(p -> {
                    if (p.equals(caster)) return false;
                    if (p.squaredDistanceTo(caster) > r2) return false;
                    Vec3d dir = p.getPos().subtract(caster.getPos()).normalize();
                    // Horizontal angle only (ignore Y)
                    double dot = facing.x * dir.x + facing.z * dir.z;
                    return dot >= cosHalf;
                })
                .collect(Collectors.toList());
    }
}
