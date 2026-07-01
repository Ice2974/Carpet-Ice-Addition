package com.ice2974.carpeticeaddition.rules;

/**
 * craftableCoralBlocks 规则的运行期冲突锁定标志。
 *
 * <p>纯 Java，不引用任何 Minecraft 类，也不读取规则字段（字段位于平台侧 settings 类，
 * 因 Validator 需引用 MC 类而无法放在 common）。平台侧 {@code effective()} =
 * {@code <平台Settings>.craftableCoralBlocks && !isConflictLocked()}。
 *
 * <p>语义：当外部数据包 / 模组提供与本模组自带珊瑚块配方同产物的 crafting 配方时，
 * 本标志被置 true，规则在运行期锁定为 false。Carpet 字段本身与 {@code carpet.conf}
 * 均不被修改；所有查询 / 配方书 / 菜单刷新路径应改用平台侧 {@code effective()}。
 *
 * <p>线程模型：{@code volatile}，由服务器主线程在 {@code onServerLoaded} / {@code onReload}
 * 中写入，被合成查询路径读取。
 */
public final class CraftableCoralBlocksState {
    private CraftableCoralBlocksState() {
    }

    private static volatile boolean conflictLocked = false;

    public static boolean isConflictLocked() {
        return conflictLocked;
    }

    public static void setConflictLocked(boolean locked) {
        conflictLocked = locked;
    }
}
