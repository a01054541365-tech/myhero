package com.jjk.combat;

import com.jjk.data.PlayerData;

public class CCManager {

    public static boolean tryApplyCC(PlayerData target,
            String ccType, int durationTicks, long currentTick) {

        String immuneKey = "cc_immune_" + ccType;
        String windowKey = "cc_window_" + ccType;

        // 면역 체크
        if (target.cooldowns.getOrDefault(immuneKey, 0L) > currentTick) return false;

        // DR 윈도우 체크 (스턴 4초, 수면 8초, 속박 없음)
        long windowUntil = target.cooldowns.getOrDefault(windowKey, 0L);
        if (windowUntil > currentTick) {
            // 윈도우 내 중복 적용 시 배율 감소
            durationTicks = (int)(durationTicks * 0.5f);
            if (durationTicks <= 0) {
                // 면역 부여
                int immuneTicks = switch (ccType) {
                    case "stun"  -> 20;   // 1초
                    case "sleep" -> 40;   // 2초
                    case "bind"  -> 10;   // 0.5초
                    default      -> 20;
                };
                target.cooldowns.put(immuneKey, currentTick + immuneTicks);
                return false;
            }
        }

        // CC 적용
        target.cooldowns.put("status_" + ccType, currentTick + durationTicks);

        // DR 윈도우 세팅
        int windowTicks = switch (ccType) {
            case "stun"  -> 80;   // 4초
            case "sleep" -> 160;  // 8초
            default      -> 0;
        };
        if (windowTicks > 0) {
            target.cooldowns.put(windowKey, currentTick + windowTicks);
        }
        return true;
    }
}
