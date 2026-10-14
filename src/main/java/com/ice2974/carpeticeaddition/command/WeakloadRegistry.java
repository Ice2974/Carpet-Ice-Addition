package com.ice2974.carpeticeaddition.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 弱加载登记表的一致性核心。持久化视图（registered，含未加载维度的记录）与运行时
 * 票据视图（ticketed，仅实际持有票据的维度）分离：保存任一维度的变更都不会静默删除
 * 未加载维度的记录，维度维活判定只看 ticketed。
 *
 * <p>变更流程固定为「副本计算 → 原子持久化 → 票据差分 → 状态交换」：持久化失败时
 * 票据与内存零变更（整批回滚）；票据应用意外失败时回滚本批票据并回写旧 JSON，回写
 * 再失败则进入写保护模式，拒绝后续一切变更，禁止不一致继续扩大。
 */
final class WeakloadRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(WeakloadRegistry.class);
    private static final long[] EMPTY_CHUNKS = new long[0];

    /** 变更被保护模式拒绝（损坏文件无法迁移或回滚后旧数据恢复失败）。 */
    static final class WriteProtectedException extends IOException {
        WriteProtectedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** 票据应用失败，本批已回滚且旧 JSON 已恢复。 */
    static final class TicketApplyException extends IOException {
        TicketApplyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final StoreIo store;
    private Map<String, long[]> registered = new TreeMap<>();
    private Map<String, long[]> ticketed = new TreeMap<>();
    private boolean writeProtected;

    WeakloadRegistry(Path filePath) {
        this(fileIo(filePath));
    }

    WeakloadRegistry(StoreIo store) {
        this.store = store;
        loadOrRecover();
    }

    /** 票据应用回调：按 add 语义对单个 packed 区块坐标加票或删票。 */
    interface TicketOps {
        void apply(long packedChunkPos, boolean add);
    }

    /** 存取抽象，测试注入失败用。 */
    interface StoreIo {
        boolean exists();

        Map<String, long[]> read() throws Exception;

        void write(Map<String, long[]> state) throws IOException;

        void backupCorrupted() throws IOException;
    }

    static StoreIo fileIo(Path filePath) {
        return new StoreIo() {
            @Override
            public boolean exists() {
                return Files.exists(filePath);
            }

            @Override
            public Map<String, long[]> read() throws IOException {
                return WeakloadStore.read(filePath);
            }

            @Override
            public void write(Map<String, long[]> state) throws IOException {
                WeakloadStore.write(filePath, state);
            }

            @Override
            public void backupCorrupted() throws IOException {
                WeakloadStore.backupCorrupted(filePath);
            }
        };
    }

    static long pack(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | ((long) chunkZ & 0xFFFFFFFFL);
    }

    static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    static int unpackZ(long packed) {
        return (int) packed;
    }

    /** 持久化视图：该维度登记的全部区块（含尚未恢复票据的记录），按 packed 升序。 */
    synchronized long[] registeredChunks(String dimensionId) {
        long[] chunks = registered.get(dimensionId);
        return chunks == null ? EMPTY_CHUNKS.clone() : chunks.clone();
    }

    /** 维活判定：该维度当前是否实际持有至少一张弱加载票据。 */
    synchronized boolean hasActiveTickets(String dimensionId) {
        long[] chunks = ticketed.get(dimensionId);
        return chunks != null && chunks.length > 0;
    }

    synchronized boolean isWriteProtected() {
        return writeProtected;
    }

    /** 恢复路径专用：世界加载后登记已实际应用的票据。 */
    synchronized void markTicketed(String dimensionId, long[] appliedChunks) {
        if (appliedChunks.length == 0) {
            ticketed.remove(dimensionId);
        } else {
            ticketed.put(dimensionId, appliedChunks.clone());
        }
    }

    /**
     * 一致性变更入口。
     *
     * @return 登记表实际发生变化的区块数（0 表示幂等无变化，不写盘也不动票据）
     * @throws IOException 持久化失败（零变更）或票据应用失败（已回滚）
     */
    synchronized int mutate(String dimensionId, long[] addPacked, long[] removePacked, TicketOps ops)
            throws IOException {
        if (writeProtected) {
            throw new WriteProtectedException("Weakload data is write protected", null);
        }

        TreeSet<Long> desired = new TreeSet<>();
        for (long packed : registered.getOrDefault(dimensionId, EMPTY_CHUNKS)) {
            desired.add(packed);
        }
        int removed = 0;
        for (long packed : removePacked) {
            if (desired.remove(packed)) {
                removed++;
            }
        }
        int added = 0;
        for (long packed : addPacked) {
            if (desired.add(packed)) {
                added++;
            }
        }
        int changed = added + removed;
        if (changed == 0) {
            return 0;
        }

        Map<String, long[]> newState = copyState(registered);
        if (desired.isEmpty()) {
            newState.remove(dimensionId);
        } else {
            newState.put(dimensionId, toArray(desired));
        }
        try {
            store.write(newState);
        } catch (IOException exception) {
            LOGGER.error("Failed to persist weakload data, no tickets or state were changed", exception);
            throw exception;
        }

        TreeSet<Long> currentTickets = new TreeSet<>();
        for (long packed : ticketed.getOrDefault(dimensionId, EMPTY_CHUNKS)) {
            currentTickets.add(packed);
        }
        List<Long> toApply = new ArrayList<>();
        List<Long> toRetract = new ArrayList<>();
        for (long packed : desired) {
            if (!currentTickets.contains(packed)) {
                toApply.add(packed);
            }
        }
        for (long packed : currentTickets) {
            if (!desired.contains(packed)) {
                toRetract.add(packed);
            }
        }

        List<Long> appliedAdds = new ArrayList<>();
        List<Long> appliedRemoves = new ArrayList<>();
        try {
            for (long packed : toApply) {
                ops.apply(packed, true);
                appliedAdds.add(packed);
            }
            for (long packed : toRetract) {
                ops.apply(packed, false);
                appliedRemoves.add(packed);
            }
        } catch (Throwable ticketFailure) {
            LOGGER.error("Failed to apply weakload tickets for dimension {}, rolling back the batch",
                    dimensionId, ticketFailure);
            rollbackTickets(ops, appliedAdds, appliedRemoves);
            try {
                store.write(registered);
            } catch (IOException restoreFailure) {
                writeProtected = true;
                LOGGER.error("Failed to restore the previous weakload data after a ticket rollback. "
                        + "Weakload data is now write protected; fix the world files and restart the server. "
                        + "Ticket failure was:", ticketFailure);
                LOGGER.error("...and the data restore failure was:", restoreFailure);
                throw new WriteProtectedException("Weakload data entered write protection mode", restoreFailure);
            }
            throw new TicketApplyException("Failed to apply weakload tickets, the change was rolled back",
                    ticketFailure);
        }

        registered = newState;
        if (desired.isEmpty()) {
            ticketed.remove(dimensionId);
        } else {
            ticketed.put(dimensionId, toArray(desired));
        }
        return changed;
    }

    private static void rollbackTickets(TicketOps ops, List<Long> appliedAdds, List<Long> appliedRemoves) {
        for (long packed : appliedAdds) {
            try {
                ops.apply(packed, false);
            } catch (Throwable rollbackFailure) {
                LOGGER.error("Failed to roll back weakload ticket for chunk [{}, {}]",
                        unpackX(packed), unpackZ(packed), rollbackFailure);
            }
        }
        for (long packed : appliedRemoves) {
            try {
                ops.apply(packed, true);
            } catch (Throwable rollbackFailure) {
                LOGGER.error("Failed to roll back weakload ticket for chunk [{}, {}]",
                        unpackX(packed), unpackZ(packed), rollbackFailure);
            }
        }
    }

    private void loadOrRecover() {
        if (!store.exists()) {
            registered = new TreeMap<>();
            ticketed = new TreeMap<>();
            return;
        }
        try {
            registered = store.read();
            ticketed = new TreeMap<>();
            return;
        } catch (Exception exception) {
            LOGGER.error("Failed to read weakload data, the file is corrupted or unreadable", exception);
        }
        registered = new TreeMap<>();
        ticketed = new TreeMap<>();
        try {
            store.backupCorrupted();
            LOGGER.warn("The corrupted weakload data file was moved aside; continuing with an empty registry");
        } catch (IOException backupFailure) {
            writeProtected = true;
            LOGGER.error("Failed to move the corrupted weakload data file aside. Weakload data is now "
                    + "write protected: the corrupted file is kept untouched and mutations are rejected "
                    + "until the world files are fixed and the server restarted", backupFailure);
        }
    }

    private static Map<String, long[]> copyState(Map<String, long[]> state) {
        Map<String, long[]> copy = new TreeMap<>();
        for (Map.Entry<String, long[]> entry : state.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().clone());
        }
        return copy;
    }

    private static long[] toArray(TreeSet<Long> set) {
        long[] array = new long[set.size()];
        int index = 0;
        for (long value : set) {
            array[index++] = value;
        }
        return array;
    }
}
