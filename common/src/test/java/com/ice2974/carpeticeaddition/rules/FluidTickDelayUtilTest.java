package com.ice2974.carpeticeaddition.rules;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FluidTickDelayUtilTest {

    // ---- isValidRuleValue ----

    @Test
    void acceptsFreeze() {
        assertTrue(FluidTickDelayUtil.isValidRuleValue("freeze"));
    }

    @Test
    void acceptsPositiveIntegers() {
        assertTrue(FluidTickDelayUtil.isValidRuleValue("1"));
        assertTrue(FluidTickDelayUtil.isValidRuleValue("2"));
        assertTrue(FluidTickDelayUtil.isValidRuleValue("3"));
        assertTrue(FluidTickDelayUtil.isValidRuleValue("5"));
        assertTrue(FluidTickDelayUtil.isValidRuleValue("6"));
        assertTrue(FluidTickDelayUtil.isValidRuleValue("10"));
        assertTrue(FluidTickDelayUtil.isValidRuleValue("30"));
        assertTrue(FluidTickDelayUtil.isValidRuleValue("60"));
    }

    @Test
    void acceptsIntegerMaxValue() {
        assertTrue(FluidTickDelayUtil.isValidRuleValue("2147483647"));
    }

    @Test
    void rejectsZero() {
        assertFalse(FluidTickDelayUtil.isValidRuleValue("0"));
    }

    @Test
    void rejectsNegative() {
        assertFalse(FluidTickDelayUtil.isValidRuleValue("-1"));
    }

    @Test
    void rejectsDecimal() {
        assertFalse(FluidTickDelayUtil.isValidRuleValue("1.5"));
    }

    @Test
    void rejectsNonNumeric() {
        assertFalse(FluidTickDelayUtil.isValidRuleValue("abc"));
    }

    @Test
    void rejectsEmpty() {
        assertFalse(FluidTickDelayUtil.isValidRuleValue(""));
    }

    @Test
    void rejectsNull() {
        assertFalse(FluidTickDelayUtil.isValidRuleValue(null));
    }

    @Test
    void rejectsValueExceedingIntegerMax() {
        assertFalse(FluidTickDelayUtil.isValidRuleValue("2147483648"));
    }

    @Test
    void rejectsValueWithTrailingSpace() {
        assertFalse(FluidTickDelayUtil.isValidRuleValue("5 "));
    }

    // ---- isFrozen ----

    @Test
    void isFrozenTrueForFreeze() {
        assertTrue(FluidTickDelayUtil.isFrozen("freeze"));
    }

    @Test
    void isFrozenFalseForNumbers() {
        assertFalse(FluidTickDelayUtil.isFrozen("5"));
        assertFalse(FluidTickDelayUtil.isFrozen("1"));
    }

    // ---- parsePositiveDelayOrNull ----

    @Test
    void parsesValidDelays() {
        assertEquals(Integer.valueOf(1), FluidTickDelayUtil.parsePositiveDelayOrNull("1"));
        assertEquals(Integer.valueOf(5), FluidTickDelayUtil.parsePositiveDelayOrNull("5"));
        assertEquals(Integer.valueOf(30), FluidTickDelayUtil.parsePositiveDelayOrNull("30"));
        assertEquals(Integer.valueOf(2147483647), FluidTickDelayUtil.parsePositiveDelayOrNull("2147483647"));
    }

    @Test
    void parseReturnsNullForInvalid() {
        assertNull(FluidTickDelayUtil.parsePositiveDelayOrNull("0"));
        assertNull(FluidTickDelayUtil.parsePositiveDelayOrNull("-1"));
        assertNull(FluidTickDelayUtil.parsePositiveDelayOrNull("1.5"));
        assertNull(FluidTickDelayUtil.parsePositiveDelayOrNull("abc"));
        assertNull(FluidTickDelayUtil.parsePositiveDelayOrNull(""));
        assertNull(FluidTickDelayUtil.parsePositiveDelayOrNull("2147483648"));
        assertNull(FluidTickDelayUtil.parsePositiveDelayOrNull("freeze"));
        assertNull(FluidTickDelayUtil.parsePositiveDelayOrNull(null));
    }

    // ---- getLavaDelay ----

    @Test
    void lavaDelayNonUltrawarmReturnsConfigured() {
        assertEquals(1, FluidTickDelayUtil.getLavaDelay(1, false));
        assertEquals(6, FluidTickDelayUtil.getLavaDelay(6, false));
        assertEquals(30, FluidTickDelayUtil.getLavaDelay(30, false));
        assertEquals(60, FluidTickDelayUtil.getLavaDelay(60, false));
    }

    @Test
    void lavaDelayUltrawarmDividesByThreeWithMinimumOne() {
        assertEquals(1, FluidTickDelayUtil.getLavaDelay(1, true));
        assertEquals(1, FluidTickDelayUtil.getLavaDelay(2, true));
        assertEquals(1, FluidTickDelayUtil.getLavaDelay(3, true));
        assertEquals(1, FluidTickDelayUtil.getLavaDelay(4, true));
        assertEquals(1, FluidTickDelayUtil.getLavaDelay(5, true));
        assertEquals(2, FluidTickDelayUtil.getLavaDelay(6, true));
        assertEquals(10, FluidTickDelayUtil.getLavaDelay(30, true));
        assertEquals(20, FluidTickDelayUtil.getLavaDelay(60, true));
    }
}
