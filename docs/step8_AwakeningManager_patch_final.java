// 파일:   src/main/java/com/jjk/awakening/AwakeningManager.java
// 변경:   checkAndActivate(:21 근처), tickCheck(:31 근처) TODO 채움
//
// ── 추가할 import (파일 상단) ────────────────────────────────────────
//
//   import com.jjk.network.s2c.AwakeningS2CPacket;
//   import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
//
// ── checkAndActivate(ServerPlayerEntity player) 내부 ────────────────
//
// 교체 전 (:44 근처):
//   // TODO: send AwakeningS2CPacket
//
// 교체 후:

        ServerPlayNetworking.send(player,
                new AwakeningS2CPacket(player.getUuid(), true));

// ── tickCheck(ServerPlayerEntity player) 내부 ────────────────────────
//
// 교체 전:
//   // TODO: send AwakeningS2CPacket (false)
//
// 교체 후:

        ServerPlayNetworking.send(player,
                new AwakeningS2CPacket(player.getUuid(), false));
