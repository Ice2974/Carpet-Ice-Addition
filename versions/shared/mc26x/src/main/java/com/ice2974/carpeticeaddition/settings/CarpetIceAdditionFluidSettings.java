package com.ice2974.carpeticeaddition.settings;

import carpet.api.settings.Rule;
import com.ice2974.carpeticeaddition.rules.FluidTickDelayUtil;
import com.ice2974.carpeticeaddition.rules.FluidTickDelayValidator;

import static carpet.api.settings.RuleCategory.FEATURE;

/**
 * Fluid tick-delay rules for 26.x (Mojang mappings).
 *
 * <p>The {@link Validator} needs the MC type {@code CommandSourceStack}, so the
 * rule fields cannot live in the MC-free common module. The parsing logic itself
 * is shared via {@link FluidTickDelayUtil}.
 *
 * <p>Cached values ({@link #waterFrozen}, {@link #waterDelay}, etc.) are updated
 * by {@link #refreshCachedValues()} on the server main thread (after
 * {@code parseSettingsClass}, after each rule change via the rule observer, and
 * as a fallback in {@code onServerLoadedWorlds}). They are read from Mixin hot
 * paths ({@code getTickDelay} / {@code tick}).
 */
@SuppressWarnings("unused")
public final class CarpetIceAdditionFluidSettings {
    public static final String ICE = CarpetIceAdditionSettings.ICE;

    private CarpetIceAdditionFluidSettings() {
    }

    @Rule(
            categories = {ICE, FEATURE},
            options = {"freeze", "1", "5", "10"},
            strict = false,
            validators = FluidTickDelayValidator.class
    )
    public static String waterFluidTickDelay = String.valueOf(FluidTickDelayUtil.DEFAULT_WATER_DELAY);

    @Rule(
            categories = {ICE, FEATURE},
            options = {"freeze", "6", "30", "60"},
            strict = false,
            validators = FluidTickDelayValidator.class
    )
    public static String lavaFluidTickDelay = String.valueOf(FluidTickDelayUtil.DEFAULT_LAVA_DELAY);

    // ---- Cached values (written from main thread, read from Mixin hot paths) ----

    public static volatile boolean waterFrozen = false;
    public static volatile int waterDelay = FluidTickDelayUtil.DEFAULT_WATER_DELAY;

    public static volatile boolean lavaFrozen = false;
    public static volatile int lavaDelay = FluidTickDelayUtil.DEFAULT_LAVA_DELAY;

    /**
     * Re-reads the rule fields and refreshes the cached delay / frozen flags.
     * Called on the server main thread after rule registration, on every rule
     * change, and as a fallback in {@code onServerLoadedWorlds}.
     */
    public static void refreshCachedValues() {
        FluidTickDelayUtil.CachedDelayState water = FluidTickDelayUtil.computeWaterState(waterFluidTickDelay);
        waterFrozen = water.frozen();
        waterDelay = water.delay();

        FluidTickDelayUtil.CachedDelayState lava = FluidTickDelayUtil.computeLavaState(lavaFluidTickDelay);
        lavaFrozen = lava.frozen();
        lavaDelay = lava.delay();
    }
}
