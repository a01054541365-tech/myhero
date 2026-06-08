package com.jjk.entity.npc;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * 이치지 키요타카 튜토리얼 NPC.
 * 첫 접속 플레이어 스폰 지점 근처에 자동 스폰.
 * 우클릭 1회에 메시지 전송 후 hasCompletedTutorial = true 로 기록.
 */
public class TutorialNpcEntity extends SimpleNpcEntity {

    private static final String[] MESSAGES = {
        "§e[이치지] 주술계에 온 것을 환영합니다.",
        "§e[이치지] 캐릭터 선택 책으로 술식을 선택하십시오. (인벤토리 확인)",
        "§e[이치지] F키: 기본 술식 / R키: 영역 전개 / V키: 각성",
        "§e[이치지] /jj grade 명령어로 현재 등급을 확인할 수 있습니다.",
        "§e[이치지] 행운을 빕니다."
    };

    public TutorialNpcEntity(EntityType<? extends TutorialNpcEntity> type, World world) {
        super(type, world, "tutorial_npc", "이치지 키요타카");
    }

    @Override
    protected void onInteract(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.hasCompletedTutorial) return;

        for (String msg : MESSAGES) {
            player.sendMessage(Text.literal(msg), false);
        }
        data.hasCompletedTutorial = true;
        JJKMod.getPlayerRepository().saveImmediate(data);
    }
}
