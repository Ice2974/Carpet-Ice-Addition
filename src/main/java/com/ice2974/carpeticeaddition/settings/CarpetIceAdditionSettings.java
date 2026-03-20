package com.ice2974.carpeticeaddition.settings;

import carpet.api.settings.Rule;

import static carpet.api.settings.RuleCategory.BUGFIX;
import static carpet.api.settings.RuleCategory.CLIENT;
import static carpet.api.settings.RuleCategory.FEATURE;
import static carpet.api.settings.RuleCategory.SURVIVAL;

@SuppressWarnings("unused")
public final class CarpetIceAdditionSettings {
    public static final String ICE = "carpetIceAddition";

    private CarpetIceAdditionSettings() {
    }

    @Rule(categories = {ICE, SURVIVAL, FEATURE})
    public static boolean safeScaffoldingBreak = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean crafterStopsWhenOutputBlocked = false;

    @Rule(categories = {ICE, BUGFIX})
    public static boolean recordWorldEventFix = false;

    @Rule(categories = {ICE, CLIENT})
    public static boolean disableParticlesPackets = false;
}
