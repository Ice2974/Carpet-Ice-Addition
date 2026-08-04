package com.ice2974.carpeticeaddition.rules;

/**
 * Pure-Java parsing and calculation helpers for the {@code waterFluidTickDelay}
 * and {@code lavaFluidTickDelay} Carpet rules.
 *
 * <p>This class lives in the common module so it can be unit-tested without any
 * Minecraft dependency. The actual rule fields and their {@code Validator}
 * (which needs MC types) live in the per-mapping shared roots.
 */
public final class FluidTickDelayUtil {
    /** Sentinel value indicating that a fluid is frozen. */
    public static final String FREEZE = "freeze";

    /** Default delay for water, matching vanilla. */
    public static final int DEFAULT_WATER_DELAY = 5;

    /** Default delay for lava in non-ultrawarm dimensions, matching vanilla. */
    public static final int DEFAULT_LAVA_DELAY = 30;

    private FluidTickDelayUtil() {
    }

    /**
     * @return {@code true} if the rule value represents the freeze sentinel.
     */
    public static boolean isFrozen(String value) {
        return FREEZE.equals(value);
    }

    /**
     * Parses a rule value into a positive integer delay.
     *
     * @return the parsed delay, or {@code null} if the value is not a valid
     *         positive integer (zero, negative, decimal, non-numeric, empty,
     *         or outside {@code int} range are all rejected).
     */
    public static Integer parsePositiveDelayOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            // Use long to catch out-of-range inputs before narrowing.
            long parsed = Long.parseLong(value);
            if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
                return null;
            }
            return (int) parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * @return {@code true} if the value is a valid rule value ({@code freeze}
     *         or a positive integer).
     */
    public static boolean isValidRuleValue(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (isFrozen(value)) {
            return true;
        }
        return parsePositiveDelayOrNull(value) != null;
    }

    /**
     * Computes the effective lava flow delay for a dimension.
     *
     * @param configuredDelay the configured delay in game ticks
     * @param ultrawarm       whether the current dimension is ultrawarm
     *                        (or fast-lava in 26.x), where lava flows faster
     * @return the effective delay; in ultrawarm dimensions the configured
     *         delay is divided by 3 with a minimum of 1
     */
    public static int getLavaDelay(int configuredDelay, boolean ultrawarm) {
        if (ultrawarm) {
            return Math.max(1, configuredDelay / 3);
        }
        return configuredDelay;
    }
}
