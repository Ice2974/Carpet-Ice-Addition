package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link RecipePackReconciler} 的纯函数规划测试。 */
class RecipePackReconcilerTest {
    private static final String CORAL = "carpet-ice-addition:craftable_coral_blocks";
    private static final String CALCITE = "carpet-ice-addition:calcite_stonecutting";

    private static List<RecipePackReconciler.PackDesire> desires(boolean coral, boolean calcite) {
        return List.of(
                new RecipePackReconciler.PackDesire(CORAL, "craftableCoralBlocks", coral),
                new RecipePackReconciler.PackDesire(CALCITE, "calciteStonecuttingRecipe", calcite));
    }

    @Test
    void planningIsIdempotentAndConvergesToFixedPoint() {
        LinkedHashSet<String> selected = new LinkedHashSet<>(List.of("vanilla"));

        RecipePackReconciler.Plan first = RecipePackReconciler.plan(selected, desires(true, true));
        assertTrue(first.changed());
        assertEquals(List.of("vanilla", CORAL, CALCITE), List.copyOf(first.next()));
        assertTrue(first.disabledPackIds().isEmpty());

        RecipePackReconciler.Plan second = RecipePackReconciler.plan(first.next(), desires(true, true));
        assertFalse(second.changed(), "固定点：再次规划不得产生变化（禁止无意义 reload）");
        assertTrue(second.disabledPackIds().isEmpty());
    }

    @Test
    void bothRulesEnableInOnePlanSoOneReloadCoversBoth() {
        LinkedHashSet<String> selected = new LinkedHashSet<>(List.of("vanilla"));
        RecipePackReconciler.Plan plan = RecipePackReconciler.plan(selected, desires(true, true));
        assertTrue(plan.next().contains(CORAL));
        assertTrue(plan.next().contains(CALCITE));
    }

    @Test
    void disablingRecordsPacksForImmediateCleanup() {
        LinkedHashSet<String> selected = new LinkedHashSet<>(List.of("vanilla", CORAL, CALCITE));
        RecipePackReconciler.Plan plan = RecipePackReconciler.plan(selected, desires(false, true));
        assertEquals(List.of(CORAL), plan.disabledPackIds(), "由选中变为未选中的包必须先做即时清理");
        assertFalse(plan.next().contains(CORAL));
        assertTrue(plan.next().contains(CALCITE));
        assertTrue(plan.changed());
    }

    @Test
    void disablingAnAlreadyDeselectedPackIsNotAChangeAndRunsNoCleanup() {
        LinkedHashSet<String> selected = new LinkedHashSet<>(List.of("vanilla"));
        RecipePackReconciler.Plan plan = RecipePackReconciler.plan(selected, desires(false, false));
        assertFalse(plan.changed());
        assertTrue(plan.disabledPackIds().isEmpty(), "未选中的包不得触发即时清理");
    }

    @Test
    void conflictLockedRuleIsTreatedAsDisabled() {
        LinkedHashSet<String> selected = new LinkedHashSet<>(List.of("vanilla", CORAL));
        // 锁定：规则字段为 true，但 effective() 为 false
        RecipePackReconciler.Plan plan = RecipePackReconciler.plan(selected, desires(false, false));
        assertTrue(plan.changed());
        assertEquals(List.of(CORAL), plan.disabledPackIds());
    }

    @Test
    void userDatapacksArePreservedWithRelativeOrder() {
        LinkedHashSet<String> selected = new LinkedHashSet<>(
                List.of("vanilla", "file/user_pack.zip", CORAL, "file/another.zip"));
        RecipePackReconciler.Plan plan = RecipePackReconciler.plan(selected, desires(true, true));
        assertEquals(
                List.of("vanilla", "file/user_pack.zip", CORAL, "file/another.zip", CALCITE),
                List.copyOf(plan.next()),
                "用户数据包不得被移除，相对顺序必须保持");
    }

    @Test
    void targetKeyIsOrderSensitiveAndStable() {
        LinkedHashSet<String> a = new LinkedHashSet<>(List.of("vanilla", CORAL));
        LinkedHashSet<String> b = new LinkedHashSet<>(List.of("vanilla", CORAL));
        assertEquals(RecipePackReconciler.targetKey(a), RecipePackReconciler.targetKey(b));
        assertFalse(RecipePackReconciler.targetKey(List.of(CORAL, "vanilla"))
                .equals(RecipePackReconciler.targetKey(List.of("vanilla", CORAL))),
                "选中顺序即数据包优先级，键必须顺序敏感");
    }

    @Test
    void packConvergedRequiresSelectionToMatchDesire() {
        LinkedHashSet<String> selected = new LinkedHashSet<>(List.of("vanilla", CORAL));

        assertTrue(RecipePackReconciler.packConverged(selected, CORAL, true));
        assertTrue(RecipePackReconciler.packConverged(selected, CALCITE, false));
        assertFalse(RecipePackReconciler.packConverged(selected, CORAL, false), "选中但期望关闭 ⇒ 未收敛");
        assertFalse(RecipePackReconciler.packConverged(selected, CALCITE, true), "未选中但期望开启 ⇒ 未收敛");
    }

    @Test
    void packConvergedIsFalseForMissingInputs() {
        LinkedHashSet<String> selected = new LinkedHashSet<>(List.of(CORAL));

        assertFalse(RecipePackReconciler.packConverged(null, CORAL, true));
        assertFalse(RecipePackReconciler.packConverged(selected, null, true));
        assertFalse(RecipePackReconciler.packConverged(selected, "", true));
    }
}
