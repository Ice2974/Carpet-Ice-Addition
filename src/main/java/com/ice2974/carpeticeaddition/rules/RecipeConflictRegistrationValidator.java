package com.ice2974.carpeticeaddition.rules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Pure preflight, with no Fabric or server dependencies. */
public final class RecipeConflictRegistrationValidator {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    public record Registration(String rule, String pack, RecipeConflictDefinition conflict) {}
    private RecipeConflictRegistrationValidator() {}

    public static void validate(List<Registration> registrations) {
        if (registrations == null || registrations.isEmpty()) throw new IllegalArgumentException("No recipe registrations");
        Map<String, String> rules = new HashMap<>(), packs = new HashMap<>(), recipes = new HashMap<>(), targets = new HashMap<>();
        for (Registration r : registrations) {
            if (r == null || r.rule() == null || r.rule().isBlank() || !r.rule().equals(r.rule().trim()))
                throw new IllegalArgumentException("Invalid recipe rule: " + r);
            unique(rules, r.rule(), r.rule(), "rule");
            requireId(r.pack(), r.rule(), "pack");
            unique(packs, r.pack(), r.rule(), "pack");
            RecipeConflictDefinition d = r.conflict();
            if (d == null || d.type() == null || d.recipeIds() == null || d.recipeIds().isEmpty()
                    || d.resultIds() == null || d.resultIds().isEmpty())
                throw new IllegalArgumentException("Invalid conflict definition for " + r.rule());
            for (String id : d.recipeIds()) { requireId(id, r.rule(), "recipe"); unique(recipes, id, r.rule(), "recipe"); }
            for (String id : d.resultIds()) { requireId(id, r.rule(), "result"); unique(targets, d.type() + " " + id, r.rule(), "result"); }
        }
    }
    private static void requireId(String id, String rule, String kind) {
        if (id == null || !ID.matcher(id).matches()) throw new IllegalArgumentException("Invalid " + kind + " id for " + rule + ": " + id);
    }
    private static void unique(Map<String, String> seen, String value, String rule, String kind) {
        String previous = seen.putIfAbsent(value, rule);
        if (previous != null) throw new IllegalArgumentException("Duplicate " + kind + " " + value
                + " in rules " + previous + " and " + rule);
    }
}
