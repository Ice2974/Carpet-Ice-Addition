package com.ice2974.carpeticeaddition.rules;

/**
 * ironGolemSpawningOptimization 规则的纯文本判定逻辑。
 * 调用方需传入 CustomName 的纯文本（Yarn Text#getString / Mojmap Component#getString）：
 * 比较纯文本内容，区分大小写，颜色、粗体等样式不影响匹配。
 */
public final class IronGolemVillagerOptimizer {

    public static final String OPTIMIZED_VILLAGER_NAME = "iron_golem";

    private IronGolemVillagerOptimizer() {
    }

    /**
     * 纯文本精确等于小写 iron_golem 时命中；前后空格、其他大小写或其他字符均不命中。
     */
    public static boolean matchesOptimizedVillagerName(String plainTextName) {
        return OPTIMIZED_VILLAGER_NAME.equals(plainTextName);
    }
}
