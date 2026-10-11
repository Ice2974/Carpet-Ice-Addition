package com.ice2974.carpeticeaddition.rules;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RecipePackOrchestrator}（生产编排核心）的直接驱动单测。
 *
 * <p>不新建第二套模拟实现：被测对象即生产类，伪造的只有 {@link ServerAdapter} 与
 * {@link RecipePackCoordinator.ManagedPack} 钩子的记录实现。测试令牌与钩子 server 引用分离
 * （互异 {@code Object} 令牌区分服务器身份，钩子引用传 null——记录钩子不解引用）。
 *
 * <p><b>时序镜像</b>：真实环境中 Fabric 的 START 在 {@code reloadResources} 内部（HEAD 注入）同步到达，
 * END 经 {@code handleAsync} 作为独立任务在返回后投递，且服务器线程上 reload 阻塞至完成。
 * 适配器据此建模：{@code onStart} 在 {@code reloadResources} 入口同步触发；正常模式返回已完成
 * future 并应用所请求的选中集合；延迟模式返回未完成 future，由测试补完成与 END。
 *
 * <p>结算语义锁定：{@code settleReturnedFuture} 只在 {@code reloadResources} 返回后快照一次，
 * END 不结算自有请求；延迟 future 用例明确区分原请求与 END 后可能启动的新请求（以请求计数与
 * failureCount 区分，不把新请求状态覆盖误读为原请求结算）。
 */
class RecipePackOrchestratorTest {

    // ------------------------------------------------------------------ 静默点与 pass 编排

    @Test
    void worldsLoadedRunsPassInOrderWithoutUnnecessaryReload() {
        Harness h = new Harness();
        h.loadWorlds();
        // 两个包都已收敛（desired=false 且未选中）：绑定重置 → 全部 recompute 先于全部 sync，不发起 reload
        assertEquals(List.of("A:resetLockState", "B:resetLockState",
                "A:recompute", "B:recompute", "A:sync", "B:sync"), h.eventLog);
        assertEquals(0, h.adapter.reloadRequests.size());
        assertEquals(1, h.adapter.targetUpdates.size());
        assertNull(h.orch.gate().ownOutcome());
        assertFalse(h.orch.isReconcileInFlight());
    }

    @Test
    void ruleChangeToDesiredReloadsOnceWithCorrectTargetAndConverges() {
        Harness h = new Harness();
        h.loadWorlds();
        h.packA.desired = true;
        assertTrue(h.orch.onRuleChanged("ruleA", h.serverA, null));
        // 服务器线程阻塞语义：reloadResources 返回即完成 → 自有请求立即结算成功
        assertEquals(1, h.adapter.reloadRequests.size());
        assertEquals(java.util.Set.of("vanilla", "carpet-ice-addition:pack_a"),
                java.util.Set.copyOf(h.adapter.reloadRequests.get(0)));
        assertEquals(RecipeReloadGate.Outcome.SUCCESS, h.orch.gate().ownOutcome());
        // END 后置到达：静默点 pass + 收敛判定；reload 已应用目标集合 → 不再发起
        h.orch.onReloadEnd(h.serverA, true);
        assertTrue(h.adapter.selected.contains("carpet-ice-addition:pack_a"));
        assertEquals(1, h.adapter.reloadRequests.size());
        // 同令牌重复入口：不重复绑定、不重复更新目标
        h.packA.desired = false;
        h.packA.desired = true;
        assertTrue(h.orch.onRuleChanged("ruleA", h.serverA, null));
        assertEquals(1, h.adapter.targetUpdates.size());
    }

    @Test
    void singlePackPassFailureIsIsolatedAndReOwesPass() {
        Harness h = new Harness();
        h.packA.failRecompute = true;
        h.loadWorlds();
        // A 的 recompute 失败不阻断 B 与后续 sync 段（allOk 聚合：整体失败但逐钩子隔离）
        assertTrue(h.eventLog.containsAll(List.of("B:recompute", "A:sync", "B:sync")));
        // pass 失败 → 重新欠一次：外部 reload 的正常 END 触发再次 pass
        int recomputeBefore = h.packB.recomputeCalls;
        h.packA.failRecompute = false;
        h.externalReload(true);
        assertEquals(recomputeBefore + 1, h.packB.recomputeCalls);
        assertEquals(recomputeBefore + 1, h.packA.recomputeCalls);
        assertEquals(0, h.adapter.reloadRequests.size());
    }

