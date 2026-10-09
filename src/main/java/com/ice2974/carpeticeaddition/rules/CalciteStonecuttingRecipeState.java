package com.ice2974.carpeticeaddition.rules;

/**
 * {@code calciteStonecuttingRecipe} 规则的运行期冲突锁定状态。
 *
 * <p>纯 Java，不引用任何 Minecraft 类，也不读取规则字段（字段位于平台侧 settings 类，
 * 因 Validator 需引用 MC 类而无法放在 common）。平台侧 {@code effective()} =
 * {@code <平台Settings>.calciteStonecuttingRecipe && !isConflictLocked()}。
 *
 * <p>语义：当外部数据包 / 模组提供了与本模组自带切石配方同产物的切石配方，或本模组配方 id 被覆盖成
 * 不符合规则承诺的内容时，本标志被置 true，规则在运行期锁定为 false。锁定时检测器会通过直接静态字段写
 * 把 {@code calciteStonecuttingRecipe} 压成 false（不经 SettingsManager，不触发 observer / validator /
 * 配置保存，不修改 {@code carpet.conf}）。
 *
 * <p>{@code desiredValue} 记录冲突锁定前规则字段的期望值，供冲突解除后恢复：
 * <ul>
 *   <li>新锁定时保存冲突前字段值；</li>
 *   <li>锁定期用户通过命令显式选择 false 时，由 {@code Validator.validate} 将 desiredValue 置 false；</li>
 *   <li>冲突解除时按 desiredValue 恢复字段，随后立即清空；</li>
 *   <li>服务器关闭 / 换服时复位 conflictLocked=false 与 desiredValue=null。</li>
 * </ul>
 *
 * <p>线程模型：{@code volatile}，由服务器主线程在静默点写入，被合成查询路径与 validator 读取。
 */
public final class CalciteStonecuttingRecipeState {
    private CalciteStonecuttingRecipeState() {
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
