package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link IsolatedHookRunner} 的隔离与聚合上报测试。
 *
 * <p>覆盖：单条失败不影响其它条目、失败计数、首次根因保留、聚合异常消息里必须带上根因的
 * 异常类型与简短信息（既有兼容性上报只打印外层异常的 toString）、消息压平换行与长度截断。
 */
class IsolatedHookRunnerTest {
    private static final String DESCRIPTION = "calciteStonecuttingRecipe: stonecutter menu sync";

    @Test
    void allSuccessfulTasksReportOk() {
        List<String> executed = new ArrayList<>();
        IsolatedHookRunner.Result result = IsolatedHookRunner.runAll(List.of(
                () -> executed.add("a"),
                () -> executed.add("b"),
                () -> executed.add("c")));

        assertEquals(List.of("a", "b", "c"), executed);
        assertTrue(result.ok());
        assertEquals(3, result.total());
        assertEquals(0, result.failed());
        assertNull(result.firstCause());
    }

    @Test
    void singleFailureIsIsolatedAndRemainingTasksStillRun() {
        List<String> executed = new ArrayList<>();
        RuntimeException failure = new IllegalStateException("menu rebuild failed");

        IsolatedHookRunner.Result result = IsolatedHookRunner.runAll(List.of(
                () -> executed.add("first"),
                () -> {
                    executed.add("second");
                    throw failure;
                },
                () -> executed.add("third")));

        assertEquals(List.of("first", "second", "third"), executed, "单条失败不得阻止后续条目被执行");
        assertEquals(3, result.total());
        assertEquals(1, result.failed());
        assertSame(failure, result.firstCause());
    }

    @Test
    void multipleFailuresCountAllAndKeepFirstCause() {
        RuntimeException first = new IllegalStateException("first");
        RuntimeException second = new IllegalArgumentException("second");

        IsolatedHookRunner.Result result = IsolatedHookRunner.runAll(List.of(
                () -> {
                    throw first;
                },
                () -> {
                },
                () -> {
                    throw second;
                }));

        assertEquals(3, result.total());
        assertEquals(2, result.failed());
        assertSame(first, result.firstCause(), "首次根因必须取第一条异常");
    }

    @Test
    void nullTaskCountsAsFailureAndDoesNotStopOthers() {
        List<String> executed = new ArrayList<>();
        List<Runnable> tasks = Arrays.asList(() -> executed.add("a"), null, () -> executed.add("b"));

        IsolatedHookRunner.Result result = IsolatedHookRunner.runAll(tasks);

        assertEquals(List.of("a", "b"), executed);
        assertEquals(3, result.total());
        assertEquals(1, result.failed(), "null 条目按失败计（fail closed），不得静默跳过");
        assertTrue(result.firstCause() instanceof NullPointerException);
    }

    @Test
    void emptyAndNullTaskListsAreOk() {
        IsolatedHookRunner.Result empty = IsolatedHookRunner.runAll(List.of());
        assertTrue(empty.ok());
        assertEquals(0, empty.total());
        assertNull(empty.firstCause());

        IsolatedHookRunner.Result nullList = IsolatedHookRunner.runAll(null);
        assertTrue(nullList.ok());
        assertEquals(0, nullList.total());
        assertNull(nullList.firstCause());
    }

    @Test
    void aggregatedExceptionCarriesDescriptionCountsAndFirstCauseBothWays() {
        RuntimeException failure = new IllegalStateException("menu rebuild failed");
        IsolatedHookRunner.Result result = IsolatedHookRunner.runAll(List.of(
                () -> {
                    throw failure;
                },
                () -> {
                },
                () -> {
                }));

        RuntimeException aggregated = result.aggregatedException(DESCRIPTION);
        String message = aggregated.getMessage();

        assertTrue(message.contains(DESCRIPTION), "必须带上调用方描述：" + message);
        assertTrue(message.contains("1/3"), "必须带上失败/总数：" + message);
        assertTrue(message.contains(IllegalStateException.class.getName()),
                "根因异常类型必须出现在消息里（兼容性上报只打印外层 toString）：" + message);
        assertTrue(message.contains("menu rebuild failed"),
                "根因简短信息必须出现在消息里：" + message);
        assertSame(failure, aggregated.getCause(), "同时保留 cause 链供完整栈排查");
    }

    @Test
    void aggregatedExceptionKeepsSingleLineAndTruncatesLongCauseMessage() {
        RuntimeException noisy = new IllegalStateException("line one\r\nline two " + "x".repeat(500));
        IsolatedHookRunner.Result result = IsolatedHookRunner.runAll(List.of(() -> {
            throw noisy;
        }));

        String message = result.aggregatedException(DESCRIPTION).getMessage();

        assertTrue(!message.contains("\n") && !message.contains("\r"), "日志必须只占一行：" + message);
        assertTrue(message.contains("line one") && message.contains("line two"),
                "换行应被压平（不保留原始换行）：" + message);
        assertTrue(message.endsWith("..."), "超长根因信息必须被截断：" + message);
        String tail = message.substring(message.indexOf("first cause: ") + "first cause: ".length());
        assertTrue(tail.length() <= IllegalStateException.class.getName().length() + 2 + 203,
                "截断后长度必须有界：" + tail.length());
    }

    @Test
    void okResultStillDescribesZeroFailuresSoCallersMustCheckOkFirst() {
        IsolatedHookRunner.Result result = IsolatedHookRunner.runAll(List.of(() -> {
        }));

        RuntimeException aggregated = result.aggregatedException(DESCRIPTION);

        assertTrue(aggregated.getMessage().contains("0/1"), "消息格式为 失败数/总数：" + aggregated.getMessage());
        assertNull(aggregated.getCause(), "无失败时不得凭空产生 cause");
    }
}
