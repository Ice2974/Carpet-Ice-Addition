package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Detects conflicting external crafting recipes for craftableCoralBlocks.
 */
public final class CraftableCoralBlocksConflictDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");

    private CraftableCoralBlocksConflictDetector() {
    }

    /**
     * 扫描 RecipeManager 判定是否存在外部 crafting 配方冲突。
     */
    public static boolean detectConflict(MinecraftServer server) {
        return CraftingRecipeConflictScanner.detectConflict(server, CraftableCoralBlocksRecipes.CONFLICT);
    }

    /**
     * 重新计算冲突锁定状态，并在状态迁移时广播提示与写日志。
     *
     * <p>必须在 recipe book 同步与菜单刷新之前调用。字段压 false / 恢复 desiredValue 均通过
     * 直接静态字段写完成，不经 SettingsManager，因此不触发 observer / validator / 配置保存，
     * 不修改 {@code carpet.conf}。Carpet {@code /carpet} 查询走 {@code ParsedRule.value()}
     * 实时反射读字段，故显示与字段一致。调用方需在之后显式调用
     * {@link CraftableCoralBlocksRecipeBookHelper} 完成同步——直接字段写不触发 observer。
     *
     * <p>异常安全：{@link #detectConflict} 内部已 catch 单条 recipe 异常；若其本身抛出（不应发生），
     * 由调用方 try/catch 兜底，本方法不修改 conflictLocked / desiredValue / 字段，避免错误恢复或清空。
     */
    public static void recomputeAndNotify(MinecraftServer server) {
        RecipeConflictLock.Transition transition = CraftableCoralBlocksState.LOCK.apply(
                detectConflict(server) ? RecipeConflictLock.Detection.CONFLICT : RecipeConflictLock.Detection.NO_CONFLICT,
                () -> CraftableCoralBlocksSettings.craftableCoralBlocks, value -> CraftableCoralBlocksSettings.craftableCoralBlocks = value);
        if (transition == RecipeConflictLock.Transition.LOCKED) {
            broadcast(server, "carpet.rule.craftableCoralBlocks.conflict.locked");
        } else if (transition == RecipeConflictLock.Transition.UNLOCKED) {
            broadcast(server, "carpet.rule.craftableCoralBlocks.conflict.resolved");
        }
    }

    private static void broadcast(MinecraftServer server, String key) {
        String text = TranslationFormatUtil.translate(key);
        LOGGER.warn("[Carpet Ice Addition] {}", text);
        if (server.getPlayerList() != null) {
            Component msg = Component.literal(text);
            server.getPlayerList().broadcastSystemMessage(msg, false);
        }
    }

}
