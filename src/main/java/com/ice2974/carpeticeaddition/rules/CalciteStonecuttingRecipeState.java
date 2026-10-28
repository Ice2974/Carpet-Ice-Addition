package com.ice2974.carpeticeaddition.rules;

/** Independent runtime lock for CalciteStonecuttingRecipe. */
public final class CalciteStonecuttingRecipeState {
    static final RecipeConflictLock LOCK = new RecipeConflictLock();
    private CalciteStonecuttingRecipeState() {}
    public static boolean isConflictLocked() { return LOCK.isLocked(); }
}
