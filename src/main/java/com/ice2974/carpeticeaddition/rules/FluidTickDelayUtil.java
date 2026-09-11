package com.ice2974.carpeticeaddition.rules;

import java.util.Objects;

/**
 * Pure-Java parsing and calculation helpers for the {@code waterFluidTickDelay}
 * and {@code lavaFluidTickDelay} Carpet rules.
 *
 * <p>This class is intentionally free of Minecraft types so it can be unit-tested
 * by the core-platform test suite ({@code :1.21.11:test}). The actual rule fields
 * and their {@code Validator} (which needs MC types) live in
 * {@code CarpetIceAdditionFluidSettings}.
 */
public final class FluidTickDelayUtil {
    /** Sentinel value indicating that a fluid is frozen. */
    public static final String FREEZE = "freeze";

    /**
     * Sentinel value indicating that vanilla's own tick-delay calculation must
     * be used unmodified, letting vanilla (and other mods hooking the same
     * method) decide the delay. This is the rule default.
     */
    public static final String VANILLA = "vanilla";

    /** Default delay for water, matching vanilla. */
    public static final int DEFAULT_WATER_DELAY = 5;

    /** Default delay for lava in non-ultrawarm dimensions, matching vanilla. */
    public static final int DEFAULT_LAVA_DELAY = 30;

    /** Maximum base delay accepted by both fluid tick delay rules. */
    public static final int MAX_FLUID_TICK_DELAY = 72_000;

    private FluidTickDelayUtil() {
    }

    /**
     * The three mutually exclusive states a fluid tick-delay rule value can
     * resolve to. Exactly one mode is active per cached state; the factories in
     * this class guarantee the mutual exclusion.
     */
    public enum Mode {
        /**
         * Freeze sentinel: flow scheduling is frozen and the vanilla default
         * delay is used as the keep-alive period.
         */
        FROZEN,
        /**
         * Vanilla passthrough: the Mixin lets the vanilla {@code getTickDelay}
         * body run untouched, so other mods hooking it also take effect.
         */
        VANILLA,
        /**
         * Explicit numeric override: the Mixin forces the configured delay,
         * shadowing vanilla and any other modification.
         */
        EXPLICIT
    }

    /**
     * Immutable result of computing the cached state for one rule value.
     *
     * <p>{@code delay} is only meaningful for {@link Mode#FROZEN} (vanilla
     * default, used as the keep-alive period) and {@link Mode#EXPLICIT}
     * (configured value). For {@link Mode#VANILLA} it holds the vanilla
     * default but has no consumer.
     */
    public static final class CachedDelayState {
        private final Mode mode;
        private final int delay;

        public CachedDelayState(Mode mode, int delay) {
            this.mode = mode;
            this.delay = delay;
        }

        public Mode mode() {
            return mode;
        }

        /** @return {@code true} only when this state is the freeze sentinel. */
        public boolean frozen() {
            return mode == Mode.FROZEN;
        }

        /** @return {@code true} only when this state is the vanilla passthrough. */
        public boolean vanilla() {
            return mode == Mode.VANILLA;
        }

        public int delay() {
            return delay;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CachedDelayState)) return false;
            CachedDelayState that = (CachedDelayState) o;
            return mode == that.mode && delay == that.delay;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mode, delay);
        }

        @Override
        public String toString() {
            return "CachedDelayState{mode=" + mode + ", delay=" + delay + "}";
        }
    }

    /**
     * @return {@code true} if the rule value represents the freeze sentinel.
     */
    public static boolean isFrozen(String value) {
        return FREEZE.equals(value);
    }

    /**
     * @return {@code true} if the rule value represents the vanilla passthrough sentinel.
     */
    public static boolean isVanilla(String value) {
        return VANILLA.equals(value);
    }

    /**
     * Parses a rule value into a positive integer delay.
     *
     * @return the parsed delay, or {@code null} if the value is not an integer
     *         from 1 through {@link #MAX_FLUID_TICK_DELAY} (zero, negative,
     *         decimal, non-numeric, empty, and out-of-range values are all
     *         rejected).
     */
    public static Integer parsePositiveDelayOrNull(String value) {
        if (value == null) {
            return null;
        }
        try {
            // Use long to catch out-of-range inputs before narrowing.
            long parsed = Long.parseLong(value);
            if (parsed <= 0 || parsed > MAX_FLUID_TICK_DELAY) {
                return null;
            }
            return (int) parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * @return {@code true} if the value is a valid rule value ({@code freeze},
     * {@code vanilla}, or an integer from 1 through {@link #MAX_FLUID_TICK_DELAY}).
     */
    public static boolean isValidRuleValue(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        if (isFrozen(value) || isVanilla(value)) {
            return true;
        }
        return parsePositiveDelayOrNull(value) != null;
    }

    /**
     * Computes the cached state for a water rule value.
     *
     * <p>When frozen, the delay is the vanilla default ({@link #DEFAULT_WATER_DELAY}).
     * When the value is invalid, the vanilla passthrough is returned as the safe
     * fallback (unknown values must not override vanilla).
     *
     * @param value     the current rule string
     * @return an immutable {@link CachedDelayState} that can be directly used to
     *         update the volatile cache fields
     */
    public static CachedDelayState computeWaterState(String value) {
        if (isFrozen(value)) {
            return new CachedDelayState(Mode.FROZEN, DEFAULT_WATER_DELAY);
        }
        if (isVanilla(value)) {
            return new CachedDelayState(Mode.VANILLA, DEFAULT_WATER_DELAY);
        }
        Integer parsed = parsePositiveDelayOrNull(value);
        return parsed != null
                ? new CachedDelayState(Mode.EXPLICIT, parsed)
                : new CachedDelayState(Mode.VANILLA, DEFAULT_WATER_DELAY);
    }

    /**
     * Computes the cached state for a lava rule value.
     *
     * <p>When frozen, the delay is the vanilla default ({@link #DEFAULT_LAVA_DELAY}).
     * When the value is invalid, the vanilla passthrough is returned as the safe
     * fallback (unknown values must not override vanilla).
     *
     * @param value     the current rule string
     * @return an immutable {@link CachedDelayState} that can be directly used to
     *         update the volatile cache fields
     */
    public static CachedDelayState computeLavaState(String value) {
        if (isFrozen(value)) {
            return new CachedDelayState(Mode.FROZEN, DEFAULT_LAVA_DELAY);
        }
        if (isVanilla(value)) {
            return new CachedDelayState(Mode.VANILLA, DEFAULT_LAVA_DELAY);
        }
        Integer parsed = parsePositiveDelayOrNull(value);
        return parsed != null
                ? new CachedDelayState(Mode.EXPLICIT, parsed)
                : new CachedDelayState(Mode.VANILLA, DEFAULT_LAVA_DELAY);
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
