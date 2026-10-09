package com.ice2974.carpeticeaddition.settings;

import carpet.api.settings.Rule;
import com.ice2974.carpeticeaddition.rules.CalciteStonecuttingRecipeValidator;

import static carpet.api.settings.RuleCategory.FEATURE;
import static carpet.api.settings.RuleCategory.SURVIVAL;

/**
 * {@code calciteStonecuttingRecipe} 规则定义。
 *
 * <p>规则字段独立于 {@link CarpetIceAdditionSettings} 定义：其 {@link CalciteStonecuttingRecipeValidator}
 * 需引用 MC 类（{@code CommandSourceStack}）。
 *
 * <p>{@link #effective()} 结合运行期冲突锁定标志
 * {@link com.ice2974.carpeticeaddition.rules.CalciteStonecuttingRecipeState}：
 * 当外部数据包 / 模组提供与本模组自带切石配方同产物的切石配方，或本模组配方 id 被覆盖成不符合规则承诺的
 * 内容时，{@code conflictLocked} 被置 true，规则在运行期锁定为 false。Carpet 字段本身与
 * {@code carpet.conf} 均不被修改。
 */
public final class CalciteStonecuttingRecipeSettings {
    public static final String ICE = CarpetIceAdditionSettings.ICE;

    private CalciteStonecuttingRecipeSettings() {
    }

    @Rule(categories = {ICE, FEATURE, SURVIVAL}, validators = CalciteStonecuttingRecipeValidator.class)
    public static boolean calciteStonecuttingRecipe = false;

    /**
     * @return 规则的 effective 值：仅当字段为 true 且未处于冲突锁定时才为 true。
     */
    public static boolean effective() {
        return calciteStonecuttingRecipe
                && !com.ice2974.carpeticeaddition.rules.CalciteStonecuttingRecipeState.isConflictLocked();
    }
}