    @Test
    void onPackDisabledRunsBeforeSelectionChangingReload() {
        Harness h = new Harness();
        h.loadWorlds();
        h.packA.desired = true;
        h.orch.onRuleChanged("ruleA", h.serverA, null);
        h.orch.onReloadEnd(h.serverA, true);
        assertTrue(h.adapter.selected.contains("carpet-ice-addition:pack_a"));

        h.eventLog.clear();
        h.packA.desired = false;
        assertTrue(h.orch.onRuleChanged("ruleA", h.serverA, null));
        assertEquals(1, h.packA.disableCalls);
        int disabledAt = h.eventLog.indexOf("A:onPackDisabled");
        int reloadAt = h.eventLog.indexOf("reload");
        assertTrue(disabledAt >= 0 && reloadAt >= 0 && disabledAt < reloadAt,
                "onPackDisabled must run before the selection-changing reload");
        h.orch.onReloadEnd(h.serverA, true);
        assertFalse(h.adapter.selected.contains("carpet-ice-addition:pack_a"));
    }

    // ------------------------------------------------------------------ reload 时序与结算

    @Test
    void offThreadRequestIsRefusedWithoutRequestAccount() {
        Harness h = new Harness();
        h.loadWorlds();
        h.adapter.serverThread = false;
        h.packA.desired = true;
        assertTrue(h.orch.onRuleChanged("ruleA", h.serverA, null));
        assertEquals(0, h.adapter.reloadRequests.size());
        assertNull(h.orch.gate().ownOutcome());
        // 回到服务器线程后同一规则变化语义仍可正常发起
        h.adapter.serverThread = true;
        assertTrue(h.orch.onRuleChanged("ruleA", h.serverA, null));
        assertEquals(1, h.adapter.reloadRequests.size());
    }

    @Test
    void syncThrowSettlesFailureThenRetrySucceeds() {
        Harness h = new Harness();
        h.loadWorlds();
        h.adapter.nextReloadThrows = true;
        h.packA.desired = true;
        assertTrue(h.orch.onRuleChanged("ruleA", h.serverA, null));
        // START 已在 reloadResources 入口到达（HEAD 注入）：同步异常走「有 START 无 END」补偿递减；
        // 随后静默点自动重试（本次正常）——重试的 START 在飞，需等其专属 END 才归零
        assertEquals(1, h.orch.gate().inFlightCount());
        // 补投重试的 END 使系统静默并触发收敛
        h.orch.onReloadEnd(h.serverA, false);
        assertEquals(0, h.orch.gate().inFlightCount());
        assertEquals(RecipeReloadGate.Outcome.SUCCESS, h.orch.gate().ownOutcome());
        assertEquals(2, h.adapter.reloadRequests.size());
        assertEquals(1, h.orch.gate().failureCount());
        assertTrue(h.adapter.selected.contains("carpet-ice-addition:pack_a"));
    }

    @Test
    void failedFutureSettlesImmediatelyAndLateEndDoesNotRewrite() {
        Harness h = new Harness();
        h.loadWorlds();
        h.adapter.nextReloadFails = true;
        h.packA.desired = true;
        assertTrue(h.orch.onRuleChanged("ruleA", h.serverA, null));
        // 返回即已异常完成：立即结算 FAILURE（重试预算已记账，等待静默点）
        assertEquals(RecipeReloadGate.Outcome.FAILURE, h.orch.gate().ownOutcome());
        assertEquals(1, h.adapter.reloadRequests.size());
        assertTrue(h.orch.isReconcileInFlight());
        // END 到达：全局计数递减；静默点重试（本次成功）随即启动，其 START 在飞
        h.orch.onReloadEnd(h.serverA, true);
        assertTrue(h.orch.isReconcileInFlight());
        assertEquals(RecipeReloadGate.Outcome.SUCCESS, h.orch.gate().ownOutcome());
        assertEquals(2, h.adapter.reloadRequests.size());
        // 重试的专属 END 到达 → 静默 + pass + 收敛判定（已收敛，不再发起）
        h.orch.onReloadEnd(h.serverA, true);
        assertFalse(h.orch.isReconcileInFlight());
        // 迟到的额外 END（此时无在飞 reload）：ANOMALOUS，不改写结局、不发起无谓 reload
        h.orch.onReloadEnd(h.serverA, true);
        assertEquals(RecipeReloadGate.Outcome.SUCCESS, h.orch.gate().ownOutcome());
        assertEquals(2, h.adapter.reloadRequests.size());
    }

