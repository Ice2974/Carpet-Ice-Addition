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

    @Test
    void jobSitePoiVariantNeverMatchesHomeOrMeetingCallSites() {
        // HOME / MEETING_POINT 变体经由最宽重载委托时，两个 MemoryModuleType 参数是同一实例
        Object homeModule = new Object();
        assertFalse(IronGolemVillagerOptimizer.isJobSitePoiVariant(homeModule, homeModule));
        Object meetingModule = new Object();
        assertFalse(IronGolemVillagerOptimizer.isJobSitePoiVariant(meetingModule, meetingModule));
    }

    @Test
    void meetingPoiVariantMatchesOnlyMeetingPointModule() {
        // MEETING_POINT 变体经由最宽重载委托时，目标 memory 与 MEETING_POINT 常量为同一实例；
        // HOME 变体的目标 memory 是 HOME 实例，不会命中
        Object meetingPointModule = new Object();
        assertTrue(IronGolemVillagerOptimizer.isMeetingPoiVariant(meetingPointModule, meetingPointModule));
        Object homeModule = new Object();
        assertFalse(IronGolemVillagerOptimizer.isMeetingPoiVariant(homeModule, meetingPointModule));
    }

    @Test
    void jobSitePoiVariantMatchesDistinctMemoryModules() {
        // JOB_SITE 变体传入 JOB_SITE 与 POTENTIAL_JOB_SITE 两个不同实例
        Object jobSiteModule = new Object();
        Object potentialJobSiteModule = new Object();
        assertTrue(IronGolemVillagerOptimizer.isJobSitePoiVariant(jobSiteModule, potentialJobSiteModule));
    }
}
