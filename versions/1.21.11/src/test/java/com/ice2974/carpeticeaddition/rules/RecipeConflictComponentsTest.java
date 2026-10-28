package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class RecipeConflictComponentsTest {
    @AfterEach void reset() {
        CraftableCoralBlocksState.LOCK.reset();
        CalciteStonecuttingRecipeState.LOCK.reset();
    }

    @Test void productionLocksAreIndependentAndRetainExplicitOff() {
        RecipeConflictLock a = CraftableCoralBlocksState.LOCK, b = CalciteStonecuttingRecipeState.LOCK;
        boolean[] fields = {true, true};
        assertEquals(RecipeConflictLock.Transition.LOCKED, a.apply(RecipeConflictLock.Detection.CONFLICT, () -> fields[0], v -> fields[0] = v));
        assertEquals(RecipeConflictLock.Transition.LOCKED, b.apply(RecipeConflictLock.Detection.CONFLICT, () -> fields[1], v -> fields[1] = v));
        assertFalse(a.validateRequestedValue(true));
        assertTrue(a.validateRequestedValue(false));
        assertEquals(RecipeConflictLock.Transition.UNDETERMINED, b.apply(RecipeConflictLock.Detection.UNDETERMINED, () -> fields[1], v -> fields[1] = v));
        assertEquals(RecipeConflictLock.Transition.UNLOCKED, a.apply(RecipeConflictLock.Detection.NO_CONFLICT, () -> fields[0], v -> fields[0] = v));
        assertFalse(fields[0]);
        assertTrue(b.isLocked());
        assertEquals(RecipeConflictLock.Transition.UNLOCKED, b.apply(RecipeConflictLock.Detection.NO_CONFLICT, () -> fields[1], v -> fields[1] = v));
        assertTrue(fields[1]);
        assertEquals(RecipeConflictLock.Transition.LOCKED, b.apply(RecipeConflictLock.Detection.CONFLICT, () -> fields[1], v -> fields[1] = v));
        assertEquals(RecipeConflictLock.Transition.UNCHANGED, b.apply(RecipeConflictLock.Detection.CONFLICT, () -> fields[1], v -> fields[1] = v));
    }

    @Test void existingRuleValidatorsAndFieldsShareTheProductionLocks() {
        var coral = new CraftableCoralBlocksValidator();
        var calcite = new CalciteStonecuttingRecipeValidator();
        com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings.craftableCoralBlocks = true;
        com.ice2974.carpeticeaddition.settings.CalciteStonecuttingRecipeSettings.calciteStonecuttingRecipe = true;
        CraftableCoralBlocksState.LOCK.apply(RecipeConflictLock.Detection.CONFLICT,
                () -> com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings.craftableCoralBlocks,
                v -> com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings.craftableCoralBlocks = v);
        CalciteStonecuttingRecipeState.LOCK.apply(RecipeConflictLock.Detection.CONFLICT,
                () -> com.ice2974.carpeticeaddition.settings.CalciteStonecuttingRecipeSettings.calciteStonecuttingRecipe,
                v -> com.ice2974.carpeticeaddition.settings.CalciteStonecuttingRecipeSettings.calciteStonecuttingRecipe = v);
        assertNull(coral.validate(null, null, true, "true"));
        assertNull(calcite.validate(null, null, true, "true"));
        assertEquals(false, coral.validate(null, null, false, "false"));
        CraftableCoralBlocksState.LOCK.apply(RecipeConflictLock.Detection.NO_CONFLICT,
                () -> false, v -> com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings.craftableCoralBlocks = v);
        CalciteStonecuttingRecipeState.LOCK.apply(RecipeConflictLock.Detection.NO_CONFLICT,
                () -> false, v -> com.ice2974.carpeticeaddition.settings.CalciteStonecuttingRecipeSettings.calciteStonecuttingRecipe = v);
        assertFalse(com.ice2974.carpeticeaddition.settings.CraftableCoralBlocksSettings.craftableCoralBlocks);
        assertTrue(com.ice2974.carpeticeaddition.settings.CalciteStonecuttingRecipeSettings.calciteStonecuttingRecipe);
        com.ice2974.carpeticeaddition.settings.CalciteStonecuttingRecipeSettings.calciteStonecuttingRecipe = false;
    }

    @Test void writerFailureDoesNotGetSwallowedAndPreservesOriginalOrdering() {
        RecipeConflictLock lock = new RecipeConflictLock();
        assertThrows(IllegalStateException.class, () -> lock.apply(RecipeConflictLock.Detection.CONFLICT, () -> true, v -> { throw new IllegalStateException(); }));
        assertFalse(lock.isLocked());
        lock.apply(RecipeConflictLock.Detection.CONFLICT, () -> true, v -> {});
        assertThrows(IllegalStateException.class, () -> lock.apply(RecipeConflictLock.Detection.NO_CONFLICT, () -> false, v -> { throw new IllegalStateException(); }));
        assertTrue(lock.isLocked());
        boolean[] restored = {false};
        lock.apply(RecipeConflictLock.Detection.NO_CONFLICT, () -> false, v -> restored[0] = v);
        assertTrue(restored[0]);
    }

    private static RecipeConflictRegistrationValidator.Registration registration(String rule, String pack, RecipeConflictDefinition.Type type, List<String> ids, List<String> results) {
        return new RecipeConflictRegistrationValidator.Registration(rule, pack, new RecipeConflictDefinition(type, ids, results));
    }
    @Test void registrationRejectsInvalidAndOverlappingInputs() {
        var a = registration("a", "test:a", RecipeConflictDefinition.Type.CRAFTING, List.of("test:a"), List.of("minecraft:stone"));
        var otherType = registration("b", "test:b", RecipeConflictDefinition.Type.STONECUTTING, List.of("test:b"), List.of("minecraft:stone"));
        assertDoesNotThrow(() -> RecipeConflictRegistrationValidator.validate(List.of(a, otherType)));
        for (var bad : List.of(
                registration("a", "test:b", RecipeConflictDefinition.Type.STONECUTTING, List.of("test:b"), List.of("minecraft:dirt")),
                registration("b", "test:a", RecipeConflictDefinition.Type.STONECUTTING, List.of("test:b"), List.of("minecraft:dirt")),
                registration("b", "test:b", RecipeConflictDefinition.Type.STONECUTTING, List.of("test:a"), List.of("minecraft:dirt")),
                registration("b", "test:b", RecipeConflictDefinition.Type.CRAFTING, List.of("test:b"), List.of("minecraft:stone")),
                registration("b", "test:b", RecipeConflictDefinition.Type.STONECUTTING, List.of("test:b", "test:b"), List.of("minecraft:dirt")),
                registration("b", "test:b", RecipeConflictDefinition.Type.STONECUTTING, List.of(), List.of("minecraft:dirt")),
                registration("b", "test:b", RecipeConflictDefinition.Type.STONECUTTING, List.of("bad"), List.of("minecraft:dirt")))) {
            assertThrows(IllegalArgumentException.class, () -> RecipeConflictRegistrationValidator.validate(List.of(a, bad)));
        }
        assertThrows(IllegalArgumentException.class, () -> RecipeConflictRegistrationValidator.validate(null));
        assertThrows(IllegalArgumentException.class, () -> RecipeConflictRegistrationValidator.validate(List.of()));
        var withNull = new ArrayList<String>(); withNull.add(null);
        assertThrows(IllegalArgumentException.class, () -> RecipeConflictRegistrationValidator.validate(List.of(
                registration("b", "test:b", RecipeConflictDefinition.Type.STONECUTTING, withNull, List.of("minecraft:dirt")))));
    }

    @Test void runtimeRecipeIdsMatchBuildVerificationLists() throws Exception {
        java.nio.file.Path root = java.nio.file.Path.of("").toAbsolutePath();
        while (root != null && !java.nio.file.Files.isRegularFile(root.resolve("settings.json"))) root = root.getParent();
        assertNotNull(root, "repository root");
        String build = java.nio.file.Files.readString(root.resolve("build.gradle"));
        assertEquals(CraftableCoralBlocksRecipes.RECIPE_PATHS, recipePaths(build, "craftableCoralRecipePaths"));
        assertEquals(CalciteStonecuttingRecipeData.RECIPE_PATHS, recipePaths(build, "calciteStonecuttingRecipePaths"));
    }
    private static List<String> recipePaths(String build, String variable) {
        var match = java.util.regex.Pattern.compile("(?s)def " + variable + " = \\[(.*?)\\]").matcher(build);
        assertTrue(match.find(), variable);
        var values = new ArrayList<String>();
        var item = java.util.regex.Pattern.compile("'([^']+)'").matcher(match.group(1));
        while (item.find()) values.add(item.group(1));
        return values;
    }

    @Test void scannerUsesDefinitionForIndependentRules() {
        var a = new RecipeConflictDefinition(RecipeConflictDefinition.Type.CRAFTING, List.of("test:own"), List.of("minecraft:stone"));
        var seen = new ArrayList<String>();
        assertTrue(RecipeConflictScanner.scan(List.of("test:own", "test:external"), a, x -> true, x -> x,
                seen::add, x -> x.equals("test:external") && a.targets().contains("minecraft:stone")));
        assertEquals(List.of("test:own"), seen);
    }
}