    @Test
    void delayedFutureKeepsSnapshotSettlementAndEndDoesNotSettleOwnRequest() {
        Harness h = new Harness();
        h.loadWorlds();
        h.adapter.nextReloadDelayed = true;
        h.packA.desired = true;
        assertTrue(h.orch.onRuleChanged("ruleA", h.serverA, null));
        // 返回时未完成：settleReturnedFuture 不结算（保持 PENDING 快照），且 START 在飞 → 非静默
        assertEquals(RecipeReloadGate.Outcome.PENDING, h.orch.gate().ownOutcome());
        assertNotNull(h.adapter.lastPendingFuture);
        assertEquals(1, h.orch.gate().reloadRequestCount());
        assertTrue(h.orch.isReconcileInFlight());

        // future 事后完成（reload 最终应用目标集合）+ END 到达：END 只做全局计数递减与静默点编排，
        // 不结算自有请求——原请求结局保持返回时快照
        h.adapter.selected.add("carpet-ice-addition:pack_a");
        h.adapter.lastPendingFuture.complete(null);
        h.orch.onReloadEnd(h.serverA, true);
        assertEquals(RecipeReloadGate.Outcome.PENDING, h.orch.gate().ownOutcome());
        assertEquals(0, h.orch.gate().failureCount());
        // 已收敛 → 未启动新请求：请求计数不变是「无新请求」的区分依据
        assertEquals(1, h.orch.gate().reloadRequestCount());
        assertEquals(1, h.adapter.reloadRequests.size());
        // 外部 END 触发的静默点 pass 已执行
        assertEquals(2, h.packA.recomputeCalls);
    }

    @Test
    void delayedFutureUnconvergedEndStartsExactlyOneNewRequest() {
        Harness h = new Harness();
        h.loadWorlds();
        h.adapter.nextReloadDelayed = true;
        h.packA.desired = true;
        assertTrue(h.orch.onRuleChanged("ruleA", h.serverA, null));
        RecipeReloadGate.Outcome snapshot = h.orch.gate().ownOutcome();
        assertEquals(RecipeReloadGate.Outcome.PENDING, snapshot);

        // reload 未应用目标集合（选中态未变）→ END 后 pass 再走收敛判定，启动恰好一个新请求；
        // 原请求的 ownOp 被新请求覆盖——以请求计数与 failureCount 区分原请求与新请求，
        // 不得把新请求的结局误读为原请求的结算
        h.adapter.lastPendingFuture.complete(null);
        h.orch.onReloadEnd(h.serverA, true);
        assertEquals(2, h.orch.gate().reloadRequestCount());
        assertEquals(0, h.orch.gate().failureCount());
        assertEquals(RecipeReloadGate.Outcome.SUCCESS, h.orch.gate().ownOutcome());
        assertTrue(h.adapter.selected.contains("carpet-ice-addition:pack_a"));
    }

    @Test
    void externalReloadRunsPassButNeverInitiatesReloadOrTouchesOwnOutcome() {
        Harness h = new Harness();
        h.loadWorlds();
        int recomputeBefore = h.packA.recomputeCalls;
        // 外部（非自有）reload：START/END 对 → 正常 END 置 pendingSyncPass，静默后应重跑 pass
        h.externalReload(true);
        assertEquals(recomputeBefore + 1, h.packA.recomputeCalls);
        assertEquals(recomputeBefore + 1, h.packB.recomputeCalls);
        // 已收敛 → 不发起 reload；从未建立自有请求 → 无结局可篡改；不消耗重试预算
        assertEquals(0, h.adapter.reloadRequests.size());
        assertNull(h.orch.gate().ownOutcome());
        assertFalse(h.orch.gate().isRetryUsed());
        assertFalse(h.orch.gate().isDegraded());
    }

    // ------------------------------------------------------------------ 逐包登录同步

