package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code calciteStonecuttingRecipe} 解锁同步的纯 Java 复刻测试。
 *
 * <p>复刻三样东西，并在 {@link FakeServer} 内逐条标注依据：
 * <ol>
 *   <li><b>协调器钩子顺序</b>：静默点 pass（先冲突重算、再菜单/配方书同步）→ {@code onPackDisabled}
 *       （取消选中之前）→ 改选 → END 后再次 pass；登录钩子先过「本包收敛」门再执行。对齐
 *       {@code RecipePackCoordinator#orchestrate / #requestReconcile / #shouldSyncPackOnJoin}。</li>
 *   <li><b>原版配方书语义</b>：{@code ServerRecipeBook.addRecipes} 只对「尚未在解锁集合内」的 id
 *       写入记录、发包并按 {@code Recipe#showNotification()} 触发提示；{@code removeRecipes} 只移除
 *       已解锁的 id；登录 INIT 包通知标志为 false；登录时 {@code unpack} 谓词会清理当前
 *       {@code RecipeManager} 中不存在的记录（日志行为不作断言，见 {@code offlinePlayerRecordIsPruned...}）。</li>
 *   <li><b>两段独立尝试</b>：配方书段与菜单段分别经生产 {@link IsolatedHookRunner#runAll} 执行，
 *       再用生产 {@link IsolatedHookRunner.Result#plus} 合并上报（前一段失败不得跳过后一段）。</li>
 * </ol>
 *
 * <p>授予判据直接调用生产纯函数 {@link CalciteStonecuttingRecipeData#unlockAllowed}，避免复刻与生产漂移；
 * 撤销前的「内容仍符合规则承诺」判定在生产中由
 * {@code CalciteStonecuttingRecipeConflictDetector#matchesRuleContract} 提供（依赖 Minecraft 类型），
 * 此处以等价布尔输入建模。
 */
class CalciteStonecuttingUnlockSimulationTest {
    private static final String CALCITE_ID =
            CalciteStonecuttingRecipeData.NAMESPACE + ":" + CalciteStonecuttingRecipeData.RECIPE_PATH;
    private static final String SYNC_DESCRIPTION =
            "calciteStonecuttingRecipe: stonecutter menu sync and recipe unlock";

    // ---------------------------------------------------------------- 复刻模型

    /** 复刻服务端侧：协调器钩子 + 原版配方书语义。 */
    private static final class FakeServer {
        /** 在线玩家 → 已解锁 recipe id 集合（顺序固定，便于断言）。 */
        final Map<String, Set<String>> unlocked = new LinkedHashMap<>();
        /** 规则 effective 值（字段 ∧ 未冲突锁定）。 */
        boolean ruleEffective;
        /** 本模组内置包当前是否在选中集合内。 */
        boolean packSelected;
        /** 本规则配方能否从**当前** RecipeManager 解析出 holder（受包选中 + 加载成功影响）。 */
        boolean recipeResolvable;
        /** 同 id 配方的内容是否仍符合规则承诺（生产：matchesRuleContract）。 */
        boolean contentMatchesContract = true;

        int toasts;
        int addPackets;
        int removePackets;
        RuntimeException lastAggregated;
        final List<String> eventLog = new ArrayList<>();
        final Set<String> failBookStageFor = new LinkedHashSet<>();
        final Set<String> failMenuStageFor = new LinkedHashSet<>();

        void join(String player) {
            unlocked.putIfAbsent(player, new LinkedHashSet<>());
        }

        /** 复刻 ServerRecipeBook#addRecipes 的「新解锁才记录 + 发包 + 提示」。 */
        private void award(String player) {
            if (unlocked.get(player).add(CALCITE_ID)) {
                addPackets++;
                toasts++;
                eventLog.add("award:" + player);
            }
        }

        /** 复刻 ServerRecipeBook#removeRecipes 的「只移除已解锁 id」。 */
        private void revoke(String player) {
            if (unlocked.get(player).remove(CALCITE_ID)) {
                removePackets++;
                eventLog.add("revoke:" + player);
            }
        }

        /** 复刻 PlayerManager 的 INIT：只回填记录，notification=false ⇒ 不提示。 */
        void loginInit(String player) {
            join(player);
            eventLog.add("init:" + player);
        }

        /**
         * 复刻登录时 {@code unpack(packed, 谓词)} 的清理：配方不可解析 ⇒ 记录被移除。
         * 原版同时会打印 ERROR 日志，此处刻意不断言日志内容（行数随版本 / NBT 集合变化）。
         */
        void loginPrune(String player) {
            if (!recipeResolvable && unlocked.get(player).remove(CALCITE_ID)) {
                eventLog.add("prune:" + player);
            }
        }

        /** 复刻静默点 pass：配方书段（授予）与菜单段分别独立尝试，最后合并上报一次。 */
        void pass() {
            boolean[] apply = new boolean[1];
            IsolatedHookRunner.Result resolve = IsolatedHookRunner.runAll(List.of(() -> apply[0] =
                    CalciteStonecuttingRecipeData.unlockAllowed(ruleEffective, packSelected, recipeResolvable)));

            List<Runnable> bookStage = new ArrayList<>();
            List<Runnable> menuStage = new ArrayList<>();
            for (String player : unlocked.keySet()) {
                if (apply[0]) {
                    bookStage.add(() -> {
                        if (failBookStageFor.contains(player)) {
                            throw new IllegalStateException("book stage failed for " + player);
                        }
                        award(player);
                    });
                }
                menuStage.add(() -> {
                    if (failMenuStageFor.contains(player)) {
                        throw new IllegalStateException("menu stage failed for " + player);
                    }
                    eventLog.add("menu:" + player);
                });
            }
            recordResult(resolve
                    .plus(IsolatedHookRunner.runAll(bookStage))
                    .plus(IsolatedHookRunner.runAll(menuStage)));
        }

        /** 复刻 onPackDisabled：撤销段与结果槽清理段分别独立尝试，最后合并上报一次。 */
        void packDisabled() {
            boolean[] apply = new boolean[1];
            IsolatedHookRunner.Result resolve = IsolatedHookRunner.runAll(List.of(
                    () -> apply[0] = recipeResolvable && contentMatchesContract));

            List<Runnable> revokeStage = new ArrayList<>();
            List<Runnable> cleanupStage = new ArrayList<>();
            for (String player : unlocked.keySet()) {
                if (apply[0]) {
                    revokeStage.add(() -> {
                        if (failBookStageFor.contains(player)) {
                            throw new IllegalStateException("revoke stage failed for " + player);
                        }
                        revoke(player);
                    });
                }
                cleanupStage.add(() -> {
                    if (failMenuStageFor.contains(player)) {
                        throw new IllegalStateException("cleanup stage failed for " + player);
                    }
                    eventLog.add("clearResult:" + player);
                });
            }
            IsolatedHookRunner.Result result = resolve
                    .plus(IsolatedHookRunner.runAll(revokeStage))
                    .plus(IsolatedHookRunner.runAll(cleanupStage));
            eventLog.add("deselect");            // 协调器在 onPackDisabled 之后才改选（$4）
            recordResult(result);
        }
        /**
         * 复刻协调器登录路径：先过逐包收敛门（选中状态 == 期望，即 {@code ruleEffective}），
         * 再执行本模组登录钩子（钩子内仍走生产授予门）。
         */
        void joinHook(String player) {
            if (packSelected != ruleEffective) {
                eventLog.add("joinGateBlocked:" + player);
                return;
            }
            recordResult(IsolatedHookRunner.runAll(List.of(() -> {
                if (CalciteStonecuttingRecipeData.unlockAllowed(ruleEffective, packSelected, recipeResolvable)) {
                    award(player);
                }
            })));
        }

        private void recordResult(IsolatedHookRunner.Result result) {
            lastAggregated = result.ok() ? null : result.aggregatedException(SYNC_DESCRIPTION);
        }

        void clearObservations() {
            toasts = 0;
            addPackets = 0;
            removePackets = 0;
            eventLog.clear();
            lastAggregated = null;
        }
    }

    /** 规则启用 + 本包已选中 + 配方可解析：授予门齐备。 */
    private static FakeServer enabledServer(String... players) {
        FakeServer server = new FakeServer();
        for (String player : players) {
            server.join(player);
        }
        server.ruleEffective = true;
        server.packSelected = true;
        server.recipeResolvable = true;
        return server;
    }

    // ---------------------------------------------------------------- 提示语义

    @Test
    void enablingToastsOncePerUnlockedPlayerAndLaterPassesSendNoPackets() {
        FakeServer server = enabledServer("alice", "bob");
        server.ruleEffective = false;
        server.packSelected = false;
        server.recipeResolvable = false;
        server.pass();

        assertEquals(0, server.toasts, "本包未选中时不得授予");
        assertEquals(0, server.addPackets);

        server.ruleEffective = true;
        server.packSelected = true;
        server.recipeResolvable = true;
        server.pass();
        assertEquals(2, server.toasts, "每个未解锁玩家恰好一次原版解锁提示");
        assertEquals(2, server.addPackets);

        server.clearObservations();
        server.pass();
        assertEquals(0, server.toasts, "已解锁玩家不得重复提示");
        assertEquals(0, server.addPackets, "已解锁玩家不得再发配方书包（无意义授予操作）");
    }

    @Test
    void alreadyUnlockedPlayerReceivesNoSecondToast() {
        FakeServer server = enabledServer("alice", "bob");
        server.unlocked.get("alice").add(CALCITE_ID);   // 复刻：此前已在切石机取出过结果（原版授予）

        server.pass();

        assertEquals(1, server.toasts, "只对尚未解锁的玩家承诺一次提示");
        assertFalse(server.eventLog.contains("award:alice"), "已解锁玩家必须走原版空操作分支");
        assertTrue(server.eventLog.contains("award:bob"));
    }

    @Test
    void unrelatedReloadAndExternalReloadDoNotToastAgain() {
        FakeServer server = enabledServer("alice");
        server.pass();
        assertEquals(1, server.toasts);

        server.clearObservations();
        server.pass();                                   // 无关重载触发的静默点 pass
        for (String player : server.unlocked.keySet()) {
            server.loginInit(player);                    // 外部 /reload：原版 INIT 通知标志为 false
        }
        server.pass();
        assertEquals(0, server.toasts);
        assertEquals(0, server.addPackets);
        assertTrue(server.unlocked.get("alice").contains(CALCITE_ID), "重载不得清除解锁记录");
    }

    @Test
    void reloginKeepsUnlockWithoutToastAndJoinHookIsIdempotent() {
        FakeServer server = enabledServer("alice");
        server.pass();
        server.clearObservations();

        server.loginInit("alice");
        server.loginPrune("alice");
        server.joinHook("alice");

        assertEquals(0, server.toasts, "重登不得重复提示");
        assertEquals(0, server.addPackets);
        assertTrue(server.unlocked.get("alice").contains(CALCITE_ID));
        assertNull(server.lastAggregated);
    }

    @Test
    void freshPlayerJoiningWhileRuleOnToastsExactlyOnce() {
        FakeServer server = enabledServer("alice");
        server.pass();
        server.clearObservations();

        server.join("carol");
        server.loginInit("carol");
        server.joinHook("carol");

        assertEquals(1, server.toasts, "规则启用中登录的未解锁玩家恰好一次提示");
        assertEquals(1, server.addPackets);
        assertEquals(List.of("award:carol"),
                server.eventLog.stream().filter(event -> event.startsWith("award:")).toList(),
                "不得出现第二次授予");
    }

    @Test
    void joinGateBlocksSyncWhileOwnPackIsNotConverged() {
        FakeServer server = enabledServer("alice");
        server.pass();
        server.clearObservations();

        server.packSelected = false;                     // 本包迁移中（$4 之前的异步窗口）
        server.recipeResolvable = false;
        server.join("carol");
        server.joinHook("carol");

        assertEquals(0, server.toasts, "本包未收敛时不得在登录路径授予");
        assertTrue(server.eventLog.contains("joinGateBlocked:carol"), "登录门必须先行拦下");
        assertNull(server.lastAggregated, "被门拦下不是异常");
    }

    // ---------------------------------------------------------------- 关闭 / 重新启用

    @Test
    void disableRevokesBeforeDeselectionAndReenableToastsAgain() {
        FakeServer server = enabledServer("alice");
        server.pass();
        server.clearObservations();

        server.ruleEffective = false;
        server.packDisabled();

        assertFalse(server.unlocked.get("alice").contains(CALCITE_ID), "关闭必须撤销本模组授予的解锁记录");
        assertEquals(0, server.toasts, "撤销本身不产生提示");
        assertEquals(1, server.removePackets);
        int revokeIndex = server.eventLog.indexOf("revoke:alice");
        int deselectIndex = server.eventLog.indexOf("deselect");
        assertTrue(revokeIndex >= 0 && revokeIndex < deselectIndex, "撤销必须先于选中集合变更");
        assertTrue(server.eventLog.contains("clearResult:alice"), "结果槽即时清理必须仍然执行");

        server.clearObservations();
        server.ruleEffective = true;
        server.pass();

        assertEquals(1, server.toasts, "成功撤销后重新启用才再次提示");
        assertEquals(1, server.addPackets);
    }

    @Test
    void disableWithoutRevokeKeepsRecordSoReenableDoesNotToast() {
        FakeServer server = enabledServer("alice");
        server.pass();
        server.clearObservations();

        server.ruleEffective = false;
        server.contentMatchesContract = false;           // 同 id 已被外部数据包覆盖
        server.packDisabled();

        assertTrue(server.unlocked.get("alice").contains(CALCITE_ID),
                "内容已属外部数据包时不得撤销（不替外部内容做解锁操作）");
        assertEquals(0, server.removePackets);
        assertEquals(0, server.toasts);

        server.clearObservations();
        server.ruleEffective = true;
        server.pass();
        assertEquals(0, server.toasts, "记录未撤销 ⇒ 重新启用不会再次提示（原版语义，见方案 §4.3）");
    }

    @Test
    void disableWhileSelectionStillConvergingNeverToastsAndThenRevokes() {
        FakeServer server = enabledServer("alice");
        server.pass();
        server.clearObservations();

        server.ruleEffective = false;                    // 规则已关闭，但本包仍在选中集合内（reload 进行中）
        server.pass();
        assertEquals(0, server.toasts, "规则关闭后即使配方仍可解析也不得授予");
        assertFalse(server.eventLog.stream().anyMatch(event -> event.startsWith("award:")));

        server.packDisabled();
        assertFalse(server.unlocked.get("alice").contains(CALCITE_ID));
        assertEquals(0, server.toasts);
    }

    // ---------------------------------------------------------------- 授予门

    @Test
    void unresolvableRecipeNeverCreatesSilentUnlockRecord() {
        FakeServer server = enabledServer("alice");
        server.recipeResolvable = false;                 // 包已选中但配方尚未加载 / 加载失败
        server.pass();

        assertTrue(server.unlocked.get("alice").isEmpty(),
                "配方不可解析时不得写入解锁记录，否则后续授予会因「已解锁」永久静默");
        assertEquals(0, server.toasts);

        server.clearObservations();
        server.recipeResolvable = true;
        server.pass();

        assertEquals(1, server.toasts, "配方可解析后必须仍然能弹出一次提示");
        assertEquals(1, server.addPackets);
    }

    @Test
    void unselectedPackNeverAwardsEvenIfSameIdResolves() {
        FakeServer server = enabledServer("alice");
        server.packSelected = false;                     // 同 id 由外部数据包提供（冲突检测异常时的危险场景）
        server.pass();

        assertTrue(server.unlocked.get("alice").isEmpty(), "本包未选中时不得授予外部同 id 配方");
        assertEquals(0, server.toasts);
        assertEquals(0, server.addPackets);
    }

    // ---------------------------------------------------------------- 离线玩家

    @Test
    void offlinePlayerRecordIsPrunedOnLoginWithoutToastAndWithoutLogAssertions() {
        FakeServer server = enabledServer("alice");
        server.pass();
        assertEquals(1, server.toasts);

        // 玩家离线：关闭规则时她不在在线集合内 ⇒ 撤销段不会覆盖她（模型：暂时移出在线登记）
        server.unlocked.remove("alice");
        server.ruleEffective = false;
        server.packSelected = false;
        server.recipeResolvable = false;
        server.packDisabled();
        server.clearObservations();

        server.unlocked.put("alice", new LinkedHashSet<>(Set.of(CALCITE_ID)));   // 记录仍在玩家 NBT 中
        server.loginPrune("alice");

        assertTrue(server.unlocked.get("alice").isEmpty(), "登录时必须清理已不存在的配方记录");
        assertEquals(0, server.toasts, "清理不产生提示");
        // 原版同时会打印 ERROR 日志；按约束只作观测项，此处刻意不断言日志行数。
    }

    // ---------------------------------------------------------------- 两段独立尝试

    @Test
    void bookStageFailureDoesNotSkipMenuStageForSameOrOtherPlayers() {
        FakeServer server = enabledServer("alice", "bob");
        server.failBookStageFor.add("alice");

        server.pass();

        assertTrue(server.eventLog.contains("menu:alice"), "配方书段失败不得跳过同一玩家的菜单刷新");
        assertTrue(server.eventLog.contains("menu:bob"));
        assertTrue(server.eventLog.contains("award:bob"), "其它玩家的授予必须照常执行");
        assertFalse(server.unlocked.get("alice").contains(CALCITE_ID));
        assertEquals(1, server.toasts);

        assertNotNull(server.lastAggregated, "任一步失败必须汇总上报");
        assertTrue(server.lastAggregated.getMessage().contains("1/5"),
                "计数必须覆盖「解析 + 配方书段 + 菜单段」全部尝试：" + server.lastAggregated.getMessage());
        assertTrue(server.lastAggregated.getMessage().contains("book stage failed for alice"),
                "首个根因必须出现在聚合消息里：" + server.lastAggregated.getMessage());
    }

    @Test
    void menuStageFailureStillLeavesBookStageCompleted() {
        FakeServer server = enabledServer("alice", "bob");
        server.failMenuStageFor.add("bob");

        server.pass();

        assertTrue(server.eventLog.contains("award:alice"), "菜单段失败不得回滚 / 跳过配方书段");
        assertTrue(server.eventLog.contains("award:bob"));
        assertEquals(2, server.toasts);
        assertTrue(server.eventLog.contains("menu:alice"), "其它玩家的菜单刷新必须照常执行");

        assertNotNull(server.lastAggregated);
        assertTrue(server.lastAggregated.getMessage().contains("1/5"),
                "菜单段失败同样必须计入汇总：" + server.lastAggregated.getMessage());
        assertTrue(server.lastAggregated.getMessage().contains("menu stage failed for bob"));
    }

    @Test
    void revokeStageFailureDoesNotSkipResultSlotCleanup() {
        FakeServer server = enabledServer("alice");
        server.pass();
        server.clearObservations();
        server.ruleEffective = false;
        server.failBookStageFor.add("alice");

        server.packDisabled();

        assertTrue(server.eventLog.contains("clearResult:alice"), "撤销段失败不得跳过结果槽清理");
        assertTrue(server.unlocked.get("alice").contains(CALCITE_ID), "失败时记录保持（不假装成功）");
        assertNotNull(server.lastAggregated);
        assertTrue(server.lastAggregated.getMessage().contains("1/3"),
                "撤销段与清理段都必须计入汇总：" + server.lastAggregated.getMessage());
        assertTrue(server.lastAggregated.getMessage().contains("revoke stage failed for alice"),
                server.lastAggregated.getMessage());
    }
}
