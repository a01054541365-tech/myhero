package com.jjk.api.skill;

import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

public interface ISkillSet {
    // 기존 MC 경로 (하위 호환)
    SkillResult use(ServerPlayerEntity player, int keyId);
    boolean canUse(ServerPlayerEntity player, int keyId);
    int getCooldownTicks(int keyId);
    int getCeCost(int keyId);
    String getSkillName(int keyId);

    // 순수 PlayerData 경로 — player=null 허용 (테스트용)
    // 미구현 키는 NOT_IMPLEMENTED 반환
    default SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick)      { return SkillResult.NOT_IMPLEMENTED; }
    default SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) { return SkillResult.NOT_IMPLEMENTED; }
    default SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick)      { return SkillResult.NOT_IMPLEMENTED; }
    default SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) { return SkillResult.NOT_IMPLEMENTED; }
    default SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick)      { return SkillResult.NOT_IMPLEMENTED; }

    // keyId → on* 라우팅 (CombatPipeline / 테스트에서 사용)
    default SkillResult dispatch(int keyId, PlayerData data, ServerPlayerEntity player, long tick) {
        return switch (keyId) {
            case 0 -> onF(data, player, tick);
            case 1 -> onShiftF(data, player, tick);
            case 2 -> onR(data, player, tick);
            case 3 -> onShiftR(data, player, tick);
            case 4 -> onV(data, player, tick);
            default -> SkillResult.NOT_IMPLEMENTED;
        };
    }
}
