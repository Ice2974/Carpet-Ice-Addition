package com.ice2974.carpeticeaddition.rules;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrafterOutputBlockHelperTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void twoUnevenCrafterSlotsAcceptFourItemsLikeVanilla() {
        CrafterBlockEntity target = crafter(62, 61);
        assertPredictionMatchesVanilla(target, List.of(planks(4)), Direction.NORTH, true);
        assertEquals(64, target.getItem(0).getCount());
        assertEquals(63, target.getItem(1).getCount());
        for (int slot = 2; slot < 9; slot++) {
            assertTrue(target.isSlotDisabled(slot));
            assertTrue(target.getItem(slot).isEmpty());
        }
    }

    @Test
    void nineUnevenCrafterSlotsAcceptExactlyTheirRemainingCapacity() {
        assertPredictionMatchesVanilla(crafter(63, 63, 63, 63, 63, 63, 63, 63, 62),
                List.of(planks(10)), Direction.UP, true);
        assertPredictionMatchesVanilla(crafter(63, 63, 63, 63, 63, 63, 63, 63, 62),
                List.of(planks(11)), Direction.UP, false);
    }

    @Test
    void emptySlotsAndDisabledSlotsRetainVanillaAdmission() {
        assertPredictionMatchesVanilla(crafter(0, 0), List.of(planks(64), planks(64)), Direction.SOUTH, true);
        assertPredictionMatchesVanilla(crafter(0, 0), List.of(planks(64), planks(64), planks(1)), Direction.SOUTH, false);
        assertPredictionMatchesVanilla(crafter(), List.of(planks(1)), Direction.SOUTH, false);
    }

    @Test
    void successiveOutputStacksShareTheUpdatedCrafterSnapshot() {
        assertPredictionMatchesVanilla(crafter(62, 61), List.of(planks(2), planks(3)), Direction.WEST, true);
        assertPredictionMatchesVanilla(crafter(62, 61), List.of(planks(2), planks(4)), Direction.WEST, false);
    }

    @Test
    void itemComponentsAndItemStackLimitsStillApply() {
        CrafterBlockEntity target = crafter(63, 63);
        target.getItem(0).set(DataComponents.CUSTOM_NAME, Component.literal("reserved"));
        assertPredictionMatchesVanilla(target, List.of(planks(2)), Direction.NORTH, false);

        CrafterBlockEntity buckets = crafter(0, 0);
        buckets.setItem(0, new ItemStack(Items.BUCKET, 15));
        buckets.setItem(1, new ItemStack(Items.BUCKET, 14));
        assertPredictionMatchesVanilla(buckets, List.of(new ItemStack(Items.BUCKET, 3)), Direction.NORTH, true);
        assertEquals(16, buckets.getItem(0).getCount());
        assertEquals(16, buckets.getItem(1).getCount());
    }

    @Test
    void predictionNeverCallsTargetMutationMethodsEvenWhenInsertionFails() {
        GuardedCrafter target = new GuardedCrafter();
        target.setItem(0, planks(62));
        target.setItem(1, planks(61));
        for (int slot = 2; slot < 9; slot++) {
            target.setSlotState(slot, false);
        }
        target.readOnly = true;
        assertTrue(CrafterOutputBlockHelper.canFullyInsertAll(target, List.of(planks(2)), Direction.NORTH));
        assertFalse(CrafterOutputBlockHelper.canFullyInsertAll(target, List.of(planks(6)), Direction.NORTH));
        assertEquals(62, target.getItem(0).getCount());
        assertEquals(61, target.getItem(1).getCount());
    }

    @Test
    void customCrafterKeepsItsAdditionalAdmissionRestrictions() {
        CrafterBlockEntity target = new CrafterBlockEntity(BlockPos.ZERO, Blocks.CRAFTER.defaultBlockState()) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return slot == 0 && stack.is(Items.STONE) && super.canPlaceItem(slot, stack);
            }
        };
        assertPredictionMatchesVanilla(target, List.of(planks(1)), Direction.NORTH, false);
        assertPredictionMatchesVanilla(target, List.of(new ItemStack(Items.STONE)), Direction.NORTH, true);
        assertEquals(Items.STONE, target.getItem(0).getItem());
        assertPredictionMatchesVanilla(target, List.of(new ItemStack(Items.STONE)), Direction.NORTH, false);
    }

    @Test
    void ordinaryContainersKeepTheirSlotRestrictionsAndStackLimits() {
        SimpleContainer restricted = new SimpleContainer(2) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return slot == 1 && stack.is(Items.OAK_PLANKS);
            }

            @Override
            public int getMaxStackSize() {
                return 2;
            }
        };
        assertFalse(CrafterOutputBlockHelper.canFullyInsertAll(restricted,
                List.of(new ItemStack(Items.STONE)), Direction.UP));
        assertFalse(CrafterOutputBlockHelper.canFullyInsertAll(restricted, List.of(planks(3)), Direction.UP));
        assertPredictionMatchesVanilla(restricted, List.of(planks(2)), Direction.UP, true);
        assertTrue(restricted.getItem(0).isEmpty());
        assertEquals(2, restricted.getItem(1).getCount());
    }

    @Test
    void brewingStandStillRestrictsAccessibleSlotsByFace() {
        BrewingStandBlockEntity target = new BrewingStandBlockEntity(BlockPos.ZERO, Blocks.BREWING_STAND.defaultBlockState());
        assertPredictionMatchesVanilla(target, List.of(new ItemStack(Items.GLASS_BOTTLE)), Direction.UP, false);
        assertPredictionMatchesVanilla(target, List.of(new ItemStack(Items.GLASS_BOTTLE)), Direction.NORTH, true);
        assertEquals(Items.GLASS_BOTTLE, target.getItem(0).getItem());
        assertTrue(target.getItem(3).isEmpty());
    }

    @Test
    void shulkerBoxStillRejectsNestedShulkerBoxesThroughItsFacePredicate() {
        ShulkerBoxBlockEntity target = new ShulkerBoxBlockEntity(BlockPos.ZERO, Blocks.SHULKER_BOX.defaultBlockState());
        assertPredictionMatchesVanilla(target, List.of(new ItemStack(Items.SHULKER_BOX)), Direction.NORTH, false);
        assertPredictionMatchesVanilla(target, List.of(planks(2)), Direction.NORTH, true);
    }

    private static CrafterBlockEntity crafter(int... counts) {
        CrafterBlockEntity target = new CrafterBlockEntity(BlockPos.ZERO, Blocks.CRAFTER.defaultBlockState());
        for (int slot = 0; slot < target.getContainerSize(); slot++) {
            if (slot < counts.length) {
                target.setItem(slot, planks(counts[slot]));
            } else {
                target.setSlotState(slot, false);
            }
        }
        return target;
    }

    private static ItemStack planks(int count) {
        return new ItemStack(Items.OAK_PLANKS, count);
    }

    private static void assertPredictionMatchesVanilla(Container target, List<ItemStack> outputs, Direction side,
                                                       boolean expected) {
        List<ItemStack> originalStacks = new ArrayList<>();
        List<ItemStack> originalValues = new ArrayList<>();
        List<ItemStack> outputValues = outputs.stream().map(ItemStack::copy).toList();
        for (int slot = 0; slot < target.getContainerSize(); slot++) {
            originalStacks.add(target.getItem(slot));
            originalValues.add(target.getItem(slot).copy());
        }

        assertEquals(expected, CrafterOutputBlockHelper.canFullyInsertAll(target, outputs, side));
        for (int slot = 0; slot < target.getContainerSize(); slot++) {
            assertSame(originalStacks.get(slot), target.getItem(slot), "prediction replaced a target stack");
            assertTrue(ItemStack.matches(originalValues.get(slot), target.getItem(slot)), "prediction changed target contents");
        }
        for (int index = 0; index < outputs.size(); index++) {
            assertTrue(ItemStack.matches(outputValues.get(index), outputs.get(index)), "prediction changed its input");
        }

        // CrafterBlock delivers to crafters one item at a time. Use vanilla's real
        // insertion and admission methods as the oracle, after checking read-only behavior.
        boolean accepted = true;
        for (ItemStack output : outputs) {
            if (target instanceof CrafterBlockEntity || output.getCount() > target.getMaxStackSize(output)) {
                for (int count = 0; count < output.getCount(); count++) {
                    if (!HopperBlockEntity.addItem(null, target, output.copyWithCount(1), side).isEmpty()) {
                        accepted = false;
                        break;
                    }
                }
            } else {
                accepted = HopperBlockEntity.addItem(null, target, output.copy(), side).isEmpty();
            }
            if (!accepted) {
                break;
            }
        }
        assertEquals(expected, accepted, "vanilla insertion disagrees with the prediction");
    }

    private static final class GuardedCrafter extends CrafterBlockEntity {
        private boolean readOnly;

        private GuardedCrafter() {
            super(BlockPos.ZERO, Blocks.CRAFTER.defaultBlockState());
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            assertFalse(readOnly, "prediction must not temporarily write the target");
            super.setItem(slot, stack);
        }

        @Override
        public void setSlotState(int slot, boolean enabled) {
            assertFalse(readOnly, "prediction must not temporarily change disabled slots");
            super.setSlotState(slot, enabled);
        }

        @Override
        public void setChanged() {
            assertFalse(readOnly, "prediction must not dirty the target");
            super.setChanged();
        }
    }
}
