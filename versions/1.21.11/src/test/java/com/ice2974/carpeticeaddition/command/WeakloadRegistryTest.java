package com.ice2974.carpeticeaddition.command;

import com.ice2974.carpeticeaddition.command.WeakloadRegistry.StoreIo;
import com.ice2974.carpeticeaddition.command.WeakloadRegistry.TicketOps;
import com.ice2974.carpeticeaddition.command.WeakloadRegistry.TicketApplyException;
import com.ice2974.carpeticeaddition.command.WeakloadRegistry.WriteProtectedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WeakloadRegistry 一致性状态机的纯 Java 单元测试：不触碰任何 MC 类，
 * 通过可注入的 StoreIo 与记录型 TicketOps 验证持久化失败、批量回滚、
 * 损坏恢复、写保护与维活判定的边界行为。
 */
class WeakloadRegistryTest {
    private static final String OVERWORLD = "minecraft:overworld";
    private static final String MISSING_DIM = "custom:unloaded";

    @TempDir
    Path tempDir;

    // ---- 记录型票据操作，可选在第 N 次调用或对指定区块的 add/remove 时抛出 ----
    private static final class RecordingOps implements TicketOps {
        final List<String> log = new ArrayList<>();
        int failOnCall = -1;
        long failRemoveOn = Long.MIN_VALUE;
        long failAddOn = Long.MIN_VALUE;
        RuntimeException failure = new IllegalStateException("injected ticket failure");

        @Override
        public void apply(long packedChunkPos, boolean add) {
            if (log.size() + 1 == failOnCall
                    || (add && packedChunkPos == failAddOn)
                    || (!add && packedChunkPos == failRemoveOn)) {
                log.add(label(packedChunkPos, add) + ":FAIL");
                throw failure;
            }
            log.add(label(packedChunkPos, add));
        }

        private static String label(long packed, boolean add) {
            return (add ? "add:" : "remove:") + WeakloadRegistry.unpackX(packed) + "," + WeakloadRegistry.unpackZ(packed);
        }
    }

    // ---- 内存 StoreIo，可注入读写与备份失败 ----
    private static final class MemoryIo implements StoreIo {
        Map<String, long[]> disk;
        final List<Map<String, long[]>> writes = new ArrayList<>();
        boolean exists = true;
        boolean failAllWrites;
        boolean failRestoreWrite;
        boolean failRead;
        boolean failBackup;

        @Override
        public boolean exists() {
            return exists;
        }

        @Override
        public Map<String, long[]> read() throws IOException {
            if (failRead) {
                throw new IOException("corrupted");
            }
            return copy(disk);
        }

        @Override
        public void write(Map<String, long[]> state) throws IOException {
            if (failAllWrites || (failRestoreWrite && !writes.isEmpty())) {
                throw new IOException("write failed");
            }
            writes.add(copy(state));
            disk = copy(state);
        }

        @Override
        public void backupCorrupted() throws IOException {
            if (failBackup) {
                throw new IOException("backup failed");
            }
            exists = false;
        }
    }

    private static Map<String, long[]> copy(Map<String, long[]> state) {
        Map<String, long[]> result = new LinkedHashMap<>();
        for (Map.Entry<String, long[]> entry : state.entrySet()) {
            result.put(entry.getKey(), entry.getValue().clone());
        }
        return result;
    }

    private static Map<String, long[]> state(Object... dimensionAndChunks) {
        Map<String, long[]> result = new LinkedHashMap<>();
        for (int index = 0; index < dimensionAndChunks.length; index += 2) {
            result.put((String) dimensionAndChunks[index], (long[]) dimensionAndChunks[index + 1]);
        }
        return result;
    }

    private static long chunk(int x, int z) {
        return WeakloadRegistry.pack(x, z);
    }

    @Test
    void mutatePersistsAppliesTicketsAndReturnsChangedChunks() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = new LinkedHashMap<>();
        io.exists = false;
        WeakloadRegistry registry = new WeakloadRegistry(io);
        RecordingOps ops = new RecordingOps();

        long[] changed = registry.mutate(OVERWORLD, new long[] {chunk(0, 0), chunk(-3, 7), chunk(0, 0)}, new long[0], ops);

