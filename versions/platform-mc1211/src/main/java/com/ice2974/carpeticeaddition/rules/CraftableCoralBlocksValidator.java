package com.ice2974.carpeticeaddition.rules;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Validator;
import com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * craftableCoralBlocks 规则的自定义 Validator（1.21.1 yarn）。
 *
 * <p>当处于冲突锁定状态时，拒绝将规则设置为 true，并向命令来源反馈锁定提示。
 *
 * <p>时序保障：{@code conflictLocked} 仅在 CarpetExtension {@code onServerLoaded} / {@code onReload}
 * 中被置 true，而这两者均在 Carpet {@code SettingsManager.attachServer}（读取 {@code carpet.conf}，
 * 触发 validator）之后执行。因此 {@code carpet.conf} 中的 {@code craftableCoralBlocks true} 在启动加载时
 * validator 必然放行（{@code conflictLocked == false}），不会被拒绝，也不会被自动改写。
 *
 * <p>不影响设置 false；冲突解除后 {@code conflictLocked == false}，validator 放行 true。
 */
public final class CraftableCoralBlocksValidator extends Validator<Boolean> {
    @Override
    public Boolean validate(@Nullable ServerCommandSource source, CarpetRule<Boolean> changingRule, Boolean newValue, String userInput) {
        if (newValue != null && newValue && CraftableCoralBlocksState.isConflictLocked()) {
            return null;
        }
        return newValue;
    }

    @Override
    public void notifyFailure(ServerCommandSource source, CarpetRule<Boolean> currentRule, String providedValue) {
        // source 在 notifyFailure 中不会为 null（仅当 validate 因非 null source 的命令调用而拒绝时触发）。
        // 走服务端翻译：解析翻译键为 literal 文本发送给客户端。
        source.sendFeedback(() -> Text.literal(TranslationFormatUtil.translate("carpet.rule.craftableCoralBlocks.conflict.command")), false);
    }
}
