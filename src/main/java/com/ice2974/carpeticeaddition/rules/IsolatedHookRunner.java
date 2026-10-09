package com.ice2974.carpeticeaddition.rules;

import java.util.List;

/**
 * 逐条执行动作并隔离单条异常的纯 Java 运行器（无 Minecraft 依赖，可直接单元测试）。
 *
 * <p>用途：面向「逐玩家」的刷新 / 同步动作——单个目标失败不得阻止其它目标被处理，但失败也不能像
 * {@code catch (Throwable ignored)} 那样被吞掉，否则上层会把握把实际失败的同步当成成功
 * （协调器 {@code runPass} 判定为全成功 → {@code RecipeReloadGate.onPassFinished(true)} → 误报 ready，
 * 且不再重新欠 pass）。
 *
 * <p>因此本类同时承担两件事：
 * <ol>
 *   <li><b>隔离</b>：每个动作独立 try/catch，单条失败不影响后续动作；</li>
 *   <li><b>聚合上报</b>：返回失败条数与第一条根因，由调用方抛出
 *       {@link Result#aggregatedException(String)}，交给既有的钩子失败路径处理。</li>
 * </ol>
 *
 * <p>本类不写任何日志：上报交给调用方经既有通道完成（协调器 {@code safeRun} →
 * {@code FeatureCompatibilityReporter}，按规则名去重），因此不会重复刷屏。
 */
public final class IsolatedHookRunner {
    /** 聚合消息中首次根因信息的上限长度，避免超长异常信息撑爆单行日志。 */
    private static final int MAX_CAUSE_DESCRIPTION = 200;

    /**
     * 执行结果。
     *
     * @param total      尝试执行的动作总数
     * @param failed     抛异常的动作数
     * @param firstCause 第一条异常（全部成功时为 {@code null}）
     */
    public record Result(int total, int failed, Throwable firstCause) {
        public boolean ok() {
            return failed == 0;
        }

        /**
         * 构造供上层抛出的聚合异常。
         *
         * <p>消息里显式带上首次根因的异常类型与简短信息：既有 {@code FeatureCompatibilityReporter}
         * 只打印外层异常的 {@code toString()}（不展开 cause 栈），根因必须出现在消息里才能被日志看到；
         * 同时保留 cause 链，便于需要完整栈的排查场景。
         */
        public RuntimeException aggregatedException(String description) {
            return new IllegalStateException(
                    description + " failed for " + failed + "/" + total + " target(s); first cause: "
                            + describe(firstCause),
                    firstCause);
        }

        /**
         * 合并两段**独立执行**的结果。
         *
         * <p>用途：同一次同步里「配方书动作」与「菜单动作」必须分别尝试——前一段失败不得跳过后一段，
         * 因此两段各自调用 {@link #runAll(java.util.List)}，再合并成**一次**上报，避免后一段的失败
         * 被前一段的聚合异常顶掉，也避免同一次同步上报两条记录。
         *
         * <p>语义：计数相加；{@code firstCause} 取先出现者（本结果的根因优先）。{@code null} 视为无失败。
         */
        public Result plus(Result other) {
            if (other == null) {
                return this;
            }
            return new Result(
                    total + other.total,
                    failed + other.failed,
                    firstCause != null ? firstCause : other.firstCause);
        }
    }

    private IsolatedHookRunner() {
    }

    /**
     * 逐条执行全部动作：单条异常被隔离并计入失败数，其余动作照常执行。
     *
     * <p>列表中的 {@code null} 元素按失败计（fail closed），不做静默跳过。
     */
    public static Result runAll(List<? extends Runnable> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return new Result(0, 0, null);
        }
        int failed = 0;
        Throwable firstCause = null;
        for (Runnable task : tasks) {
            try {
                task.run();
            } catch (Throwable throwable) {
                failed++;
                if (firstCause == null) {
                    firstCause = throwable;
                }
            }
        }
        return new Result(tasks.size(), failed, firstCause);
    }

    /** 首次根因的「异常类型 + 简短信息」描述：压平换行、截断长度，保证只占一行日志。 */
    private static String describe(Throwable throwable) {
        if (throwable == null) {
            return "none";
        }
        String type = throwable.getClass().getName();
        String message;
        try {
            message = throwable.getMessage();
        } catch (Throwable ignored) {
            return type;
        }
        if (message == null) {
            return type;
        }
        String flattened = message.replace('\n', ' ').replace('\r', ' ').trim();
        if (flattened.isEmpty()) {
            return type;
        }
        if (flattened.length() > MAX_CAUSE_DESCRIPTION) {
            flattened = flattened.substring(0, MAX_CAUSE_DESCRIPTION) + "...";
        }
        return type + ": " + flattened;
    }
}
