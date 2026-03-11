package com.ice2974.carpeticeaddition.rules;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import java.util.List;

public final class CrafterOutputBlockHelper {
    private CrafterOutputBlockHelper() {
    }

    public static boolean canFullyInsertAll(Inventory inventory, List<ItemStack> stacks, Direction fromSide) {
        int[] slots = getAvailableSlots(inventory, fromSide);
        if (slots.length == 0) {
            return false;
        }

        ItemStack[] snapshot = new ItemStack[inventory.size()];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = inventory.getStack(i).copy();
        }

        for (ItemStack stack : stacks) {
            if (!tryInsertWholeStack(inventory, snapshot, slots, fromSide, stack)) {
                return false;
            }
        }
        return true;
    }

    private static boolean tryInsertWholeStack(Inventory inventory, ItemStack[] snapshot, int[] slots, Direction fromSide, ItemStack stack) {
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

    private static boolean tryInsertOne(Inventory inventory, ItemStack[] snapshot, int[] slots, Direction fromSide, ItemStack single) {
        for (int slot : slots) {
            if (!canInsert(inventory, single, slot, fromSide)) {
                continue;
            }

            ItemStack slotStack = snapshot[slot];
            if (slotStack.isEmpty()) {
                if (inventory.getMaxCount(single) <= 0) {
                    continue;
                }
                snapshot[slot] = single.copy();
                return true;
            }

            if (!ItemStack.areItemsAndComponentsEqual(slotStack, single)) {
                continue;
            }

            int maxCount = Math.min(slotStack.getMaxCount(), inventory.getMaxCount(slotStack));
            if (slotStack.getCount() < maxCount) {
                slotStack.increment(1);
                return true;
            }
        }

        return false;
    }

    private static boolean canInsert(Inventory inventory, ItemStack stack, int slot, Direction fromSide) {
        return inventory.isValid(slot, stack)
                && (!(inventory instanceof SidedInventory sided) || sided.canInsert(slot, stack, fromSide));
    }

    private static int[] getAvailableSlots(Inventory inventory, Direction fromSide) {
        if (inventory instanceof SidedInventory sided) {
            return sided.getAvailableSlots(fromSide);
        }

        int[] slots = new int[inventory.size()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = i;
        }
        return slots;
    }
}