    @Test
    void playerJoinGateSyncsOnlyConvergedPacksAndReportsLockedOnes() {
        Harness h = new Harness();
        // 未绑定：安全 no-op
        h.orch.onPlayerLoggedIn(null, h.lockedKeys::add);
        assertEquals(0, h.packA.joinCalls);

        h.loadWorlds();
        // A：desired=true 且已选中（收敛）；B：desired=false 且未选中（收敛）→ 两包都同步
        h.packA.desired = true;
        h.adapter.selected.add("carpet-ice-addition:pack_a");
        h.orch.onPlayerLoggedIn(null, h.lockedKeys::add);
        assertEquals(1, h.packA.joinCalls);
        assertEquals(1, h.packB.joinCalls);
        assertTrue(h.lockedKeys.isEmpty());

        // A 进入未收敛窗口（选中态与期望不一致）且被冲突锁定：A 跳过同步、sink 收到锁定提示；B 不受影响
        h.adapter.selected.remove("carpet-ice-addition:pack_a");
        h.packA.locked = true;
        h.orch.onPlayerLoggedIn(null, h.lockedKeys::add);
        assertEquals(1, h.packA.joinCalls);
        assertEquals(2, h.packB.joinCalls);
        assertEquals(List.of("carpet.rule.ruleA.conflict.locked"), h.lockedKeys);
    }

    // ------------------------------------------------------------------ 服务器身份生命周期

    @Test
    void lateEndAfterCloseAndDoubleCloseAreIgnored() {
        Harness h = new Harness();
        h.loadWorlds();
        h.orch.onServerClosed(h.serverA);
        int logSize = h.eventLog.size();
        int resets = h.packA.resetCalls;
        int targets = h.adapter.targetUpdates.size();

        // 关闭后的迟到 END：忽略（无 pass、无广播路径、无目标更新）
        h.orch.onReloadEnd(h.serverA, true);
        assertEquals(logSize, h.eventLog.size());
        assertEquals(targets, h.adapter.targetUpdates.size());
        // 重复关闭：幂等（无二次锁状态复位、无目标更新）
        h.orch.onServerClosed(h.serverA);
        assertEquals(resets, h.packA.resetCalls);
        assertEquals(targets, h.adapter.targetUpdates.size());
    }

    @Test
    void rebindAfterCloseFullyResetsAndRunsNewCycle() {
        Harness h = new Harness();
        h.loadWorlds();
        int resets = h.packA.resetCalls;
        h.orch.onServerClosed(h.serverA);
        h.orch.onServerLoadedWorlds(h.serverB, null);
        // 换服务器：完整重置（锁状态重放）+ 新周期可编排（worlds 重新标记 → pass 执行）
        assertEquals(resets + 2, h.packA.resetCalls);
        assertEquals(2, h.packA.recomputeCalls);
        // 绑定 B 后，来自旧服务器 A 的 END 被令牌比较忽略
        int logSize = h.eventLog.size();
        h.orch.onReloadEnd(h.serverA, true);
        assertEquals(logSize, h.eventLog.size());
    }

    // ------------------------------------------------------------------ Adapter 目标一致性

    @Test
    void updateTargetOnlyFollowsActualBindAndUnbindTransitions() {
        Harness h = new Harness();
        // 未知规则：不绑定、不更新目标
        assertFalse(h.orch.onRuleChanged("unknownRule", h.serverA, null));
        assertTrue(h.adapter.targetUpdates.isEmpty());
        assertEquals(0, h.packA.resetCalls);

        h.loadWorlds();
        int targets = h.adapter.targetUpdates.size();
        // 错误服务器关闭：忽略（目标与状态均不变）
        h.orch.onServerClosed(h.serverB);
        assertEquals(targets, h.adapter.targetUpdates.size());
        // 解绑 + 重新绑定：每次迁移恰一次目标更新
        h.orch.onServerClosed(h.serverA);
        h.orch.onServerLoadedWorlds(h.serverB, null);
        assertEquals(targets + 2, h.adapter.targetUpdates.size());
    }

    // ------------------------------------------------------------------ 桩与工具

    /** 伪造的服务器适配器：可控选中集合 / 线程标志 / reload 行为；onStart 镜像 Fabric 的 HEAD 注入 START。 */
    private static final class RecordingAdapter implements ServerAdapter {
        final LinkedHashSet<String> selected = new LinkedHashSet<>(List.of("vanilla"));
        boolean serverThread = true;
        boolean nextReloadThrows;
        boolean nextReloadFails;
        boolean nextReloadDelayed;
        CompletableFuture<Void> lastPendingFuture;
        /** 由 Harness 在编排核心构造后接线：每次 reloadResources 入口同步触发（镜像 START@HEAD）。 */
        Runnable onStart;
        final List<Collection<String>> reloadRequests = new ArrayList<>();
        final List<MinecraftServer> targetUpdates = new ArrayList<>();
        private final List<String> eventLog;

