package com.ice2974.carpeticeaddition.rules;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validator;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * {@code calciteStonecuttingRecipe} 规则的自定义 Validator。
 *
 * <p>当处于冲突锁定状态时，拒绝将规则设置为 true，并向命令来源反馈锁定提示；
 * 锁定期用户显式选择 false 时，记录为 {@code desiredValue=false}，供冲突解除后保持 false。
 *
 * <p>为何在 validator 而非 ruleObserver 中更新 desiredValue：冲突锁定时字段已被
 * {@code recomputeAndNotify} 直接静态字段写压成 false，此时玩家执行
 * {@code /carpet calciteStonecuttingRecipe false} 属于「设置与当前值相同的值」，Carpet
 * {@code ParsedRule.set} 会跳过字段写入与 {@code notifyRuleChanged}，因此 ruleObserver 不会触发。
 * validator 在 {@code ParsedRule.set} 中无条件执行，是唯一可靠的 desiredValue 更新点。
 *
 * <p>时序保障：{@code conflictLocked} 只在静默点（世界加载完成 / 资源 reload 结束）被置 true，
 * 而这两者均在 Carpet {@code SettingsManager.attachServer}（读取 {@code carpet.conf}，触发 validator）
 * 之后执行。因此 {@code carpet.conf} 中的 {@code calciteStonecuttingRecipe true} 在启动加载时
 * validator 必然放行，不会被拒绝，也不会被自动改写。
 */
public final class CalciteStonecuttingRecipeValidator extends Validator<Boolean> {
    @Override
    public Boolean validate(@Nullable CommandSourceStack source, CarpetRule<Boolean> changingRule, Boolean newValue, String userInput) {
        if (newValue == null) {
            return null;
        }
        if (CalciteStonecuttingRecipeState.isConflictLocked()) {
            if (newValue) {
                return null;
            }
            // 锁定期显式选择 false：记录为期望值，解除后保持 false
            CalciteStonecuttingRecipeState.setDesiredValue(Boolean.FALSE);
        }
        return newValue;
    }

    @Override
    public void notifyFailure(CommandSourceStack source, CarpetRule<Boolean> currentRule, String providedValue) {
        // 走服务端翻译：解析翻译键为 literal 文本发送给客户端（客户端可能未安装本模组）。
        source.sendSuccess(() -> Component.literal(
                TranslationFormatUtil.translate("carpet.rule.calciteStonecuttingRecipe.conflict.command")), false);
    }
}
