package com.ice2974.carpeticeaddition.rules;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * neutralPhantoms 规则「false 边界水位」的全局持久状态（Manager 模式，与
 * {@code KillItemConfigManager} / {@code MachineStatusConfigManager} 相同的
 * initialize / shutdown 生命周期，由入口类 {@code onServerLoaded} /
 * {@code onServerClosed} 接线）。
 *
 * <p>解决的问题：CIA retaliation（反击目标）是持久化状态，「规则经过 false 边界后
 * 旧 retaliation 失效」的判定需要一个跨区块卸载、跨服务器重启稳定的对照物。本类把
 * 两个值持久化到 {@code <world>/carpet-ice-addition/neutral_phantoms_watermark.json}：
 * 单调递增的 watermark 与上次规则是否启用（lastEnabled）。实体侧 NBT 记录
 * retaliation 建立时的水位，有效性 = 实体水位 == 当前水位（lazy invalidation，
 * 不扫描实体）。
 *
 * <p>水位推进的唯一条件是「规则值进入 false」，两个来源：
 * <ul>
 *   <li>运行期 observer（{@link #onRuleChanged}）：内存 lastEnabled==true 且新值为
 *       false 时 +1（true→false→true 同 tick 快速切换时，进入 false 的那次即推进）；</li>
 *   <li>离线变更：服务器关闭期间直接把 carpet conf 从 true 改为 false 不经过运行期
 *       observer，由 {@link #initialize} 以持久化的 lastEnabled 与当前实际规则值
 *       reconcile 补上该边界。</li>
 * </ul>
 *
 * <p>conf 启动加载早于 initialize，其触发的 observer 在未初始化时安全 no-op，由
 * initialize 统一收敛；false→true 方向（含 conf=true 的重启同步）不推进水位，保证
 * 未经 false 边界的合法 retaliation 正常跨卸载、跨重启保留。
 *
 * <p>首建文件（存档从未有过该文件，或内容损坏按首建回退 + warn）：当前规则 true →
 * watermark=0（旧存档 retaliation NBT 缺失水位键按 0 读，无缝保留既有语义）；当前
 * 规则 false → watermark=1（非零，使按 0 读的旧 retaliation 立即失配失效——规则已
 * 处于 false 状态，旧反击不应在规则再次开启后复活）。
 *
 * <p>降级语义：水位文件写入失败记录 warn，进程内失效保证仍成立（内存已 +1），跨
 * 重启退化为旧 retaliation 可能复活一次；读取失败按首建回退。所有方法只在服务端
 * 线程被调用（carpet rule observer / CarpetExtension 生命周期钩子 / 幻翼 AI tick），
 * 无需同步。
 */
public final class NeutralPhantomsWatermark {

    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");

    private static final String MOD_DIRECTORY = "carpet-ice-addition";
    private static final String FILE_NAME = "neutral_phantoms_watermark.json";

    private static final Gson GSON = new Gson();

    /** 未初始化哨兵：conf 启动加载阶段的 observer 调用据此安全 no-op。 */
    private static boolean initialized;
    private static Path watermarkFile;
    private static long watermark;
    private static boolean lastEnabled;

    private NeutralPhantomsWatermark() {
    }

    /**
     * 服务器启动时初始化：读持久状态并对离线 conf 变更 reconcile。
     *
     * @param worldRoot 存档根目录（{@code server.getWorldPath(LevelResource.ROOT)}）
     * @param currentRuleValue 当前实际规则值（conf 已加载后的静态字段值）
     */
    public static void initialize(Path worldRoot, boolean currentRuleValue) {
        watermarkFile = worldRoot.resolve(MOD_DIRECTORY).resolve(FILE_NAME);
        if (!readState()) {
            watermark = currentRuleValue ? 0L : 1L;
        } else if (lastEnabled && !currentRuleValue) {
            watermark++;
        }
        lastEnabled = currentRuleValue;
        writeState();
        initialized = true;
    }

    /**
     * 规则值变化的 observer 入口（post-commit，参数为新值）。未初始化时 no-op
     * （conf 启动加载早于 initialize，由其 reconcile 收敛）。
     */
    public static void onRuleChanged(boolean newValue) {
        if (!initialized) {
            return;
        }
        boolean changed = lastEnabled != newValue;
        if (lastEnabled && !newValue) {
            watermark++;
        }
        if (changed) {
            lastEnabled = newValue;
            writeState();
        }
    }

    /** 当前水位（lazy invalidation 的对照值）。 */
    public static long current() {
        if (!initialized) {
            LOGGER.warn("[Carpet Ice Addition] NeutralPhantomsWatermark 未初始化即被读取，按 0 处理");
            return 0L;
        }
        return watermark;
    }

    /** 服务器关闭时清空缓存（静态状态不得跨 server 生命周期存活）。 */
    public static void shutdown() {
        initialized = false;
        watermarkFile = null;
        watermark = 0L;
        lastEnabled = false;
    }

    private static boolean readState() {
        try {
            if (watermarkFile == null || !Files.isRegularFile(watermarkFile)) {
                return false;
            }
            JsonObject parsed = GSON.fromJson(
                    Files.readString(watermarkFile, StandardCharsets.UTF_8), JsonObject.class);
            if (parsed == null || !parsed.has("watermark") || !parsed.has("lastEnabled")) {
                LOGGER.warn("[Carpet Ice Addition] {} 结构不完整，按首建回退", watermarkFile);
                return false;
            }
            watermark = parsed.get("watermark").getAsLong();
            lastEnabled = parsed.get("lastEnabled").getAsBoolean();
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[Carpet Ice Addition] 读取 {} 失败（{}），按首建回退", watermarkFile, exception.toString());
            return false;
        }
    }

    private static void writeState() {
        try {
            Files.createDirectories(watermarkFile.getParent());
            Files.writeString(watermarkFile,
                    "{\"watermark\": " + watermark + ", \"lastEnabled\": " + lastEnabled + "}",
                    StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[Carpet Ice Addition] 写入 {} 失败（{}）：进程内失效保证仍成立，"
                    + "该次 false 边界可能不跨重启保留", watermarkFile, exception.toString());
        }
    }
}
