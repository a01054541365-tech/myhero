package com.jjk.event;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.entity.JJKEntities;
import com.jjk.entity.cursed.HannamiNpcEntity;
import com.jjk.entity.cursed.JogoNpcEntity;
import com.jjk.entity.cursed.JuugoNpcEntity;
import com.jjk.grade.GradeManager;
import com.jjk.item.CursedCrystalItem;
import com.jjk.team.TeamManager;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 사멸회유 레이드 이벤트 관리자.
 * TickScheduler period=1 서버 태스크로 등록.
 */
public class RaidEventManager {

    private enum RaidState { IDLE, ANNOUNCED, ACTIVE, REWARD }

    private RaidState state = RaidState.IDLE;
    private long ticksInState = 0L;
    private BlockPos raidCenter = null;
    private final Set<Integer> raidEntityIds = new HashSet<>();

    public void tick(MinecraftServer server) {
        if (JJKMod.getInstance() == null) return;
        int intervalTicks = JJKMod.getConfig().raidEventIntervalTicks();
        ticksInState++;

        ServerWorld overworld = server.getOverworld();

        switch (state) {
            case IDLE -> {
                if (ticksInState >= intervalTicks) {
                    transitionToAnnounced(server);
                }
            }
            case ANNOUNCED -> {
                if (ticksInState >= 1200) { // 60초 = 1200틱
                    transitionToActive(server, overworld);
                }
            }
            case ACTIVE -> {
                // 모든 레이드 엔티티 소멸 여부 확인
                boolean allDead = raidEntityIds.stream()
                    .allMatch(id -> overworld.getEntityById(id) == null
                        || !overworld.getEntityById(id).isAlive());
                if (allDead && !raidEntityIds.isEmpty()) {
                    transitionToReward(server, overworld);
                } else if (ticksInState >= 12000) { // 600초 = 12000틱, 자동 철수
                    for (int id : raidEntityIds) {
                        var e = overworld.getEntityById(id);
                        if (e != null) e.discard();
                    }
                    broadcastAll(server, "§7[주술회전] 사멸회유가 철수했습니다.");
                    reset();
                }
            }
            case REWARD -> {
                if (ticksInState >= 5) {
                    reset(); // REWARD는 즉시 완료 후 IDLE로
                }
            }
        }
    }

    private void transitionToAnnounced(MinecraftServer server) {
        state = RaidState.ANNOUNCED;
        ticksInState = 0L;
        // 스폰 위치 사전 결정
        ServerWorld overworld = server.getOverworld();
        raidCenter = selectSpawnLocation(overworld);
        int x = raidCenter != null ? raidCenter.getX() : 0;
        int z = raidCenter != null ? raidCenter.getZ() : 0;
        broadcastAll(server,
            "§c⚠ [주술회전] 사멸회유가 출몰했습니다! 주술고전 외곽으로 집결하십시오.");
        broadcastAll(server,
            "§e📍 위치: " + x + ", " + z + " 좌표 근처");
    }

    private void transitionToActive(MinecraftServer server, ServerWorld sw) {
        state = RaidState.ACTIVE;
        ticksInState = 0L;
        raidEntityIds.clear();

        if (raidCenter == null) raidCenter = BlockPos.ORIGIN;

        // 죠고 × 1
        spawnRaidEntity(sw, JJKEntities.JOGO_NPC, raidCenter);
        // 하나미 × 1
        spawnRaidEntity(sw, JJKEntities.HANNAMI_NPC, raidCenter.add(5, 0, 0));
        // 죠우고 × 2
        spawnRaidEntity(sw, JJKEntities.JUUGO_NPC, raidCenter.add(-5, 0, 0));
        spawnRaidEntity(sw, JJKEntities.JUUGO_NPC, raidCenter.add(0, 0, 5));
    }

    private <T extends net.minecraft.entity.mob.HostileEntity> void spawnRaidEntity(
            ServerWorld sw, net.minecraft.entity.EntityType<T> type, BlockPos pos) {
        T entity = type.create(sw);
        if (entity == null) return;
        // 레이드 버프: 체력 3배
        EntityAttributeInstance maxHp = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHp != null) maxHp.setBaseValue(maxHp.getBaseValue() * 3.0);
        entity.setHealth(entity.getMaxHealth());
        entity.refreshPositionAndAngles(
            pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        sw.spawnEntity(entity);
        raidEntityIds.add(entity.getId());
    }

    private void transitionToReward(MinecraftServer server, ServerWorld overworld) {
        state = RaidState.REWARD;
        ticksInState = 0L;
        broadcastAll(server,
            "§a[주술회전] 사멸회유 토벌 완료! 참여자에게 보상을 지급합니다.");
        distributeRewards(server, overworld);
    }

    private void distributeRewards(MinecraftServer server, ServerWorld overworld) {
        if (raidCenter == null) return;
        Box rewardBox = new Box(raidCenter).expand(100.0);
        List<ServerPlayerEntity> participants = new ArrayList<>();
        for (ServerPlayerEntity p : overworld.getPlayers()) {
            if (rewardBox.contains(p.getPos())) participants.add(p);
        }

        long currentTick = overworld.getTime();
        for (ServerPlayerEntity player : participants) {
            PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());

            // 공통: CE 결정체 3개
            if (CursedCrystalItem.INSTANCE != null) {
                ItemStack crystals = new ItemStack(CursedCrystalItem.INSTANCE, 3);
                if (!player.getInventory().insertStack(crystals)) {
                    player.dropItem(crystals, false);
                }
            }

            // 공통: XP 500
            JJKMod.getGradeManager().addXp(data, 500, player);

            // 진영별 추가 보상
            TeamManager.Team team = TeamManager.getTeam(data.characterId);
            if (team == TeamManager.Team.JUJUTSU_SORCERER) {
                // 손가락 드롭 확률 2배 (60초 = 1200틱)
                data.cooldowns.put("raid_finger_bonus_until", currentTick + 1200L);
                player.sendMessage(
                    Text.literal("§e[레이드] 주술사 보상: 손가락 드롭 확률 2배 (60초)"), false);
            } else if (team == TeamManager.Team.CURSED_SPIRIT) {
                // CE 최대치 +100 (60초 = 1200틱)
                data.ceMax += 100f;
                data.ceCurrent = Math.min(data.ceCurrent, data.ceMax);
                data.cooldowns.put("raid_ce_bonus_until", currentTick + 1200L);
                player.sendMessage(
                    Text.literal("§5[레이드] 주령 보상: CE 최대치 +100 (60초)"), false);
            }

            JJKMod.getPlayerRepository().saveImmediate(data);
        }
    }

    private void reset() {
        state = RaidState.IDLE;
        ticksInState = 0L;
        raidCenter = null;
        raidEntityIds.clear();
    }

    private void broadcastAll(MinecraftServer server, String message) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.sendMessage(Text.literal(message), false);
        }
    }

    private BlockPos selectSpawnLocation(ServerWorld sw) {
        int cx = JJKMod.getConfig().jjtBuilding_centerX;
        int cz = JJKMod.getConfig().jjtBuilding_centerZ;
        var rng = sw.getRandom();
        // 주술고전 반경 200블록 내 무작위
        int dx = rng.nextBetween(-200, 200);
        int dz = rng.nextBetween(-200, 200);
        int wx = cx + dx;
        int wz = cz + dz;
        int wy = sw.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz);
        return new BlockPos(wx, wy, wz);
    }
}
