package com.ice2974.carpeticeaddition.rules;

/** Independent runtime lock for CraftableCoralBlocks. */
public final class CraftableCoralBlocksState {
    static final RecipeConflictLock LOCK = new RecipeConflictLock();
    private CraftableCoralBlocksState() {}
    public static boolean isConflictLocked() { return LOCK.isLocked(); }
}
