package com.ice2974.carpeticeaddition.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * WeakloadStore 严格结构校验的单元测试：语法合法但结构非法的 JSON 必须作为
 * 读取失败抛出（进入上层的损坏备份与写保护流程），而不是被静默当成空数据。
 */
class WeakloadStoreTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @TempDir
    Path tempDir;

    private Map<String, long[]> read(String json) throws IOException {
        Path file = tempDir.resolve("weakload.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return WeakloadStore.read(file);
    }

    @Test
    void nullDocumentIsRejected() {
        assertThrows(IOException.class, () -> read("null"));
    }

    @Test
    void missingVersionIsRejected() {
        assertThrows(IOException.class, () -> read("{\"dimensions\":{}}"));
    }

    @Test
    void unsupportedVersionIsRejected() {
        assertThrows(IOException.class, () -> read("{\"version\":2,\"dimensions\":{}}"));
    }

    @Test
    void missingDimensionsIsRejected() {
        assertThrows(IOException.class, () -> read("{\"version\":1}"));
    }

    @Test
    void nullDimensionsIsRejected() {
        assertThrows(IOException.class, () -> read("{\"version\":1,\"dimensions\":null}"));
    }

    @Test
    void nullDimensionListIsRejected() {
        assertThrows(IOException.class, () -> read("{\"version\":1,\"dimensions\":{\"" + OVERWORLD + "\":null}}"));
    }

    @Test
    void chunkEntryMissingCoordinatesIsRejected() {
        assertThrows(IOException.class, () -> read(
                "{\"version\":1,\"dimensions\":{\"" + OVERWORLD + "\":[{\"x\":1}]}}"));
        assertThrows(IOException.class, () -> read(
                "{\"version\":1,\"dimensions\":{\"" + OVERWORLD + "\":[{\"z\":1}]}}"));
        assertThrows(IOException.class, () -> read(
                "{\"version\":1,\"dimensions\":{\"" + OVERWORLD + "\":[null]}}"));
    }

    @Test
    void duplicateChunksAreRejected() {
        assertThrows(IOException.class, () -> read(
                "{\"version\":1,\"dimensions\":{\"" + OVERWORLD + "\":[{\"x\":1,\"z\":2},{\"x\":1,\"z\":2}]}}"));
    }

    @Test
    void invalidDimensionIdsAreRejected() {
        assertThrows(IOException.class, () -> read(chunkJson("Overworld:x", 0, 0)));
        assertThrows(IOException.class, () -> read(chunkJson("a:b:c", 0, 0)));
        assertThrows(IOException.class, () -> read(chunkJson("", 0, 0)));
        assertThrows(IOException.class, () -> read(chunkJson(":overworld", 0, 0)));
        assertThrows(IOException.class, () -> read(chunkJson("minecraft:", 0, 0)));
        assertThrows(IOException.class, () -> read(chunkJson("min ecraft:overworld", 0, 0)));
        assertThrows(IOException.class, () -> read(chunkJson("minecraft/over:world", 0, 0)));
    }

    @Test
    void legalButUnloadedDimensionIdsAreKept() throws IOException {
        // 维度「当前不存在」不是 ID 非法：记录必须原样保留；缺省命名空间的裸形式也合法
        Map<String, long[]> parsed = read("{\"version\":1,\"dimensions\":{"
                + "\"custom:unloaded\":[{\"x\":4,\"z\":4}],"
                + "\"overworld\":[{\"x\":0,\"z\":0}]}}");
        assertArrayEquals(new long[] {WeakloadRegistry.pack(4, 4)}, parsed.get("custom:unloaded"));
        assertArrayEquals(new long[] {WeakloadRegistry.pack(0, 0)}, parsed.get("overworld"));
    }

    @Test
    void outOfRangeChunkCoordinatesAreRejected() {
        assertThrows(IOException.class, () -> read(chunkJson(OVERWORLD, 1875000, 0)));
        assertThrows(IOException.class, () -> read(chunkJson(OVERWORLD, -1875001, 0)));
        assertThrows(IOException.class, () -> read(chunkJson(OVERWORLD, 0, 1875000)));
        assertThrows(IOException.class, () -> read(chunkJson(OVERWORLD, 0, -1875001)));
    }

    @Test
    void boundaryChunkCoordinatesAreAccepted() throws IOException {
        Map<String, long[]> result = read(chunkJson(OVERWORLD, -1875000, 1874999));

        assertArrayEquals(new long[] {WeakloadRegistry.pack(-1875000, 1874999)}, result.get(OVERWORLD));
    }

    private static String chunkJson(String dimensionId, int x, int z) {
        return "{\"version\":1,\"dimensions\":{\"" + dimensionId + "\":[{\"x\":" + x + ",\"z\":" + z + "}]}}";
    }

    @Test
    void validFileParsesWithSortedPackedChunks() throws IOException {
        Map<String, long[]> result = read(
                "{\"version\":1,\"dimensions\":{\"" + OVERWORLD + "\":[{\"x\":5,\"z\":-7},{\"x\":-3,\"z\":0}]}}");

        assertArrayEquals(new long[] {WeakloadRegistry.pack(-3, 0), WeakloadRegistry.pack(5, -7)},
                result.get(OVERWORLD));
    }

    @Test
    void emptyDimensionListIsAllowed() throws IOException {
        Map<String, long[]> result = read("{\"version\":1,\"dimensions\":{\"" + OVERWORLD + "\":[]}}");

        assertNull(result.get(OVERWORLD));
    }
}
