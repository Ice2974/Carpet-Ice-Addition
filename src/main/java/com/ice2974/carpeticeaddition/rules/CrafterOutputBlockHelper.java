package com.ice2974.carpeticeaddition.rules;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;

public final class CrafterOutputBlockHelper {
    private CrafterOutputBlockHelper() {
    }

    public static boolean canFullyInsertAll(Container inventory, List<ItemStack> stacks, Direction fromSide) {
        int[] slots = getAvailableSlots(inventory, fromSide);
        if (slots.length == 0) {
            return false;
        }

        ItemStack[] snapshot = new ItemStack[inventory.getContainerSize()];
        for (int i = 0; i < snapshot.length; i++) {
            snapshot[i] = inventory.getItem(i).copy();
        }

        for (ItemStack stack : stacks) {
            if (!tryInsertWholeStack(inventory, snapshot, slots, fromSide, stack)) {
                return false;
            }
        }
        return true;
    }

    private static boolean tryInsertWholeStack(Container inventory, ItemStack[] snapshot, int[] slots, Direction fromSide, ItemStack stack) {
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

    private static boolean tryInsertOne(Container inventory, ItemStack[] snapshot, int[] slots, Direction fromSide, ItemStack single) {
        for (int slot : slots) {
            if (!canInsert(inventory, single, slot, fromSide)) {
                continue;
            }

            ItemStack slotStack = snapshot[slot];
            if (slotStack.isEmpty()) {
                if (inventory.getMaxStackSize(single) <= 0) {
                    continue;
                }
                snapshot[slot] = single.copy();
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

    private static boolean canInsert(Container inventory, ItemStack stack, int slot, Direction fromSide) {
        return inventory.canPlaceItem(slot, stack)
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
