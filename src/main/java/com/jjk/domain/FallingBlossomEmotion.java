package com.jjk.domain;

import com.jjk.JJKMod;
import com.jjk.data.Grade;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.ThreadLocalRandom;

public class FallingBlossomEmotion {

    public static boolean tryActivate(ServerPlayerEntity caster,
            ServerPlayerEntity target, long currentTick) {
        PlayerData data = JJKMod.getPlayerRepository().load(caster.getUuid());

        // 조건 1: 시전자 등급 1급 이상
        if (data.grade == null || data.grade.ordinal() < Grade.GRADE_1.ordinal()) return false;

        // 조건 2: 간이영역 활성 상태
        if (!data.simpleBarrierActive) return false;

        // 조건 3: 영역 전개 중인 대상이 존재
        if (!JJKMod.getDomainManager().hasActiveDomain(target.getUuid())) return false;

        // 속박 체크
        PlayerData targetData = JJKMod.getPlayerRepository().load(target.getUuid());
        boolean targetBound = targetData.cooldowns.getOrDefault("status_bind", 0L) > currentTick;

        float successRate = targetBound ? 0.65f : 0.80f;
        boolean success = ThreadLocalRandom.current().nextFloat() < successRate;
        if (!success) return false;

        // 성공: 영역 내 데미지 40% 경감, 10초
        data.fallingBlossomActive = true;
        data.fallingBlossomUntil = currentTick + 200;
        JJKMod.getPlayerRepository().save(data);
        return true;
    }
}
