package com.jjk.domain;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// 영역 전개 시 변경된 블록을 관리한다.
// - enqueue()         : 블록 변경 예약 + DB 기록
// - tick()            : 매 틱 CHANGES_PER_TICK 개씩 적용 (서버 부하 분산)
// - enqueueRestore()  : 도메인 종료 시 복구 예약 + DB 완료 표시
// - recoverFromDB()   : 서버 재시작 후 미복구 항목 복구
// - registerChunkLoadListener() : 미로드 청크 복구를 청크 로드 시점으로 지연
public class DomainBlockQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-domain-queue");
    private static final int CHANGES_PER_TICK = 200;

    // TPSGuard 연동 — TPS 저하 시 틱당 처리량을 줄인다 (기본값 = CHANGES_PER_TICK)
    private int maxBlocksPerTick = CHANGES_PER_TICK;
    // TPSGuard 연동 — TPS 위험 수준에서 신규 영역 전개로 인한 블록 변경을 차단
    private boolean newDeploymentsBlocked = false;

    /** TPSGuard 연동 — 틱당 최대 블록 변경 처리량 조정. */
    public void setMaxBlocksPerTick(int max) {
        this.maxBlocksPerTick = max;
    }

    /** TPSGuard 연동 — true면 enqueue()로 들어오는 신규 영역 블록 변경을 거부한다. */
    public void blockNewDeployments(boolean blocked) {
        this.newDeploymentsBlocked = blocked;
    }

    private final DomainBlockHistoryDao dao;

    // 도메인별 in-memory 캡처 (복구용 원본 상태 보관)
    private final Map<String, List<OriginalBlock>> capturedByDomain = new HashMap<>();
    // 도메인별 적용 대기 큐 (world.setBlockState 미완료 항목)
    private final Map<String, Deque<QueuedChange>> changeByDomain = new HashMap<>();
    // 전역 복구 큐 (도메인 종료 후 원상복구)
    private final Deque<QueuedChange> restoreQueue = new ArrayDeque<>();
    // 미로드 청크 복구 대기: worldKey → chunkPosLong → 항목 목록
    private final Map<String, Map<Long, List<DomainBlockHistoryDao.BlockHistoryEntry>>>
        pendingChunkRecovery = new ConcurrentHashMap<>();

    private record OriginalBlock(BlockPos pos, BlockState originalState) {}
    private record QueuedChange(BlockPos pos, BlockState targetState, String worldKey) {}

    public DomainBlockQueue(DomainBlockHistoryDao dao) {
        this.dao = dao;
    }

    // ─── 영역 전개 시 호출 ─────────────────────────────────────────────────────

    // 영역 블록 변경 예약. originalState 캡처 후 DB 기록 → 실제 변경은 tick()에서 처리.
    public void enqueue(String domainId, ServerWorld world,
                        List<BlockPos> positions, BlockState targetState) {
        if (newDeploymentsBlocked) {
            LOGGER.warn("[JJK] TPS 위험 수준 — 신규 영역 블록 변경 거부: domainId={}", domainId);
            return;
        }
        String worldKey = world.getRegistryKey().getValue().toString();
        List<DomainBlockHistoryDao.BlockHistoryEntry> dbEntries = new ArrayList<>();
        List<OriginalBlock> captured = capturedByDomain
                .computeIfAbsent(domainId, k -> new ArrayList<>());
        Deque<QueuedChange> queue = changeByDomain
                .computeIfAbsent(domainId, k -> new ArrayDeque<>());

        String changedId = Registries.BLOCK.getId(targetState.getBlock()).toString();
        for (BlockPos pos : positions) {
            BlockState original = world.getBlockState(pos);
            // 공기·유체는 기록 제외
            if (original.isAir() || !original.getFluidState().isEmpty()) continue;
            captured.add(new OriginalBlock(pos, original));
            queue.add(new QueuedChange(pos, targetState, worldKey));
            String origId = Registries.BLOCK.getId(original.getBlock()).toString();
            dbEntries.add(new DomainBlockHistoryDao.BlockHistoryEntry(
                    -1L, domainId, worldKey,
                    pos.getX(), pos.getY(), pos.getZ(),
                    origId, changedId));
        }
        if (!dbEntries.isEmpty()) {
            dao.insertBatch(domainId, worldKey, dbEntries);
        }
    }

    // ─── 도메인 종료 시 호출 ───────────────────────────────────────────────────

    // 미적용 변경 취소 + 이미 적용된 블록 복구 예약 + DB 완료 표시.
    public void enqueueRestore(String domainId, ServerWorld world) {
        String worldKey = world.getRegistryKey().getValue().toString();

        // 아직 적용되지 않은 변경 예약 취소
        changeByDomain.remove(domainId);

        // in-memory 원본에서 복구 큐 생성
        List<OriginalBlock> captured = capturedByDomain.remove(domainId);
        if (captured != null) {
            for (OriginalBlock ob : captured) {
                restoreQueue.add(new QueuedChange(ob.pos(), ob.originalState(), worldKey));
            }
        }

        // DB: 도메인 전체 복구 완료 표시 (복구 진행 중이므로 즉시 마킹)
        dao.markAllRecovered(domainId);
    }

    // ─── TickScheduler 등록 (registerServerTask, period=1) ────────────────────

    public void tick(MinecraftServer server) {
        // 1. 도메인 블록 변경 적용
        Iterator<Map.Entry<String, Deque<QueuedChange>>> it = changeByDomain.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Deque<QueuedChange>> entry = it.next();
            Deque<QueuedChange> queue = entry.getValue();
            int count = 0;
            while (!queue.isEmpty() && count++ < maxBlocksPerTick) {
                QueuedChange c = queue.poll();
                ServerWorld w = getWorld(server, c.worldKey());
                if (w == null) continue;
                forceLoadChunk(w, c.pos());
                w.setBlockState(c.pos(), c.targetState(), Block.NOTIFY_LISTENERS);
            }
            if (queue.isEmpty()) it.remove();
        }

        // 2. 복구 처리
        int restoreCount = 0;
        while (!restoreQueue.isEmpty() && restoreCount++ < CHANGES_PER_TICK) {
            QueuedChange c = restoreQueue.poll();
            ServerWorld w = getWorld(server, c.worldKey());
            if (w == null) { w = server.getOverworld(); }
            if (w == null) continue;
            forceLoadChunk(w, c.pos());
            w.setBlockState(c.pos(), c.targetState(), Block.NOTIFY_LISTENERS);
        }
    }

    // ─── 서버 재시작 복구 ──────────────────────────────────────────────────────

    // SERVER_STARTED 이벤트 내에서 1회 호출.
    public void recoverFromDB(MinecraftServer server) {
        List<DomainBlockHistoryDao.BlockHistoryEntry> unrecovered = dao.loadUnrecovered();
        if (unrecovered.isEmpty()) return;
        LOGGER.info("[JJK] 미복구 영역 블록 {}개 복구 시작", unrecovered.size());

        // worldKey 기준으로 그룹화
        Map<String, List<DomainBlockHistoryDao.BlockHistoryEntry>> byWorld = new HashMap<>();
        for (var entry : unrecovered) {
            byWorld.computeIfAbsent(entry.worldKey(), k -> new ArrayList<>()).add(entry);
        }

        int restoredNow = 0;
        int deferred = 0;
        for (var worldEntry : byWorld.entrySet()) {
            ServerWorld world = getWorld(server, worldEntry.getKey());
            if (world == null) {
                LOGGER.warn("[JJK] 미복구 블록: 월드 '{}' 없음 — 스킵", worldEntry.getKey());
                continue;
            }
            for (var entry : worldEntry.getValue()) {
                BlockPos pos = new BlockPos(entry.x(), entry.y(), entry.z());
                ChunkPos chunkPos = new ChunkPos(pos);
                if (world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
                    restoreBlock(world, entry);
                    dao.markRecovered(entry.id());
                    restoredNow++;
                } else {
                    // 청크 미로드 → 청크 로드 이벤트 시 복구
                    pendingChunkRecovery
                        .computeIfAbsent(worldEntry.getKey(), k -> new ConcurrentHashMap<>())
                        .computeIfAbsent(chunkPos.toLong(), k -> new ArrayList<>())
                        .add(entry);
                    deferred++;
                }
            }
        }
        LOGGER.info("[JJK] 즉시 복구 {}개, 청크 로드 대기 {}개", restoredNow, deferred);
    }

    // onInitialize() 또는 onServerStarting() 에서 1회 호출.
    // ServerChunkEvents.CHUNK_LOAD — Fabric API 1.21.1 지원 확인됨.
    public void registerChunkLoadListener(MinecraftServer server) {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            String worldKey = world.getRegistryKey().getValue().toString();
            Map<Long, List<DomainBlockHistoryDao.BlockHistoryEntry>> worldMap =
                    pendingChunkRecovery.get(worldKey);
            if (worldMap == null) return;

            List<DomainBlockHistoryDao.BlockHistoryEntry> entries =
                    worldMap.remove(chunk.getPos().toLong());
            if (entries == null || entries.isEmpty()) return;

            // 블록 복구는 메인 스레드에서 실행
            server.execute(() -> {
                for (var entry : entries) {
                    restoreBlock(world, entry);
                    dao.markRecovered(entry.id());
                }
                LOGGER.debug("[JJK] 청크 로드 시 미복구 블록 {}개 복구", entries.size());
            });
        });
    }

    // ─── 관리자 명령 (/jj rollback) ────────────────────────────────────────────

    // /jj rollback domain — domainId의 미복구 블록 전체를 즉시 복구 (청크 강제 로드).
    public int rollbackDomain(MinecraftServer server, String domainId) {
        List<DomainBlockHistoryDao.BlockHistoryEntry> unrecovered = dao.loadUnrecovered();
        int restored = 0;
        for (var entry : unrecovered) {
            if (!entry.domainId().equals(domainId)) continue;
            ServerWorld world = getWorld(server, entry.worldKey());
            if (world == null) continue;
            BlockPos pos = new BlockPos(entry.x(), entry.y(), entry.z());
            forceLoadChunk(world, pos);
            restoreBlock(world, entry);
            dao.markRecovered(entry.id());
            restored++;
        }
        dao.markAllRecovered(domainId);
        return restored;
    }

    // /jj rollback chunk — 지정 청크의 미복구 블록 즉시 복구. 청크 미로드 시 -1.
    public int rollbackChunk(ServerWorld world, int chunkX, int chunkZ) {
        if (!world.isChunkLoaded(chunkX, chunkZ)) return -1;
        String worldKey = world.getRegistryKey().getValue().toString();
        List<DomainBlockHistoryDao.BlockHistoryEntry> unrecovered = dao.loadUnrecovered();
        int restored = 0;
        for (var entry : unrecovered) {
            if (!entry.worldKey().equals(worldKey)) continue;
            if ((entry.x() >> 4) != chunkX || (entry.z() >> 4) != chunkZ) continue;
            restoreBlock(world, entry);
            dao.markRecovered(entry.id());
            restored++;
        }
        return restored;
    }

    // ─── 내부 헬퍼 ────────────────────────────────────────────────────────────

    private void restoreBlock(ServerWorld world,
                               DomainBlockHistoryDao.BlockHistoryEntry entry) {
        Identifier id = Identifier.tryParse(entry.originalState());
        if (id == null) return;
        net.minecraft.block.Block block = Registries.BLOCK.get(id);
        if (block == null) return;
        BlockPos pos = new BlockPos(entry.x(), entry.y(), entry.z());
        world.setBlockState(pos, block.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private void forceLoadChunk(ServerWorld world, BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        if (!world.isChunkLoaded(cx, cz)) {
            world.getChunkManager().getChunk(
                cx, cz, net.minecraft.world.chunk.ChunkStatus.FULL, true);
        }
    }

    private static ServerWorld getWorld(MinecraftServer server, String worldKey) {
        if (worldKey == null) return server.getOverworld();
        for (ServerWorld w : server.getWorlds()) {
            if (w.getRegistryKey().getValue().toString().equals(worldKey)) return w;
        }
        return null;
    }
}
