package com.ice2974.carpeticeaddition.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 弱加载登记表的纯 JSON 存取层。区块以 x/z 整数对存储而不是 packed long：
 * 区块 packed 坐标会超出 JSON 双精度安全整数范围且可能为负，直接存数值会精度失真。
 * 写入走临时文件 + 原子替换；读取做严格结构校验，语法合法但结构非法的数据
 * （缺字段、null 槽位、版本不符、重复条目、非法维度 ID、区块坐标越界）一律按
 * 读取失败抛出，由上层（WeakloadRegistry）决定备份与保护策略，不会被静默
 * 当成空数据。维度 ID 只校验字符串合法性，维度当前不存在不视为非法。
 */
final class WeakloadStore {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final String MOD_DIRECTORY = "carpet-ice-addition";
    private static final String FILE_NAME = "weakload.json";
    private static final int SCHEMA_VERSION = 1;

    private WeakloadStore() {
    }

    static Path resolvePath(Path saveRoot) {
        return saveRoot.resolve(MOD_DIRECTORY).resolve(FILE_NAME);
    }

    static Map<String, long[]> read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            StoreData data = GSON.fromJson(reader, StoreData.class);
            if (data == null) {
                throw invalid(path, "no JSON object found");
            }
            if (data.version == null) {
                throw invalid(path, "missing schema version");
            }
            if (data.version != SCHEMA_VERSION) {
                throw invalid(path, "unsupported schema version " + data.version);
            }
            if (data.dimensions == null) {
                throw invalid(path, "missing dimensions");
            }
            Map<String, long[]> result = new TreeMap<>();
            for (Map.Entry<String, List<ChunkEntry>> entry : data.dimensions.entrySet()) {
                if (!isValidDimensionId(entry.getKey())) {
                    throw invalid(path, "'" + entry.getKey() + "' is not a valid dimension id");
                }
                long[] chunks = readDimension(path, entry.getKey(), entry.getValue());
                if (chunks.length > 0) {
                    result.put(entry.getKey(), chunks);
                }
            }
            return result;
        }
    }

    private static long[] readDimension(Path path, String dimensionId, List<ChunkEntry> chunks)
            throws IOException {
        if (chunks == null) {
            throw invalid(path, "dimension '" + dimensionId + "' has no chunk list");
        }
        long[] packed = new long[chunks.size()];
        for (int index = 0; index < chunks.size(); index++) {
            ChunkEntry chunk = chunks.get(index);
            if (chunk == null || chunk.x == null || chunk.z == null) {
                throw invalid(path, "dimension '" + dimensionId + "' has an incomplete chunk entry");
            }
            if (chunk.x < WeakloadRegistry.MIN_CHUNK_COORD || chunk.x > WeakloadRegistry.MAX_CHUNK_COORD
                    || chunk.z < WeakloadRegistry.MIN_CHUNK_COORD || chunk.z > WeakloadRegistry.MAX_CHUNK_COORD) {
                throw invalid(path, "dimension '" + dimensionId + "' has a chunk entry outside the world bounds");
            }
            packed[index] = WeakloadRegistry.pack(chunk.x, chunk.z);
        }
        Arrays.sort(packed);
        for (int index = 1; index < packed.length; index++) {
            if (packed[index] == packed[index - 1]) {
                throw invalid(path, "dimension '" + dimensionId + "' has duplicate chunk entries");
            }
        }
        return packed;
    }

    /**
     * 维度 ID 的字符串合法性，规则与原版 ResourceLocation 一致：至多一个冒号，
     * 命名空间字符集 [a-z0-9_.-]、路径字符集 [a-z0-9_./-]，缺省命名空间合法。
     * 只校验字符串本身——维度「当前不存在」不是非法，其记录必须保留。
     */
    private static boolean isValidDimensionId(String dimensionId) {
        int separator = dimensionId.indexOf(':');
        String namespace = separator >= 0 ? dimensionId.substring(0, separator) : "minecraft";
        String path = separator >= 0 ? dimensionId.substring(separator + 1) : dimensionId;
        if (namespace.isEmpty() || path.isEmpty() || dimensionId.indexOf(':', separator + 1) >= 0) {
            return false;
        }
        for (int index = 0; index < namespace.length(); index++) {
            if (!isNamespaceChar(namespace.charAt(index))) {
                return false;
            }
        }
        for (int index = 0; index < path.length(); index++) {
            if (!isPathChar(path.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNamespaceChar(char c) {
        return c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-';
    }

    private static boolean isPathChar(char c) {
        return isNamespaceChar(c) || c == '/';
    }

    private static IOException invalid(Path path, String detail) {
        return new IOException("Invalid weakload data in " + path + ": " + detail);
    }

    static void write(Path path, Map<String, long[]> state) throws IOException {
        try {
            Files.createDirectories(path.getParent());
            Path tempFile = path.resolveSibling(FILE_NAME + ".tmp");
            try (Writer writer = Files.newBufferedWriter(
                    tempFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                GSON.toJson(toStoreData(state), writer);
            }
            try {
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveException) {
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IOException("Failed to save weakload data to " + path, exception);
        }
    }

    /**
     * 把损坏的数据文件移出规范路径，保证后续任何写盘都不会覆盖原始损坏数据。
     * 不覆盖已有备份：默认目标已存在时改用带时间戳的唯一文件名。
     */
    static void backupCorrupted(Path path) throws IOException {
        Path backup = path.resolveSibling(FILE_NAME + ".corrupt");
        try {
            try {
                Files.move(path, backup);
            } catch (FileAlreadyExistsException alreadyExists) {
                Path unique = path.resolveSibling(FILE_NAME + ".corrupt-" + System.currentTimeMillis());
                Files.move(path, unique);
            }
        } catch (IOException exception) {
            throw new IOException("Failed to back up the corrupted weakload data file " + path, exception);
        }
    }

    private static StoreData toStoreData(Map<String, long[]> state) {
        StoreData data = new StoreData();
        data.version = SCHEMA_VERSION;
        Map<String, List<ChunkEntry>> dimensions = new LinkedHashMap<>();
        for (Map.Entry<String, long[]> entry : new TreeMap<>(state).entrySet()) {
            List<ChunkEntry> chunks = new ArrayList<>();
            for (long packed : entry.getValue()) {
                chunks.add(new ChunkEntry(
                        WeakloadRegistry.unpackX(packed),
                        WeakloadRegistry.unpackZ(packed)));
            }
            dimensions.put(entry.getKey(), chunks);
        }
        data.dimensions = dimensions;
        return data;
    }

    // 装箱且不带初始化器：字段缺失时反序列化为 null，由 read() 的严格校验拒绝，
    // 而不是被当成默认版本或空数据静默接受
    private static final class StoreData {
        private Integer version;
        private Map<String, List<ChunkEntry>> dimensions;
    }

    private static final class ChunkEntry {
        private Integer x;
        private Integer z;

        private ChunkEntry() {
        }

        private ChunkEntry(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
}
