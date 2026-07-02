package com.ice2974.carpeticeaddition.rules;

/**
 * craftableCoralBlocks 规则的运行期冲突锁定状态。
 *
 * <p>纯 Java，不引用任何 Minecraft 类，也不读取规则字段（字段位于平台侧 settings 类，
 * 因 Validator 需引用 MC 类而无法放在 common）。平台侧 {@code effective()} =
 * {@code <平台Settings>.craftableCoralBlocks && !isConflictLocked()}。
 *
 * <p>语义：当外部数据包 / 模组提供与本模组自带珊瑚块配方同产物的 crafting 配方时，
 * 本标志被置 true，规则在运行期锁定为 false。锁定时平台侧 {@code recomputeAndNotify} 会通过
 * 直接静态字段写把 {@code craftableCoralBlocks} 压成 false（不经 SettingsManager，不触发 observer /
 * validator / 配置保存，不修改 {@code carpet.conf}）；Carpet {@code /carpet} 查询走
 * {@code ParsedRule.value()} 实时反射读字段，因此显示为 false。
 *
 * <p>{@code desiredValue} 记录冲突锁定前规则字段的期望值，供冲突解除后恢复使用：
 * <ul>
 *   <li>新锁定时保存冲突前字段值；</li>
 *   <li>锁定期用户通过命令显式选择 false 时，由 {@code Validator.validate} 将 desiredValue 置 false
 *      （因字段已被压成 false，Carpet {@code ParsedRule.set} 检测到同值不触发 observer，故不能依赖 observer 更新 desiredValue）；</li>
 *   <li>冲突解除时按 desiredValue 恢复字段，随后立即清空 desiredValue；</li>
 *   <li>{@code onServerClosed} 同时复位 conflictLocked=false 与 desiredValue=null。</li>
 * </ul>
 *
 * <p>线程模型：{@code volatile}，由服务器主线程在 {@code onServerLoadedWorlds} / {@code onReload}
 * 中写入，被合成查询路径与 validator 读取。
 */
public final class CraftableCoralBlocksState {
    private CraftableCoralBlocksState() {
    }

    private static volatile boolean conflictLocked = false;

    /** 冲突锁定前的规则字段期望值；null 表示未锁定或已恢复。 */
    private static volatile Boolean desiredValue = null;

    public static boolean isConflictLocked() {
        return conflictLocked;
    }

    public static void setConflictLocked(boolean locked) {
        conflictLocked = locked;
    }

    public static Boolean getDesiredValue() {
        return desiredValue;
    }

    public static void setDesiredValue(Boolean value) {
        desiredValue = value;
    }
}
