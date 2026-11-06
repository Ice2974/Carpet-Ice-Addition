package com.ice2974.carpeticeaddition.rules;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;

public final class CrafterOutputBlockHelper {
    private CrafterOutputBlockHelper() {
    }

    public static boolean canFullyInsertAll(Container inventory, List<ItemStack> stacks, Direction fromSide) {
        int[] slots = getAvailableSlots(inventory, fromSide);
        if (slots.length == 0) {
            return false;
        }

        Container snapshot = createSnapshot(inventory);

        for (ItemStack stack : stacks) {
            if (!tryInsertWholeStack(inventory, snapshot, slots, fromSide, stack)) {
                return false;
            }
        }
        return true;
    }

    private static Container createSnapshot(Container inventory) {
        // A vanilla copy cannot preserve subclass overrides or their additional state.
        // Keep custom crafters on the original admission path instead of bypassing it.
        if (inventory.getClass() == CrafterBlockEntity.class) {
            CrafterBlockEntity crafter = (CrafterBlockEntity) inventory;
            // Vanilla crafter admission depends on the other slots after each insertion.
            // Keep this copy detached from the world; never temporarily edit the target.
            CrafterBlockEntity copy = new CrafterBlockEntity(crafter.getBlockPos(), crafter.getBlockState());
            for (int slot = 0; slot < crafter.getContainerSize(); slot++) {
                copy.setItem(slot, crafter.getItem(slot).copy());
                copy.setSlotState(slot, !crafter.isSlotDisabled(slot));
            }
            return copy;
        }
        ItemStack[] stacks = new ItemStack[inventory.getContainerSize()];
        for (int slot = 0; slot < stacks.length; slot++) {
            stacks[slot] = inventory.getItem(slot).copy();
        }
        return new SimpleContainer(stacks);
    }

    private static boolean tryInsertWholeStack(Container inventory, Container snapshot, int[] slots, Direction fromSide, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        ItemStack single = stack.copyWithCount(1);
        int left = stack.getCount();
        while (left > 0) {
            if (!tryInsertOne(inventory, snapshot, slots, fromSide, single)) {
                return false;
            }
            left--;
        }

        return true;
    }

    private static boolean tryInsertOne(Container inventory, Container snapshot, int[] slots, Direction fromSide, ItemStack single) {
        for (int slot : slots) {
            if (!canInsert(inventory, snapshot, single, slot, fromSide)) {
                continue;
            }

            ItemStack slotStack = snapshot.getItem(slot);
            if (slotStack.isEmpty()) {
                if (inventory.getMaxStackSize(single) <= 0) {
                    continue;
                }
                snapshot.setItem(slot, single.copy());
                return true;
            }

            if (!ItemStack.isSameItemSameComponents(slotStack, single)) {
                continue;
            }

            int maxCount = Math.min(slotStack.getMaxStackSize(), inventory.getMaxStackSize(slotStack));
            if (slotStack.getCount() < maxCount) {
                slotStack.grow(1);
                return true;
            }
        }

        return false;
    }

    private static boolean canInsert(Container inventory, Container snapshot, ItemStack stack, int slot, Direction fromSide) {
        Container admission = snapshot instanceof CrafterBlockEntity ? snapshot : inventory;
        return admission.canPlaceItem(slot, stack)
                && (!(inventory instanceof WorldlyContainer sided) || sided.canPlaceItemThroughFace(slot, stack, fromSide));
    }

    private static int[] getAvailableSlots(Container inventory, Direction fromSide) {
        if (inventory instanceof WorldlyContainer sided) {
            return sided.getSlotsForFace(fromSide);
        }

        int[] slots = new int[inventory.getContainerSize()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }
}
