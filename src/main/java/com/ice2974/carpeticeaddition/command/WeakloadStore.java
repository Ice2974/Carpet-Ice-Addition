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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 弱加载登记表的纯 JSON 存取层。区块以 x/z 整数对存储而不是 packed long：
 * 区块 packed 坐标会超出 JSON 双精度安全整数范围且可能为负，直接存数值会精度失真。
 * 写入走临时文件 + 原子替换；读取失败由上层（WeakloadRegistry）决定备份与保护策略。
 */
final class WeakloadStore {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final String MOD_DIRECTORY = "carpet-ice-addition";
    private static final String FILE_NAME = "weakload.json";

    private WeakloadStore() {
    }

    static Path resolvePath(Path saveRoot) {
        return saveRoot.resolve(MOD_DIRECTORY).resolve(FILE_NAME);
    }

    static Map<String, long[]> read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            StoreData data = GSON.fromJson(reader, StoreData.class);
            Map<String, long[]> result = new TreeMap<>();
            if (data != null && data.dimensions != null) {
                for (Map.Entry<String, List<ChunkEntry>> entry : data.dimensions.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }
                    long[] chunks = entry.getValue().stream()
                            .filter(chunk -> chunk != null)
                            .mapToLong(chunk -> WeakloadRegistry.pack(chunk.x, chunk.z))
                            .sorted()
                            .toArray();
                    if (chunks.length > 0) {
                        result.put(entry.getKey(), chunks);
                    }
                }
            }
            return result;
        }
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

    private static final class StoreData {
        @SuppressWarnings("unused") // 反射序列化字段，用于 weakload.json 的 schema 版本标记
        private int version = 1;
        private Map<String, List<ChunkEntry>> dimensions = new LinkedHashMap<>();
    }

    private static final class ChunkEntry {
        private int x;
        private int z;

        private ChunkEntry() {
        }

        private ChunkEntry(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
}
