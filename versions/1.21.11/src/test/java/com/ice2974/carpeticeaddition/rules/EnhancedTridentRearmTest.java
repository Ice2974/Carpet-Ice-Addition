package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnhancedTridentRearmTest {

    // ---- EnhancedTridentRearmEpoch（规则代际）----

    @Test
    void generationIsStableWithoutAdvance() {
        int captured = EnhancedTridentRearmEpoch.current();
        assertTrue(EnhancedTridentRearmEpoch.current() == captured);
    }

    @Test
    void advanceInvalidatesPreviouslyCapturedGeneration() {
        int captured = EnhancedTridentRearmEpoch.current();
        EnhancedTridentRearmEpoch.advance();
        assertFalse(EnhancedTridentRearmEpoch.current() == captured);
    }

    @Test
    void advanceIsStrictlyMonotonic() {
        int first = EnhancedTridentRearmEpoch.current();
        EnhancedTridentRearmEpoch.advance();
        int second = EnhancedTridentRearmEpoch.current();
        EnhancedTridentRearmEpoch.advance();
        int third = EnhancedTridentRearmEpoch.current();
        assertTrue(second > first);
        assertTrue(third > second);
    }

    // ---- isFlightRoundPermitted（R1 门控 truth-table）----

    @Test
    void freshTridentAlwaysPermitted() {
        assertTrue(EnhancedTridentHelper.isFlightRoundPermitted(false, false));
        assertTrue(EnhancedTridentHelper.isFlightRoundPermitted(false, true));
    }

    @Test
    void dealtDamageUsesActiveRearm() {
        assertTrue(EnhancedTridentHelper.isFlightRoundPermitted(true, true));
    }

    @Test
    void consumedOrExpiredRearmBlocksRound() {
        // 资格已消费（建轮后）与跨规则开关周期（代际失效）都不得再建轮
        assertFalse(EnhancedTridentHelper.isFlightRoundPermitted(true, false));
    }
}
