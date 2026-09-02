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

    /**
     * FindPointOfInterestTask / AcquirePoi 工厂调用是否为“找工作点”变体（写入 POTENTIAL_JOB_SITE）。
     * 原版 HOME / MEETING_POINT 变体虽会经由最宽重载委托进入同一方法体，但其传入的两个
     * MemoryModuleType 参数始终是同一实例；只有 JOB_SITE 变体传入两个不同实例
     * （JOB_SITE 与 POTENTIAL_JOB_SITE），原版自身也以该引用不等作为包装分支条件。
     * 因此本判定为 true 时只可能是 JOB_SITE 变体，HOME / MEETING_POINT 永远返回 false。
     */
    public static boolean isJobSitePoiVariant(Object poiPosModule, Object potentialPoiPosModule) {
        return poiPosModule != potentialPoiPosModule;
    }
}
