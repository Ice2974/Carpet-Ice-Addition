package com.ice2974.carpeticeaddition.rules;

/**
 * enhancedTrident 规则 grounded-rearm 资格的代际（generation）计数器，仅存于内存。
 *
 * <p>每次 {@code enhancedTrident} 规则值真实变化时由 {@code CarpetIceAdditionMod}
 * 注册的 rule observer 递增。三叉戟实体侧的 rearm 资格记录授予时的代际，有效性 =
 * 授予代际 == 当前代际；因此 {@code true → false → true} 在实体下一 tick 前快速
 * 完成时，旧授予不会在重新开启规则后复活。
 *
 * <p>carpet 的 rule observer（1.4.147 与 26.2 字节码双端核实）为 post-commit 通知，
 * 仅在值实际变化（{@code !newValue.equals(oldValue)}）且 source 非空（/carpet 命令
 * 与 conf 加载，均在服务端线程）时触发，故计数器只在服务端线程读写，无需同步。
 * 不写入 NBT；server 重启后计数器归零，但实体侧资格同样为非持久状态（默认无资格），
 * 语义自洽。
 */
public final class EnhancedTridentRearmEpoch {

    private static int generation = 0;

    private EnhancedTridentRearmEpoch() {
    }

    public static int current() {
        return generation;
    }

    public static void advance() {
        generation++;
    }
}
