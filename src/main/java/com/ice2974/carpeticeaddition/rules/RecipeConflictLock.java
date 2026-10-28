package com.ice2974.carpeticeaddition.rules;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Per-rule runtime state. Mutations occur on the server thread; reads may occur elsewhere. */
public final class RecipeConflictLock {
    public enum Detection { CONFLICT, NO_CONFLICT, UNDETERMINED }
    public enum Transition { LOCKED, UNLOCKED, UNCHANGED, UNDETERMINED }

    private volatile boolean locked;
    private volatile Boolean desired;

    public boolean isLocked() { return locked; }

    /** The reader and writer access the Carpet field directly, never SettingsManager. */
    public Transition apply(Detection result, BooleanSupplier read, Consumer<Boolean> write) {
        if (result == Detection.UNDETERMINED) return Transition.UNDETERMINED;
        if (result == Detection.CONFLICT) {
            boolean wasLocked = locked;
            if (!wasLocked) desired = read.getAsBoolean();
            write.accept(false);
            locked = true;
            return wasLocked ? Transition.UNCHANGED : Transition.LOCKED;
        }
        if (locked) {
            if (desired != null) write.accept(desired);
            desired = null;
            locked = false;
            return Transition.UNLOCKED;
        }
        return Transition.UNCHANGED;
    }

    /** Called by the Carpet validator even when the field already equals false. */
    public boolean validateRequestedValue(boolean value) {
        if (!locked) return true;
        if (value) return false;
        desired = false;
        return true;
    }

    public void reset() { locked = false; desired = null; }
}
