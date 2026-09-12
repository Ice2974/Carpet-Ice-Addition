package com.ice2974.carpeticeaddition.rules;

/**
 * bedrockTridentPort 规则的开启世代计数器。
 *
 * <p>规则每次变更（任意方向）时经 CarpetIceAdditionMod 的 rule observer 递增。
 * ThrownTridentBedrockTridentPortMixin 以「武装时的世代」标记重新命中资格的有效性：
 * 世代不匹配即视为无资格。这样即使 true→false→true 在某三叉戟下一次实体 tick 前完成，
 * 上一次开启周期遗留的资格也因世代不匹配立即失效，无需遍历实体、不引入 NBT。
 */
public final class BedrockTridentPortRuleTracker {

    private static volatile int generation;

    private BedrockTridentPortRuleTracker() {
    }

    public static void onRuleChanged() {
        generation++;
    }

    public static int generation() {
        return generation;
    }
}
