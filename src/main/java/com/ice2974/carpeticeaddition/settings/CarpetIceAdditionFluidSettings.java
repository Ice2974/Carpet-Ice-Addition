package com.ice2974.carpeticeaddition.settings;

import carpet.api.settings.Rule;
import com.ice2974.carpeticeaddition.rules.FluidTickDelayUtil;
import com.ice2974.carpeticeaddition.rules.FluidTickDelayValidator;

import static carpet.api.settings.RuleCategory.FEATURE;

/**
 * Fluid tick-delay rules.
 *
 * <p>The {@link FluidTickDelayValidator} needs the MC type {@code CommandSourceStack}.
 * The pure parsing logic itself is shared via {@link FluidTickDelayUtil}.
 *
 * <p>The rule default is the {@code vanilla} sentinel: CIA does not override
 * {@code getTickDelay} at all, so vanilla (and other mods hooking the same
 * method) decide the delay. Any explicit integer — including values equal to
 * the vanilla defaults — is a forced override; {@code freeze} freezes flow
 * scheduling while keeping vanilla-default keep-alive ticks.
 *
 * <p>Cached values ({@link #waterFrozen}, {@link #waterDelay}, etc.) are updated
 * by {@link #refreshCachedValues()} on the server main thread (after
 * {@code parseSettingsClass}, after each rule change via the rule observer, and
 * as a fallback in {@code onServerLoadedWorlds}). They are read from Mixin hot
 * paths ({@code getTickDelay} / {@code tick}).
 */
public final class CarpetIceAdditionFluidSettings {
    public static final String ICE = CarpetIceAdditionSettings.ICE;

    private CarpetIceAdditionFluidSettings() {
    }

    @Rule(
            categories = {ICE, FEATURE},
            options = {"freeze", "vanilla"},
            strict = false,
            validators = FluidTickDelayValidator.class
    )
    public static String waterFluidTickDelay = FluidTickDelayUtil.VANILLA;

    @Rule(
            categories = {ICE, FEATURE},
            options = {"freeze", "vanilla"},
            strict = false,
            validators = FluidTickDelayValidator.class
    )
    public static String lavaFluidTickDelay = FluidTickDelayUtil.VANILLA;

    // ---- Cached values (written from main thread, read from Mixin hot paths) ----

    public static volatile boolean waterFrozen = false;
    public static volatile boolean waterVanilla = true;
    public static volatile int waterDelay = FluidTickDelayUtil.DEFAULT_WATER_DELAY;

    public static volatile boolean lavaFrozen = false;
    public static volatile boolean lavaVanilla = true;
    public static volatile int lavaDelay = FluidTickDelayUtil.DEFAULT_LAVA_DELAY;

    /**
     * Re-reads the rule fields and refreshes the cached mode / delay / frozen
     * flags. Called on the server main thread after rule registration, on every
     * rule change, and as a fallback in {@code onServerLoadedWorlds}.
     */
    public static void refreshCachedValues() {
        FluidTickDelayUtil.CachedDelayState water = FluidTickDelayUtil.computeWaterState(waterFluidTickDelay);
        waterFrozen = water.frozen();
        waterVanilla = water.vanilla();
        waterDelay = water.delay();

        FluidTickDelayUtil.CachedDelayState lava = FluidTickDelayUtil.computeLavaState(lavaFluidTickDelay);
        lavaFrozen = lava.frozen();
        lavaVanilla = lava.vanilla();
        lavaDelay = lava.delay();
    }
}