        assertArrayEquals(new long[] {chunk(-3, 7), chunk(0, 0)}, changed);
        assertArrayEquals(new long[] {chunk(-3, 7), chunk(0, 0)}, registry.registeredChunks(OVERWORLD));
        assertTrue(registry.hasActiveTickets(OVERWORLD));
        assertEquals(1, io.writes.size());
        // desired 是按 packed 升序的 TreeSet，负坐标区块排在最前
        assertEquals(List.of("add:-3,7", "add:0,0"), ops.log);
    }

    @Test
    void idempotentAddSkipsPersistenceAndTickets() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = state(OVERWORLD, new long[] {chunk(5, 5)});
        WeakloadRegistry registry = new WeakloadRegistry(io);
        assertArrayEquals(new long[0], registry.mutate(OVERWORLD, new long[] {chunk(5, 5)}, new long[0], new RecordingOps()));
        assertTrue(io.writes.isEmpty());
    }

    @Test
    void saveFailureLeavesEverythingUnchanged() {
        MemoryIo io = new MemoryIo();
        io.disk = state(OVERWORLD, new long[] {chunk(1, 1)});
        io.failAllWrites = true;
        WeakloadRegistry registry = new WeakloadRegistry(io);
        RecordingOps ops = new RecordingOps();

        assertThrows(IOException.class,
                () -> registry.mutate(OVERWORLD, new long[] {chunk(2, 2)}, new long[0], ops));
        assertArrayEquals(new long[] {chunk(1, 1)}, registry.registeredChunks(OVERWORLD));
        assertFalse(registry.hasActiveTickets(OVERWORLD));
        assertTrue(ops.log.isEmpty());
    }

    @Test
    void batchTicketFailureRollsBackAndRestoresPreviousState() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = state(OVERWORLD, new long[] {chunk(1, 1)});
        WeakloadRegistry registry = new WeakloadRegistry(io);
        RecordingOps ops = new RecordingOps();
        ops.failOnCall = 3;

        IOException exception = assertThrows(IOException.class,
                () -> registry.mutate(OVERWORLD, new long[] {chunk(2, 2), chunk(3, 3)}, new long[0], ops));

        assertInstanceOf(TicketApplyException.class, exception);
        // 票据序：补恢复既有登记 add:1,1、add:2,2 → 失败点 add:3,3 → 回滚前两个
        assertEquals(List.of("add:1,1", "add:2,2", "add:3,3:FAIL", "remove:1,1", "remove:2,2"), ops.log);
        // 回滚后旧状态已回写（最后一次写是旧状态），内存登记未变化
        assertArrayEquals(new long[] {chunk(1, 1)}, io.writes.get(io.writes.size() - 1).get(OVERWORLD));
        assertArrayEquals(new long[] {chunk(1, 1)}, registry.registeredChunks(OVERWORLD));
        assertFalse(registry.hasActiveTickets(OVERWORLD));
        assertFalse(registry.isWriteProtected());
    }

    @Test
    void rollbackRestoreFailureEntersProtectionMode() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = state(OVERWORLD, new long[] {chunk(1, 1)});
        io.failRestoreWrite = true;
        WeakloadRegistry registry = new WeakloadRegistry(io);
        RecordingOps ops = new RecordingOps();
        ops.failOnCall = 2;

        assertThrows(WriteProtectedException.class,
                () -> registry.mutate(OVERWORLD, new long[] {chunk(2, 2)}, new long[0], ops));
        assertTrue(registry.isWriteProtected());
        // 保护模式下禁止后续一切变更
        assertThrows(WriteProtectedException.class,
                () -> registry.mutate(OVERWORLD, new long[] {chunk(9, 9)}, new long[0], new RecordingOps()));
        assertArrayEquals(new long[] {chunk(1, 1)}, registry.registeredChunks(OVERWORLD));
    }

    @Test
    void incompleteRollbackOfAddsEntersProtectionMode() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = state(OVERWORLD, new long[] {chunk(1, 1)});
        WeakloadRegistry registry = new WeakloadRegistry(io);
        RecordingOps ops = new RecordingOps();
        ops.failOnCall = 3;              // 前向在补恢复 add:3,3 时失败
        ops.failRemoveOn = chunk(2, 2);  // 回滚逆操作 remove:2,2 再失败

        // 不得报告为 TicketApplyException（其语义是「已完整回滚」）
        assertThrows(WriteProtectedException.class,
                () -> registry.mutate(OVERWORLD, new long[] {chunk(2, 2), chunk(3, 3)}, new long[0], ops));
        assertTrue(registry.isWriteProtected());
        // 单点逆操作失败后仍继续处理其余逆操作
        assertEquals(List.of("add:1,1", "add:2,2", "add:3,3:FAIL", "remove:1,1", "remove:2,2:FAIL"), ops.log);
        // 旧状态仍被尽力回写（最后一次写是旧状态），内存登记未变化
        assertArrayEquals(new long[] {chunk(1, 1)}, io.writes.get(io.writes.size() - 1).get(OVERWORLD));
        assertArrayEquals(new long[] {chunk(1, 1)}, registry.registeredChunks(OVERWORLD));
        // 保护模式下禁止后续变更
        assertThrows(WriteProtectedException.class,
                () -> registry.mutate(OVERWORLD, new long[] {chunk(9, 9)}, new long[0], new RecordingOps()));
    }

    @Test
    void incompleteRollbackOfRemovalsEntersProtectionMode() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = new LinkedHashMap<>();
        io.exists = false;
        WeakloadRegistry registry = new WeakloadRegistry(io);
        registry.mutate(OVERWORLD, new long[] {chunk(1, 1), chunk(2, 2)}, new long[0], new RecordingOps());

        RecordingOps ops = new RecordingOps();
        ops.failOnCall = 2;          // 前向第二个 remove:2,2 失败
        ops.failAddOn = chunk(1, 1); // 回滚逆操作补回 add:1,1 再失败

        assertThrows(WriteProtectedException.class,
                () -> registry.mutate(OVERWORLD, new long[0], new long[] {chunk(1, 1), chunk(2, 2)}, ops));
        assertTrue(registry.isWriteProtected());
        assertEquals(List.of("remove:1,1", "remove:2,2:FAIL", "add:1,1:FAIL"), ops.log);
        assertArrayEquals(new long[] {chunk(1, 1), chunk(2, 2)}, registry.registeredChunks(OVERWORLD));
        assertArrayEquals(new long[] {chunk(1, 1), chunk(2, 2)},
                io.writes.get(io.writes.size() - 1).get(OVERWORLD));
    }

    @Test
    void removingLastChunkStopsKeepAlive() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = new LinkedHashMap<>();
        io.exists = false;
        WeakloadRegistry registry = new WeakloadRegistry(io);
        registry.mutate(OVERWORLD, new long[] {chunk(0, 0)}, new long[0], new RecordingOps());
        assertTrue(registry.hasActiveTickets(OVERWORLD));

        long[] changed = registry.mutate(OVERWORLD, new long[0], new long[] {chunk(0, 0)}, new RecordingOps());

        assertArrayEquals(new long[] {chunk(0, 0)}, changed);
        assertFalse(registry.hasActiveTickets(OVERWORLD));
        assertEquals(0, registry.registeredChunks(OVERWORLD).length);
    }

    @Test
    void recordsOfUnloadedDimensionsSurviveOtherMutations() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = state(MISSING_DIM, new long[] {chunk(4, 4)}, OVERWORLD, new long[] {chunk(1, 1)});
        WeakloadRegistry registry = new WeakloadRegistry(io);
        assertFalse(registry.hasActiveTickets(MISSING_DIM));

        registry.mutate(OVERWORLD, new long[] {chunk(2, 2)}, new long[0], new RecordingOps());

        assertArrayEquals(new long[] {chunk(4, 4)}, registry.registeredChunks(MISSING_DIM));
        assertArrayEquals(new long[] {chunk(4, 4)}, io.writes.get(0).get(MISSING_DIM));
        assertFalse(registry.hasActiveTickets(MISSING_DIM));
    }

    @Test
    void corruptedRealFileIsBackedUpAndContinuesFresh() throws IOException {
        Path file = tempDir.resolve("weakload.json");
        String corrupted = "{ not valid json";
        Files.createDirectories(file.getParent());
        Files.writeString(file, corrupted, StandardCharsets.UTF_8);

        WeakloadRegistry registry = new WeakloadRegistry(file);

        assertEquals(0, registry.registeredChunks(OVERWORLD).length);
        assertFalse(registry.isWriteProtected());
        assertFalse(Files.exists(file), "original file must be moved away");
        Path backup = tempDir.resolve("weakload.json.corrupt");
        assertTrue(Files.exists(backup), "corrupt backup must exist");
        assertEquals(corrupted, Files.readString(backup, StandardCharsets.UTF_8), "backup must keep original bytes");

        registry.mutate(OVERWORLD, new long[] {chunk(1, 2)}, new long[0], new RecordingOps());
        assertArrayEquals(new long[] {chunk(1, 2)}, WeakloadStore.read(file).get(OVERWORLD));
        assertEquals(corrupted, Files.readString(backup, StandardCharsets.UTF_8), "backup must stay untouched");
    }

    @Test
    void structurallyInvalidFileIsBackedUpAndContinuesFresh() throws IOException {
        Path file = tempDir.resolve("weakload.json");
        // 语法合法但结构非法：维度列表为 null，不得被静默当成空数据
        String invalid = "{\"version\":1,\"dimensions\":{\"minecraft:overworld\":null}}";
        Files.createDirectories(file.getParent());
        Files.writeString(file, invalid, StandardCharsets.UTF_8);

        WeakloadRegistry registry = new WeakloadRegistry(file);

        assertFalse(registry.isWriteProtected());
        assertEquals(0, registry.registeredChunks(OVERWORLD).length);
        assertFalse(Files.exists(file), "original file must be moved away");
        assertEquals(invalid, Files.readString(tempDir.resolve("weakload.json.corrupt"),
                StandardCharsets.UTF_8), "backup must keep original bytes");
    }

    @Test
    void corruptBackupFailureEntersProtectionMode() {
        MemoryIo io = new MemoryIo();
        io.failRead = true;
        io.failBackup = true;
        WeakloadRegistry registry = new WeakloadRegistry(io);

        assertTrue(registry.isWriteProtected());
        assertEquals(0, registry.registeredChunks(OVERWORLD).length);
        assertThrows(WriteProtectedException.class,
                () -> registry.mutate(OVERWORLD, new long[] {chunk(1, 1)}, new long[0], new RecordingOps()));
    }

    @Test
    void missingFileStartsEmpty() {
        MemoryIo io = new MemoryIo();
        io.exists = false;
        io.disk = null;
        WeakloadRegistry registry = new WeakloadRegistry(io);

        assertEquals(0, registry.registeredChunks(OVERWORLD).length);
        assertFalse(registry.isWriteProtected());
        assertFalse(registry.hasActiveTickets(OVERWORLD));
    }

    @Test
    void packRoundTripsNegativeAndLargeCoordinates() {
        for (int[] coordinate : new int[][] {{0, 0}, {-1, 0}, {0, -1}, {-1875000, 1874999}, {123456, -654321}}) {
            long packed = WeakloadRegistry.pack(coordinate[0], coordinate[1]);
            assertEquals(coordinate[0], WeakloadRegistry.unpackX(packed));
            assertEquals(coordinate[1], WeakloadRegistry.unpackZ(packed));
        }
    }

    @Test
    void removalChangedSetUsesSymmetricDifference() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = state(OVERWORLD, new long[] {chunk(1, 1), chunk(2, 2), chunk(3, 3)});
        WeakloadRegistry registry = new WeakloadRegistry(io);

        long[] changed = registry.mutate(OVERWORLD, new long[] {chunk(4, 4)}, new long[] {chunk(2, 2), chunk(3, 3), chunk(9, 9)},
                new RecordingOps());

        assertArrayEquals(new long[] {chunk(2, 2), chunk(3, 3), chunk(4, 4)}, changed);
        assertArrayEquals(new long[] {chunk(1, 1), chunk(4, 4)}, registry.registeredChunks(OVERWORLD));
        assertArrayEquals(new long[] {chunk(1, 1), chunk(4, 4)}, io.writes.get(0).get(OVERWORLD));
    }

    @Test
    void changedSetReportsOnlyActuallyChangedChunks() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = state(OVERWORLD, new long[] {chunk(2, 2)});
        WeakloadRegistry registry = new WeakloadRegistry(io);

        // 范围 [(1,1)..(2,2)] 四区块中仅 (2,2) 已登记 → 变更集为其余三者，不含 (2,2)
        long[] changed = registry.mutate(OVERWORLD,
                new long[] {chunk(1, 1), chunk(1, 2), chunk(2, 1), chunk(2, 2)}, new long[0], new RecordingOps());
        assertArrayEquals(new long[] {chunk(1, 1), chunk(1, 2), chunk(2, 1)}, changed);

        // 删除方向同理：(3,3) 未登记，不出现在变更集中
        long[] removed = registry.mutate(OVERWORLD, new long[0],
                new long[] {chunk(2, 1), chunk(2, 2), chunk(3, 3)}, new RecordingOps());
        assertArrayEquals(new long[] {chunk(2, 1), chunk(2, 2)}, removed);
    }

    @Test
    void sameChunkInBothDirectionsReportsNoNetChange() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = state(OVERWORLD, new long[] {chunk(1, 1), chunk(2, 2)});
        WeakloadRegistry registry = new WeakloadRegistry(io);
        RecordingOps ops = new RecordingOps();

        // (1,1) 已登记且同时出现在 add/remove：净效果为零，不算变更、不写盘、不动票据
        long[] changed = registry.mutate(OVERWORLD, new long[] {chunk(1, 1)}, new long[] {chunk(1, 1)}, ops);

        assertArrayEquals(new long[0], changed);
        assertTrue(io.writes.isEmpty());
        assertTrue(ops.log.isEmpty());
        assertArrayEquals(new long[] {chunk(1, 1), chunk(2, 2)}, registry.registeredChunks(OVERWORLD));
    }

    @Test
    void sameChunkInBothDirectionsStillRegistersWhenAbsent() throws IOException {
        MemoryIo io = new MemoryIo();
        io.disk = new LinkedHashMap<>();
        io.exists = false;
        WeakloadRegistry registry = new WeakloadRegistry(io);

        // (2,2) 未登记：remove 是空操作，最终效果是登记，变更集恰为该区块
        long[] changed = registry.mutate(OVERWORLD, new long[] {chunk(2, 2)}, new long[] {chunk(2, 2)},
                new RecordingOps());

        assertArrayEquals(new long[] {chunk(2, 2)}, changed);
        assertArrayEquals(new long[] {chunk(2, 2)}, registry.registeredChunks(OVERWORLD));
    }
}
