package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NeutralPhantomsWatermark 的持久化与 false 边界推进单测（core 平台）。
 *
 * <p>锁定关键语义：首建基线（当前规则 true → 0 / false → 非零）；水位只在「进入
 * false」时推进（false→true 不推进、同 tick true→false→true 恰好推进一次）；
 * round-trip（shutdown 后 initialize 从文件恢复）；离线 conf true→false 由
 * initialize reconcile 补上；conf 加载阶段的 observer 调用（未初始化）安全 no-op；
 * 损坏 / 缺字段文件按首建回退。
 */
class NeutralPhantomsWatermarkTest {

    @AfterEach
    void resetState() {
        NeutralPhantomsWatermark.shutdown();
    }

    private static Path watermarkFile(Path worldRoot) {
        return worldRoot.resolve("carpet-ice-addition").resolve("neutral_phantoms_watermark.json");
    }

    private static void writeWatermarkFile(Path worldRoot, long watermark, boolean lastEnabled) throws IOException {
        Files.createDirectories(watermarkFile(worldRoot).getParent());
        Files.writeString(watermarkFile(worldRoot),
                "{\"watermark\": " + watermark + ", \"lastEnabled\": " + lastEnabled + "}",
                StandardCharsets.UTF_8);
    }

    // ---- 首建基线 ----

    @Test
    void freshFileWithRuleEnabledStartsAtZero(@TempDir Path worldRoot) {
        NeutralPhantomsWatermark.initialize(worldRoot, true);
        assertEquals(0L, NeutralPhantomsWatermark.current());
        assertTrue(Files.isRegularFile(watermarkFile(worldRoot)));
    }

    @Test
    void freshFileWithRuleDisabledStartsNonZero(@TempDir Path worldRoot) {
        // 旧存档 retaliation NBT 缺失水位键按 0 读：非零基线使其立即失配失效
        NeutralPhantomsWatermark.initialize(worldRoot, false);
        assertFalse(NeutralPhantomsWatermark.current() == 0L);
    }

    // ---- 运行期 observer：仅进入 false 推进 ----

    @Test
    void enteringFalseAdvancesWatermark(@TempDir Path worldRoot) {
        NeutralPhantomsWatermark.initialize(worldRoot, true);
        NeutralPhantomsWatermark.onRuleChanged(false);
        assertEquals(1L, NeutralPhantomsWatermark.current());
    }

    @Test
    void leavingFalseDoesNotAdvance(@TempDir Path worldRoot) {
        NeutralPhantomsWatermark.initialize(worldRoot, true);
        NeutralPhantomsWatermark.onRuleChanged(false);
        NeutralPhantomsWatermark.onRuleChanged(true);
        assertEquals(1L, NeutralPhantomsWatermark.current());
    }

    @Test
    void sameTickFalseTrueRoundTripAdvancesOnce(@TempDir Path worldRoot) {
        NeutralPhantomsWatermark.initialize(worldRoot, true);
        // 命令方块同 tick 双命令：进入 false 的那次 observer 即推进，随后开启不回退
        NeutralPhantomsWatermark.onRuleChanged(false);
        NeutralPhantomsWatermark.onRuleChanged(true);
        assertEquals(1L, NeutralPhantomsWatermark.current());
    }

    // ---- round-trip：shutdown 后 initialize 从文件恢复 ----

    @Test
    void watermarkSurvivesShutdownAndReinitialize(@TempDir Path worldRoot) {
        NeutralPhantomsWatermark.initialize(worldRoot, true);
        NeutralPhantomsWatermark.onRuleChanged(false);
        NeutralPhantomsWatermark.onRuleChanged(true);
        NeutralPhantomsWatermark.shutdown();
        // conf=true 的正常重启：false→true 同步不推进，水位保持（合法 retaliation 跨重启保留）
        NeutralPhantomsWatermark.initialize(worldRoot, true);
        assertEquals(1L, NeutralPhantomsWatermark.current());
    }

    @Test
    void runtimeAdvancePersistedAcrossRestartWithConfFalse(@TempDir Path worldRoot) {
        NeutralPhantomsWatermark.initialize(worldRoot, true);
        NeutralPhantomsWatermark.onRuleChanged(false);
        NeutralPhantomsWatermark.shutdown();
        // conf=false 的正常重启：内存 false == conf false，无变化；水位已持久化
        NeutralPhantomsWatermark.initialize(worldRoot, false);
        assertEquals(1L, NeutralPhantomsWatermark.current());
    }

    // ---- 离线 reconcile（服务器关闭期间直接修改 conf）----

    @Test
    void offlineTrueToFalseAdvancesOnInitialize(@TempDir Path worldRoot) throws IOException {
        writeWatermarkFile(worldRoot, 3L, true);
        NeutralPhantomsWatermark.initialize(worldRoot, false);
        assertEquals(4L, NeutralPhantomsWatermark.current());
    }

    @Test
    void offlineFalseToTrueDoesNotAdvance(@TempDir Path worldRoot) throws IOException {
        writeWatermarkFile(worldRoot, 3L, false);
        NeutralPhantomsWatermark.initialize(worldRoot, true);
        assertEquals(3L, NeutralPhantomsWatermark.current());
    }

    @Test
    void offlineNoChangeDoesNotAdvance(@TempDir Path worldRoot) throws IOException {
        writeWatermarkFile(worldRoot, 5L, true);
        NeutralPhantomsWatermark.initialize(worldRoot, true);
        assertEquals(5L, NeutralPhantomsWatermark.current());
    }

    // ---- conf 加载阶段的 observer（早于 initialize）安全 no-op ----

    @Test
    void observerBeforeInitializeIsNoOpAndReconciledLater(@TempDir Path worldRoot) throws IOException {
        NeutralPhantomsWatermark.onRuleChanged(false);
        NeutralPhantomsWatermark.onRuleChanged(true);
        writeWatermarkFile(worldRoot, 2L, true);
        NeutralPhantomsWatermark.initialize(worldRoot, true);
        assertEquals(2L, NeutralPhantomsWatermark.current());
    }

    // ---- 损坏 / 缺字段文件按首建回退 ----

    @Test
    void corruptedFileFallsBackToFreshState(@TempDir Path worldRoot) throws IOException {
        Files.createDirectories(watermarkFile(worldRoot).getParent());
        Files.writeString(watermarkFile(worldRoot), "not json {", StandardCharsets.UTF_8);
        NeutralPhantomsWatermark.initialize(worldRoot, true);
        assertEquals(0L, NeutralPhantomsWatermark.current());
    }

    @Test
    void missingFieldsFallBackToFreshState(@TempDir Path worldRoot) throws IOException {
        Files.createDirectories(watermarkFile(worldRoot).getParent());
        Files.writeString(watermarkFile(worldRoot), "{\"watermark\": 7}", StandardCharsets.UTF_8);
        NeutralPhantomsWatermark.initialize(worldRoot, false);
        assertFalse(NeutralPhantomsWatermark.current() == 7L);
    }
}
