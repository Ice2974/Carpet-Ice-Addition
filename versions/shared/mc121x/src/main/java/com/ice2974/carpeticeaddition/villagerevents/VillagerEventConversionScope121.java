package com.ice2974.carpeticeaddition.villagerevents;

import java.util.ArrayDeque;
import net.minecraft.entity.Entity;

/**
 * Only used for the 1.21.1 ServerWorldAccess default method, whose void signature hides
 * spawnEntity's boolean result. The stack is scoped to one original call and is always popped.
 */
public final class VillagerEventConversionScope121 {
    private static final ThreadLocal<ArrayDeque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private VillagerEventConversionScope121() { }

    public static void push(VillagerEventState state, Entity expectedRootWitch) { STACK.get().push(new Frame(state, expectedRootWitch)); }
    public static void pop(VillagerEventState expected) {
        ArrayDeque<Frame> stack = STACK.get();
        if (!stack.isEmpty() && stack.peek().sourceState == expected) stack.pop();
        else stack.removeIf(frame -> frame.sourceState == expected);
        if (stack.isEmpty()) STACK.remove();
    }
    public static void recordSpawn(Entity entity, boolean accepted) {
        ArrayDeque<Frame> stack = STACK.get();
        if (stack.isEmpty()) return;
        Frame frame = stack.peek();
        if (entity != frame.expectedRootWitch) return;
        if (accepted) frame.sourceState.carpetIceAddition$recordConversionSpawn(true);
    }
    private static final class Frame {
        private final VillagerEventState sourceState;
        private final Entity expectedRootWitch;
        private Frame(VillagerEventState sourceState, Entity expectedRootWitch) { this.sourceState = sourceState; this.expectedRootWitch = expectedRootWitch; }
    }
}
