package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IronGolemVillagerOptimizerTest {

    @Test
    void acceptsExactLowercaseName() {
        assertTrue(IronGolemVillagerOptimizer.matchesOptimizedVillagerName("iron_golem"));
    }

    @Test
    void rejectsNullName() {
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName(null));
    }

    @Test
    void rejectsCaseVariants() {
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName("Iron_Golem"));
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName("IRON_GOLEM"));
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName("iron_goleM"));
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName("irongolem"));
    }

    @Test
    void rejectsSurroundingWhitespaceOrExtraCharacters() {
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName("iron_golem "));
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName(" iron_golem"));
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName("iron golem"));
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName("iron_golem."));
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName(""));
    }

    @Test
    void rejectsOtherNames() {
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName("trade"));
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName("snow_golem"));
        assertFalse(IronGolemVillagerOptimizer.matchesOptimizedVillagerName("Villager"));
    }
}
