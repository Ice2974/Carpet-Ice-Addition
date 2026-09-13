package com.ice2974.carpeticeaddition.rules;

/**
 * betterTridentDespawnCondition 规则静止计时的代际（generation）计数器，仅存于内存。
 *
 * <p>每次 {@code betterTridentDespawnCondition} 规则值真实变化时由
 * {@code CarpetIceAdditionMod} 注册的 rule observer 递增。三叉戟实体侧的静止采样
 * 状态记录采样时的代际，有效性 = 采样代际 == 当前代际；因此
 * {@code true → false → true} 在实体下一 tick 前快速完成时，旧累积的静止采样
 * 不会在重新开启规则后复活（规则关闭期间实体可能被移动而未被采样，旧计数不可信）。
 *
 * <p>carpet 的 rule observer（1.4.147 与 26.2 字节码双端核实）为 post-commit 通知，
 * 仅在值实际变化且 source 非空（/carpet 命令与 conf 加载，均在服务端线程）时触发，
 * 故计数器只在服务端线程读写，无需同步。不写入 NBT；server 重启后计数器归零，但
 * 实体侧静止计时同样为非持久状态（重载后自下一次采样从 1 重新累积），语义自洽。
 */
public final class BetterTridentDespawnEpoch {

    private static int generation = 0;

    private BetterTridentDespawnEpoch() {
    }

    public static int current() {
        return generation;
    }

    public static void advance() {
        generation++;
    }
}
