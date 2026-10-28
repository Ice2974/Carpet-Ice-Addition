package com.ice2974.carpeticeaddition.rules;

import java.util.List;
import java.util.Set;

/** Rule-owned conflict inputs. Lists retain duplicates until registration validation. */
public record RecipeConflictDefinition(Type type, List<String> recipeIds, List<String> resultIds) {
    public enum Type { CRAFTING, STONECUTTING }

    public RecipeConflictDefinition {
        recipeIds = recipeIds == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(recipeIds));
        resultIds = resultIds == null ? null : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(resultIds));
    }

    public boolean owns(String id) { return recipeIds.contains(id); }
    public Set<String> targets() { return Set.copyOf(resultIds); }
}
