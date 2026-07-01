package com.ice2974.carpeticeaddition.settings;

import carpet.api.settings.Rule;
import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksValidator;

import static carpet.api.settings.RuleCategory.FEATURE;

/**
 * craftableCoralBlocks 规则的平台侧定义（1.21.3 ~ 1.21.11 yarn，shared）。
 *
 * <p>规则字段从 common 的 {@link CarpetIceAdditionSettings} 迁移至此：其 {@link CraftableCoralBlocksValidator}
 * 需引用 MC 类（{@code ServerCommandSource}），无法放入无 MC 依赖的 common 模块。
 *
 * <p>{@link #effective()} 结合运行期冲突锁定标志 {@link com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksState}：
 * 当外部数据包 / 模组提供与本模组自带珊瑚块配方同产物的 crafting 配方时，{@code conflictLocked} 被置 true，
 * 规则在运行期锁定为 false。Carpet 字段本身与 {@code carpet.conf} 均不被修改。
 */
public final class CraftableCoralBlocksSettings {
    public static final String ICE = CarpetIceAdditionSettings.ICE;

    private CraftableCoralBlocksSettings() {
    }

    @Rule(categories = {ICE, FEATURE}, validators = CraftableCoralBlocksValidator.class)
    public static boolean craftableCoralBlocks = false;

    /**
     * @return 规则的 effective 值：仅当字段为 true 且未处于冲突锁定时才为 true。
     */
    public static boolean effective() {
        return craftableCoralBlocks && !com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksState.isConflictLocked();
    }
}