        RecordingAdapter(List<String> eventLog) {
            this.eventLog = eventLog;
        }

        @Override
        public boolean isServerThread() {
            return serverThread;
        }

        @Override
        public Collection<String> selectedPackIds() {
            return selected;
        }

        @Override
        public CompletableFuture<Void> reloadResources(Collection<String> selectedIds) {
            reloadRequests.add(List.copyOf(selectedIds));
            eventLog.add("reload");
            if (onStart != null) {
                onStart.run();
            }
            if (nextReloadThrows) {
                nextReloadThrows = false;
                throw new IllegalStateException("synchronous reload failure");
            }
            if (nextReloadFails) {
                nextReloadFails = false;
                return CompletableFuture.failedFuture(new RuntimeException("asynchronous reload failure"));
            }
            if (nextReloadDelayed) {
                nextReloadDelayed = false;
                lastPendingFuture = new CompletableFuture<>();
                return lastPendingFuture;
            }
            // 正常完成：reload 应用所请求的选中集合（真实 reloadResources 的效果）
            selected.clear();
            selected.addAll(selectedIds);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void updateTarget(MinecraftServer server) {
            targetUpdates.add(server);
        }
    }

    /** 记录型受管包：钩子只计数与记日志，不解引用 server / player 参数。 */
    private static final class RecordingPack {
        final String packId;
        final String ruleName;
        final String tag;
        private final List<String> eventLog;

        volatile boolean desired;
        volatile boolean locked;
        boolean failRecompute;
        boolean failSync;
        int recomputeCalls;
        int syncCalls;
        int disableCalls;
        int joinCalls;
        int resetCalls;

        RecordingPack(String packId, String ruleName, String tag, List<String> eventLog) {
            this.packId = packId;
            this.ruleName = ruleName;
            this.tag = tag;
            this.eventLog = eventLog;
        }

        RecipePackCoordinator.ManagedPack toManagedPack() {
            return new RecipePackCoordinator.ManagedPack(
                    Identifier.tryParse(packId),
                    ruleName,
                    "carpet.rule." + ruleName + ".conflict.locked",
                    () -> locked,
                    () -> desired,
                    server -> {
                        recomputeCalls++;
                        eventLog.add(tag + ":recompute");
                        if (failRecompute) {
                            throw new RuntimeException(tag + " recompute failed");
                        }
                    },
                    server -> {
                        syncCalls++;
                        eventLog.add(tag + ":sync");
                        if (failSync) {
                            throw new RuntimeException(tag + " sync failed");
                        }
                    },
                    server -> {
                        disableCalls++;
                        eventLog.add(tag + ":onPackDisabled");
                    },
                    (server, player) -> {
                        joinCalls++;
                        eventLog.add(tag + ":join");
                    },
                    () -> {
                        resetCalls++;
                        eventLog.add(tag + ":resetLockState");
                    });
        }
    }

    /** 每个测试方法独立实例：编排核心全部运行期状态天然隔离。 */
    private static final class Harness {
        final Object serverA = new Object();
        final Object serverB = new Object();
        final List<String> eventLog = new ArrayList<>();
        final List<String> lockedKeys = new ArrayList<>();
        final RecordingAdapter adapter = new RecordingAdapter(eventLog);
        final RecordingPack packA = new RecordingPack("carpet-ice-addition:pack_a", "ruleA", "A", eventLog);
        final RecordingPack packB = new RecordingPack("carpet-ice-addition:pack_b", "ruleB", "B", eventLog);
        final RecipePackOrchestrator orch = new RecipePackOrchestrator(
                List.of(packA.toManagedPack(), packB.toManagedPack()), adapter);

        Harness() {
            adapter.onStart = () -> orch.onReloadStart(serverA, null);
        }

        void loadWorlds() {
            orch.onServerLoadedWorlds(serverA, null);
        }

        void externalReload(boolean success) {
            orch.onReloadStart(serverA, null);
            orch.onReloadEnd(serverA, success);
        }
    }
}
