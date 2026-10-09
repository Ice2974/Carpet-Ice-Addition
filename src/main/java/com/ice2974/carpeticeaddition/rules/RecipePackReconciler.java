package com.ice2974.carpeticeaddition.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 配方类内置数据包选中集合的纯函数规划器（无 Minecraft 依赖，可直接单元测试）。
 *
 * <p>输入「PackRepository 当前选中集合」与「全部受管包的期望状态」，输出一次性目标集合、
 * 需要执行即时清理（{@code onPackDisabled}）的包列表，以及目标集合是否与当前不同。
 *
 * <p>语义：
 * <ul>
 *   <li>从当前选中集合出发，保持其成员与相对顺序（用户自选数据包不受影响）；</li>
 *   <li>期望开启且缺失 → 追加到末尾（已存在则保持原位，不产生顺序抖动）；</li>
 *   <li>期望关闭且存在 → 移除，并记入 {@link Plan#disabledPackIds()}，供调用方在改选前
 *       执行即时清理（与既有 {@code onPackDisabled} 时序一致）；</li>
 *   <li>集合内容相等时 {@link Plan#changed()} 为 false → 调用方不得发起 reload（无意义 reload 禁止）。</li>
 * </ul>
 *
 * <p>幂等且收敛：{@code plan(plan(x).next(), desires).changed()} 恒为 false。
 */
public final class RecipePackReconciler {
    private RecipePackReconciler() {
    }

    /** 单个受管包的期望状态。{@code packId} 为 {@code namespace:path} 形式字符串。 */
    public record PackDesire(String packId, String ruleName, boolean desired) {
    }

    /** 规划结果：{@code next} 为目标选中集合（顺序保持），{@code disabledPackIds} 为本次由选中变为未选中的包。 */
    public record Plan(LinkedHashSet<String> next, List<String> disabledPackIds, boolean changed) {
        /** 目标集合的规范化键，供状态机做「同目标」比较（避免依赖集合实例）。 */
        public String targetKey() {
            return RecipePackReconciler.targetKey(next);
        }
    }

    /** 目标集合的规范化键：顺序敏感（选中顺序即数据包优先级），以不可见分隔符连接。 */
    public static String targetKey(Collection<String> ids) {
        return String.join("\u0000", ids);
    }

    public static Plan plan(Collection<String> selectedIds, List<PackDesire> desires) {
        LinkedHashSet<String> next = new LinkedHashSet<>(selectedIds);
        List<String> disabled = new ArrayList<>();
        for (PackDesire desire : desires) {
            String packId = desire.packId();
            if (packId == null || packId.isEmpty()) {
                continue;
            }
            if (desire.desired()) {
                next.add(packId);
            } else if (next.remove(packId)) {
                disabled.add(packId);
            }
        }
        boolean changed = !next.equals(new LinkedHashSet<>(selectedIds));
        return new Plan(next, List.copyOf(disabled), changed);
    }
}
