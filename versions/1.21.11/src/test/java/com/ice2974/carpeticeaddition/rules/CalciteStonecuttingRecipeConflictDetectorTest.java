package com.ice2974.carpeticeaddition.rules;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code calciteStonecuttingRecipe} 冲突检测器的边界单测：
 *
 * <ul>
 *   <li>外部切石配方的产物解析异常按 fail-closed 计为冲突（任一 display 异常即命中，
 *       即使其余 display 成功解析、甚至产物已能判定）；</li>
 *   <li>display 为空 / 解析结果为空不计为冲突（与解析异常是不同分支）；</li>
 *   <li>overworld 缺失（此处以 null server 驱动同一早退守卫的 {@code server == null} 左支；
 *       「server 非 null 而 overworld 为 null」的右支无法在无真实服务器的单测中执行，
 *       依据同一 {@code ||} 条件与单一 return 的代码同构性，不宣称已被单测覆盖）时
 *       不解除已有冲突锁定、不动 desiredValue。</li>
 * </ul>
 */
class CalciteStonecuttingRecipeConflictDetectorTest {
    private static ContextMap ctx;
    private static Set<String> targets;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ctx = new ContextMap.Builder().create(new ContextKeySet.Builder().build());
        targets = new HashSet<>(CalciteStonecuttingRecipeData.RESULT_ITEM_IDS);
    }

    @AfterEach
    void resetLockState() {
        CalciteStonecuttingRecipeState.LOCK.reset();
    }

    @Test
    void externalStonecuttingRecipeProducingCalciteIsConflict() {
        Recipe<?> recipe = new StubRecipe(RecipeType.STONECUTTING, List.of(displayOf(Items.CALCITE)));
        assertTrue(CalciteStonecuttingRecipeConflictDetector.hasConflictingResult(
                recipe, "minecraft:conflicting", ctx, targets));
    }

    @Test
    void nonStonecuttingTypeIsNeverConflict() {
        Recipe<?> recipe = new StubRecipe(RecipeType.CRAFTING, List.of(displayOf(Items.CALCITE)));
        assertFalse(CalciteStonecuttingRecipeConflictDetector.hasConflictingResult(
                recipe, "minecraft:conflicting", ctx, targets));
    }

    @Test
    void stonecuttingRecipeWithoutResolvableDisplayIsNotConflict() {
        // 空 display 列表 = 解析结果为空（合法场景），与解析异常是不同分支
        Recipe<?> recipe = new StubRecipe(RecipeType.STONECUTTING, List.of());
        assertFalse(CalciteStonecuttingRecipeConflictDetector.hasConflictingResult(
                recipe, "minecraft:conflicting", ctx, targets));
    }

    @Test
    void throwingDisplayAloneIsConflict() {
        Recipe<?> recipe = new StubRecipe(RecipeType.STONECUTTING, List.of(throwingDisplay()));
        assertTrue(CalciteStonecuttingRecipeConflictDetector.hasConflictingResult(
                recipe, "minecraft:conflicting", ctx, targets));
    }

    @Test
    void throwingDisplayIsConflictEvenWhenOtherDisplaysResolveWithoutCalcite() {
        // 多 display 部分成功：其余 display 成功解析且不含方解石，仍因任一解析异常计为冲突
        Recipe<?> recipe = new StubRecipe(
                RecipeType.STONECUTTING, List.of(displayOf(Items.STONE), throwingDisplay()));
        assertTrue(CalciteStonecuttingRecipeConflictDetector.hasConflictingResult(
                recipe, "minecraft:conflicting", ctx, targets));
    }

    @Test
    void throwingDisplayIsConflictEvenWhenProductAlreadyMatches() {
        // 其余 display 已解析出方解石（冲突本就成立），叠加解析异常仍稳定判冲突
        Recipe<?> recipe = new StubRecipe(
                RecipeType.STONECUTTING, List.of(displayOf(Items.CALCITE), throwingDisplay()));
        assertTrue(CalciteStonecuttingRecipeConflictDetector.hasConflictingResult(
                recipe, "minecraft:conflicting", ctx, targets));
    }

    @Test
    void unresolvableRecipeWarnsOnceForEachRule() {
        var first = new RecipeConflictDefinition(RecipeConflictDefinition.Type.STONECUTTING,
                List.of("test:warning_first"), List.of("minecraft:calcite"));
        var second = new RecipeConflictDefinition(RecipeConflictDefinition.Type.STONECUTTING,
                List.of("test:warning_second"), List.of("minecraft:stone"));
        var messages = new ArrayList<String>();
        var appender = new AbstractAppender("recipe-conflict-test", null, null, false, null) {
            @Override public void append(LogEvent event) { messages.add(event.getMessage().getFormattedMessage()); }
        };
        var logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger("Carpet Ice Addition");
        appender.start();
        logger.addAppender(appender);
        try {
            Recipe<?> recipe = new StubRecipe(RecipeType.STONECUTTING, List.of(throwingDisplay()));
            assertTrue(StonecuttingRecipeConflictScanner.hasConflictingResult(recipe, "test:broken", ctx, first, first.targets()));
            assertTrue(StonecuttingRecipeConflictScanner.hasConflictingResult(recipe, "test:broken", ctx, first, first.targets()));
            assertTrue(StonecuttingRecipeConflictScanner.hasConflictingResult(recipe, "test:broken", ctx, second, second.targets()));
            assertTrue(StonecuttingRecipeConflictScanner.hasConflictingResult(recipe, "test:broken", ctx, second, second.targets()));
            assertEquals(2, messages.size());
            assertTrue(messages.get(0).contains("test:warning_first"));
            assertTrue(messages.get(1).contains("test:warning_second"));
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
    }

    @Test
    void sharedScannerKeepsEachRulesOwnIdsTypesAndTargetsIndependent() {
        record Entry(String id, Recipe<?> recipe) {}
        var entries = List.of(
                new Entry("test:craft_a", new StubRecipe(RecipeType.CRAFTING, List.of(displayOf(Items.CALCITE)))),
                new Entry("test:craft_b", new StubRecipe(RecipeType.CRAFTING, List.of(displayOf(Items.STONE)))),
                new Entry("test:stone_a", new StubRecipe(RecipeType.STONECUTTING, List.of(displayOf(Items.CALCITE)))),
                new Entry("test:stone_b", new StubRecipe(RecipeType.STONECUTTING, List.of(displayOf(Items.STONE)))));
        var craftA = new RecipeConflictDefinition(RecipeConflictDefinition.Type.CRAFTING, List.of("test:craft_a"), List.of("minecraft:calcite"));
        var craftB = new RecipeConflictDefinition(RecipeConflictDefinition.Type.CRAFTING, List.of("test:craft_b"), List.of("minecraft:stone"));
        var stoneA = new RecipeConflictDefinition(RecipeConflictDefinition.Type.STONECUTTING, List.of("test:stone_a"), List.of("minecraft:calcite"));
        var stoneB = new RecipeConflictDefinition(RecipeConflictDefinition.Type.STONECUTTING, List.of("test:stone_b"), List.of("minecraft:stone"));
        for (var definition : List.of(craftA, craftB, stoneA, stoneB)) {
            var own = new ArrayList<String>();
            boolean conflict = RecipeConflictScanner.scan(entries, definition,
                    entry -> RecipeConflictScanner.matchesType(definition, entry.recipe().getType()),
                    Entry::id, entry -> own.add(entry.id()), entry -> StonecuttingRecipeConflictScanner.hasConflictingResult(
                            entry.recipe(), entry.id(), ctx, definition, definition.targets()));
            assertFalse(conflict, definition.toString());
            assertEquals(definition.recipeIds(), own);
        }
        var externalStone = new Entry("test:external", new StubRecipe(RecipeType.STONECUTTING, List.of(displayOf(Items.CALCITE))));
        assertTrue(StonecuttingRecipeConflictScanner.hasConflictingResult(externalStone.recipe(), externalStone.id(), ctx, stoneA, stoneA.targets()));
        assertFalse(StonecuttingRecipeConflictScanner.hasConflictingResult(externalStone.recipe(), externalStone.id(), ctx, stoneB, stoneB.targets()));
        assertFalse(StonecuttingRecipeConflictScanner.hasConflictingResult(externalStone.recipe(), externalStone.id(), ctx, craftA, craftA.targets()));
        var externalCraft = new Entry("test:external_craft", new StubRecipe(RecipeType.CRAFTING, List.of(displayOf(Items.CALCITE))));
        assertTrue(StonecuttingRecipeConflictScanner.hasConflictingResult(externalCraft.recipe(), externalCraft.id(), ctx, craftA, craftA.targets()));
        assertFalse(StonecuttingRecipeConflictScanner.hasConflictingResult(externalCraft.recipe(), externalCraft.id(), ctx, craftB, craftB.targets()));
        assertFalse(StonecuttingRecipeConflictScanner.hasConflictingResult(externalCraft.recipe(), externalCraft.id(), ctx, stoneA, stoneA.targets()));
    }

    @Test
    void recomputeWithUndeterminableServerKeepsLockStateAndDesiredValue() {
        CalciteStonecuttingRecipeState.LOCK.apply(RecipeConflictLock.Detection.CONFLICT, () -> true, value -> {});
        // null server 与 overworld==null 共享同一早退守卫：无法判定 ≠ 无冲突，保持原状态
        CalciteStonecuttingRecipeConflictDetector.recomputeAndNotify(null);
        assertTrue(CalciteStonecuttingRecipeState.isConflictLocked());
        boolean[] restored = {false};
        CalciteStonecuttingRecipeState.LOCK.apply(RecipeConflictLock.Detection.NO_CONFLICT, () -> false, value -> restored[0] = value);
        assertTrue(restored[0]);
    }

    // ------------------------------------------------------------------ 桩与工具

    /** 正常解析到指定物品的 display。 */
    private static RecipeDisplay displayOf(net.minecraft.world.level.ItemLike item) {
        SlotDisplay slot = new SlotDisplay.ItemStackSlotDisplay(new ItemStack(item));
        return new RecipeDisplay() {
            @Override
            public SlotDisplay result() {
                return slot;
            }

            @Override
            public SlotDisplay craftingStation() {
                return null;
            }

            @Override
            public RecipeDisplay.Type<? extends RecipeDisplay> type() {
                return null;
            }
        };
    }

    /** result() 抛异常的 display（模拟已加载配方在运行时的产物解析异常）。 */
    private static RecipeDisplay throwingDisplay() {
        return new RecipeDisplay() {
            @Override
            public SlotDisplay result() {
                throw new IllegalStateException("unresolvable result display");
            }

            @Override
            public SlotDisplay craftingStation() {
                return null;
            }

            @Override
            public RecipeDisplay.Type<? extends RecipeDisplay> type() {
                return null;
            }
        };
    }

    /** 只承载「类型 + display 列表」的最小配方桩；检测器仅触碰 getType()/display()。 */
    private static final class StubRecipe implements Recipe<RecipeInput> {
        private final RecipeType<?> type;
        private final List<RecipeDisplay> displays;

        StubRecipe(RecipeType<?> type, List<RecipeDisplay> displays) {
            this.type = type;
            this.displays = List.copyOf(displays);
        }

        @Override
        public boolean matches(RecipeInput input, Level level) {
            return false;
        }

        @Override
        public ItemStack assemble(RecipeInput input, HolderLookup.Provider lookup) {
            return ItemStack.EMPTY;
        }

        @Override
        @SuppressWarnings("unchecked")
        public RecipeType<? extends Recipe<RecipeInput>> getType() {
            return (RecipeType<? extends Recipe<RecipeInput>>) type;
        }

        @Override
        public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
            return null;
        }

        @Override
        public PlacementInfo placementInfo() {
            return PlacementInfo.NOT_PLACEABLE;
        }

        @Override
        public RecipeBookCategory recipeBookCategory() {
            return RecipeBookCategories.CRAFTING_MISC;
        }

        @Override
        public List<RecipeDisplay> display() {
            return displays;
        }
    }
}
