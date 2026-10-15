package com.ice2974.carpeticeaddition.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//#if MC<12105
//$$import java.util.Comparator;
//#endif
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * /weakload 的票据与生命周期管理层。票据等级恒为 BLOCK_TICKING：
 * 以 radius 1 添加（中心区块等级 32，恰比原版 /forceload 的 31 弱一档），
 * 区块持续执行计划刻与方块实体刻，但不执行实体刻与随机刻。
 *
 * <p>票据类型按版本注册：1.21.1–1.21.4 无注册表，直接 create；1.21.5 起必须写入
 * BuiltInRegistries.TICKET_TYPE 且在注册表冻结前完成，因此注册入口固定在 mod init。
 * 持久化由 WeakloadRegistry 的 JSON 状态机负责，本类只做票据差分的执行。
 */
public final class WeakloadManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeakloadManager.class);
    private static final int WEAKLOAD_TICKET_RADIUS = 1;
    private static final String WEAKLOAD_TICKET_ID = "carpet-ice-addition:weakload";
    private static final long[] NO_CHUNKS = new long[0];

    private static WeakloadRegistry registry;
    // 1.21.1 时代 TicketType 是泛型，这里以 raw 使用（unchecked 但可编译）
    private static TicketType weakloadTicketType;

    private WeakloadManager() {
    }

    /** 在 mod init 阶段注册票据类型；1.21.5+ 必须先于注册表冻结与世界加载。 */
    public static void registerTicketType() {
        if (weakloadTicketType != null) {
            return;
        }
        //#if MC>=12109
        weakloadTicketType = Registry.register(BuiltInRegistries.TICKET_TYPE, WEAKLOAD_TICKET_ID,
                new TicketType(TicketType.NO_TIMEOUT, weakloadTicketFlags()));
        //#elseif MC>=12105
        //$$        weakloadTicketType = Registry.register(BuiltInRegistries.TICKET_TYPE, WEAKLOAD_TICKET_ID,
        //$$                new TicketType(TicketType.NO_TIMEOUT, false, TicketType.TicketUse.LOADING_AND_SIMULATION));
        //#else
        //$$        weakloadTicketType = TicketType.create("carpet_ice_addition_weakload", Comparator.comparingLong(ChunkPos::toLong));
        //#endif
    }

    //#if MC>=12109
    private static int weakloadTicketFlags() {
        // 具名常量组合：加载 + 模拟（方块刻）+ 维度维活；不含 PERSIST，
        // 票据不写入原版存档数据，持久化完全由本模组的 JSON 承担。
        return TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE;
    }
    //#endif

    public static synchronized void initialize(Path saveRoot) {
        registry = new WeakloadRegistry(WeakloadStore.resolvePath(saveRoot));
    }

    public static synchronized void shutdown() {
        registry = null;
    }

    /** 世界加载完成后恢复各维度票据；维度不存在时跳过，其 JSON 记录原样保留。 */
    public static synchronized void restoreTickets(MinecraftServer server) {
        WeakloadRegistry activeRegistry = registry;
        if (activeRegistry == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            String dimensionId = level.dimension().identifier().toString();
            long[] chunks = activeRegistry.registeredChunks(dimensionId);
            if (chunks.length == 0) {
                continue;
            }
            List<Long> applied = new ArrayList<>(chunks.length);
            for (long packed : chunks) {
                try {
                    applyTicket(level, packed, true);
                    applied.add(packed);
                } catch (Throwable throwable) {
                    LOGGER.error("Failed to restore weakload ticket for chunk [{}, {}] in dimension {}",
                            WeakloadRegistry.unpackX(packed), WeakloadRegistry.unpackZ(packed), dimensionId,
                            throwable);
                    // 抛错的当次 add 可能已产生副作用：尝试补偿撤销。补偿再失败则票据状态
                    // 不确定（原版侧可能残留额外票据），进入写保护并交由下次重启重建；该
                    // 区块不计入 ticketed，保持登记
                    try {
                        applyTicket(level, packed, false);
                    } catch (Throwable compensationFailure) {
                        LOGGER.error("The weakload ticket state for chunk [{}, {}] in dimension {} is "
                                        + "uncertain: an extra ticket may remain on the vanilla side. Tickets "
                                        + "will be rebuilt from the stored data after the next restart; weakload "
                                        + "data is now write protected",
                                WeakloadRegistry.unpackX(packed), WeakloadRegistry.unpackZ(packed), dimensionId,
                                compensationFailure);
                        activeRegistry.enterWriteProtection();
                    }
                }
            }
            activeRegistry.markTicketed(dimensionId, toLongArray(applied));
            if (applied.size() < chunks.length) {
                LOGGER.warn("Dimension {} restored {}/{} weakload tickets; the missing entries stay "
                        + "registered and retry after the next restart", dimensionId, applied.size(), chunks.length);
            }
        }
    }

    /**
     * 1.21.1–1.21.8 的维度维活：原版维度活跃判定不含自定义票据，空维度 300 tick 后
     * 会冻结方块实体刻。仅对实际持有弱加载票据的维度清零 emptyTime。
     * 1.21.9+ 由票据的 KEEP_DIMENSION_ACTIVE 标志位交由原版处理，不调用本方法。
     */
    public static synchronized void onServerTick(MinecraftServer server) {
        WeakloadRegistry activeRegistry = registry;
        if (activeRegistry == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (activeRegistry.hasActiveTickets(level.dimension().identifier().toString())) {
                level.resetEmptyTime();
            }
        }
    }

    /** @return 实际新登记的区块 packed 坐标（升序；空数组表示幂等无变化） */
    public static synchronized long[] addChunks(ServerLevel level, long[] packedChunks) throws IOException {
        return mutate(level, packedChunks, NO_CHUNKS);
    }

    /** @return 实际移除登记的区块 packed 坐标（升序；空数组表示幂等无变化） */
    public static synchronized long[] removeChunks(ServerLevel level, long[] packedChunks) throws IOException {
        return mutate(level, NO_CHUNKS, packedChunks);
    }

    public static synchronized long[] removeAllChunks(ServerLevel level) throws IOException {
        WeakloadRegistry activeRegistry = registry;
        if (activeRegistry == null) {
            return NO_CHUNKS.clone();
        }
        return mutate(level, NO_CHUNKS, activeRegistry.registeredChunks(level.dimension().identifier().toString()));
    }

    /** query 专用：只读登记表，不加载区块、不查询实际加载等级。 */
    public static synchronized boolean isWeakloaded(ServerLevel level, long packedChunk) {
        WeakloadRegistry activeRegistry = registry;
        if (activeRegistry == null) {
            return false;
        }
        long[] chunks = activeRegistry.registeredChunks(level.dimension().identifier().toString());
        for (long packed : chunks) {
            if (packed == packedChunk) {
                return true;
            }
        }
        return false;
    }

    public static synchronized long[] chunksIn(ServerLevel level) {
        WeakloadRegistry activeRegistry = registry;
        if (activeRegistry == null) {
            return NO_CHUNKS.clone();
        }
        return activeRegistry.registeredChunks(level.dimension().identifier().toString());
    }

    public static synchronized boolean isWriteProtected() {
        WeakloadRegistry activeRegistry = registry;
        return activeRegistry != null && activeRegistry.isWriteProtected();
    }

    private static long[] mutate(ServerLevel level, long[] addPacked, long[] removePacked) throws IOException {
        WeakloadRegistry activeRegistry = registry;
        if (activeRegistry == null) {
            throw new IOException("Weakload manager has not been initialized");
        }
        registerTicketType();
        String dimensionId = level.dimension().identifier().toString();
        return activeRegistry.mutate(dimensionId, addPacked, removePacked,
                (packed, add) -> applyTicket(level, packed, add));
    }

    private static void applyTicket(ServerLevel level, long packed, boolean add) {
        ChunkPos pos = new ChunkPos(WeakloadRegistry.unpackX(packed), WeakloadRegistry.unpackZ(packed));
        ServerChunkCache chunkSource = level.getChunkSource();
        //#if MC>=12105
        if (add) {
            chunkSource.addTicketWithRadius(weakloadTicketType, pos, WEAKLOAD_TICKET_RADIUS);
        } else {
            chunkSource.removeTicketWithRadius(weakloadTicketType, pos, WEAKLOAD_TICKET_RADIUS);
        }
        //#else
        //$$        if (add) {
        //$$            chunkSource.addRegionTicket(weakloadTicketType, pos, WEAKLOAD_TICKET_RADIUS, pos);
        //$$        } else {
        //$$            chunkSource.removeRegionTicket(weakloadTicketType, pos, WEAKLOAD_TICKET_RADIUS, pos);
        //$$        }
        //#endif
    }

    private static long[] toLongArray(List<Long> values) {
        long[] array = new long[values.size()];
        for (int index = 0; index < values.size(); index++) {
            array[index] = values.get(index);
        }
        return array;
    }
}
