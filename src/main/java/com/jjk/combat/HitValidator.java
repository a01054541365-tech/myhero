package com.jjk.combat;

import com.jjk.JJKMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

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

    /**
     * 전방 직선 참격 판정: 폭 width × 길이 length 박스.
     * 시전자 전방으로 length 블록 내, 좌우 width/2 범위의 LivingEntity 반환.
     */
    public static List<LivingEntity> getNearbyBox(ServerPlayerEntity caster,
                                                   double width, double length) {
        Vec3d pos    = caster.getPos();
        Vec3d facing = caster.getRotationVec(1.0f);
        // 전방 beam의 중심 AABB (facing 방향으로 length 범위)
        Vec3d end    = pos.add(facing.multiply(length));
        double hw    = width / 2.0;
        Box searchBox = new Box(
            Math.min(pos.x, end.x) - hw, pos.y - 1, Math.min(pos.z, end.z) - hw,
            Math.max(pos.x, end.x) + hw, pos.y + 3, Math.max(pos.z, end.z) + hw
        );
        return caster.getServerWorld().getEntitiesByClass(LivingEntity.class, searchBox, e -> {
            if (!e.isAlive() || e.getUuid().equals(caster.getUuid())) return false;
            if (e instanceof ServerPlayerEntity targetPlayer) {
                com.jjk.data.PlayerData cd = JJKMod.getPlayerRepository().load(caster.getUuid());
                com.jjk.data.PlayerData td = JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
                if (JJKMod.getTeamManager().isSameTeam(cd, td)) return false;
            }
            // 전방 반구 내 있는지 확인
            Vec3d toEntity = e.getPos().subtract(pos);
            return toEntity.dotProduct(facing) >= 0 && toEntity.lengthSquared() <= length * length;
        });
    }

    /**
     * 직선 레이캐스트 관통: start~end 선분과 교차하는 모든 LivingEntity 반환.
     * @param hitboxRadius 히트박스 확장 반경
     */
    public static List<LivingEntity> getRaycastPiercing(ServerPlayerEntity caster,
                                                         Vec3d start, Vec3d end,
                                                         double hitboxRadius) {
        Box searchBox = new Box(start, end).expand(hitboxRadius);
        return caster.getServerWorld().getEntitiesByClass(LivingEntity.class, searchBox, e -> {
            if (!e.isAlive() || e.getUuid().equals(caster.getUuid())) return false;
            if (e instanceof ServerPlayerEntity targetPlayer) {
                com.jjk.data.PlayerData cd = JJKMod.getPlayerRepository().load(caster.getUuid());
                com.jjk.data.PlayerData td = JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
                if (JJKMod.getTeamManager().isSameTeam(cd, td)) return false;
            }
            Optional<Vec3d> hit = e.getBoundingBox().expand(hitboxRadius).raycast(start, end);
            return hit.isPresent();
        });
    }

    public static List<LivingEntity> getNearbyArc(ServerPlayerEntity caster,
                                                   double radius, float arcDegrees) {
        Vec3d facing = caster.getRotationVec(1.0f);
        double cosHalf = Math.cos(Math.toRadians(arcDegrees / 2.0));
        double r2 = radius * radius;
        Vec3d casterPos = caster.getPos();

        return caster.getServerWorld().getEntitiesByClass(LivingEntity.class,
                new Box(casterPos.x - radius, casterPos.y - radius, casterPos.z - radius,
                        casterPos.x + radius, casterPos.y + radius, casterPos.z + radius),
                e -> {
                    if (!e.isAlive() || e.getUuid().equals(caster.getUuid())) return false;
                    if (e instanceof ServerPlayerEntity targetPlayer) {
                        com.jjk.data.PlayerData casterData = JJKMod.getPlayerRepository().load(caster.getUuid());
                        com.jjk.data.PlayerData targetData = JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
                        if (JJKMod.getTeamManager().isSameTeam(casterData, targetData)) return false;
                    }
                    Vec3d to = e.getPos().subtract(casterPos);
                    return to.lengthSquared() <= r2
                            && facing.dotProduct(to.normalize()) >= cosHalf;
                });
    }
}
