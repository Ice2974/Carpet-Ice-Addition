package com.ice2974.carpeticeaddition.rules;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.item.crafting.RecipeType;

/** Shared traversal; platform adapters retain recipe extraction and error policy. */
public final class RecipeConflictScanner {
    private RecipeConflictScanner() {}
    public static boolean matchesType(RecipeConflictDefinition definition, RecipeType<?> type) {
        return type == (definition.type() == RecipeConflictDefinition.Type.CRAFTING
                ? RecipeType.CRAFTING : RecipeType.STONECUTTING);
    }
    public static <T> boolean scan(Iterable<T> recipes, RecipeConflictDefinition definition,
            Predicate<T> eligible, Function<T, String> id, Consumer<T> own, Predicate<T> external) {
        for (T recipe : recipes) {
            if (!eligible.test(recipe)) continue;
            if (definition.owns(id.apply(recipe))) own.accept(recipe);
            else if (external.test(recipe)) return true;
        }
        return false;
    }
}
