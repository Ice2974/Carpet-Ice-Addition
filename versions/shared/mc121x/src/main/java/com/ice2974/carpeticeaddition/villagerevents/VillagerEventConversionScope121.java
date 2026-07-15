package com.ice2974.carpeticeaddition.villagerevents;

import java.util.ArrayDeque;

/**
 * Only used for the 1.21.1 ServerWorldAccess default method, whose void signature hides
 * spawnEntity's boolean result. The stack is scoped to one original call and is always popped.
 */
public final class VillagerEventConversionScope121 {
    private static final ThreadLocal<ArrayDeque<VillagerEventState>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private VillagerEventConversionScope121() { }

    public static void push(VillagerEventState state) { STACK.get().push(state); }
    public static void pop(VillagerEventState expected) {
        ArrayDeque<VillagerEventState> stack = STACK.get();
        if (!stack.isEmpty() && stack.peek() == expected) stack.pop();
        else stack.removeFirstOccurrence(expected);
        if (stack.isEmpty()) STACK.remove();
    }
    public static void recordSpawn(boolean accepted) {
        ArrayDeque<VillagerEventState> stack = STACK.get();
        if (accepted && !stack.isEmpty()) stack.peek().carpetIceAddition$recordConversionSpawn(true);
    }
}
