package com.ice2974.carpeticeaddition.settings;

import carpet.api.settings.Rule;
import carpet.api.settings.Validators;

import static carpet.api.settings.RuleCategory.BUGFIX;
import static carpet.api.settings.RuleCategory.COMMAND;
import static carpet.api.settings.RuleCategory.FEATURE;
import static carpet.api.settings.RuleCategory.SURVIVAL;

@SuppressWarnings("unused")
public final class CarpetIceAdditionSettings {
    public static final String ICE = "CarpetIceAddition";

    private CarpetIceAdditionSettings() {
    }

    @Rule(categories = {ICE, SURVIVAL})
    public static boolean safeScaffoldingBreak = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean crafterStopsWhenOutputBlocked = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean craftableCoralBlocks = false;

    @Rule(categories = {ICE, BUGFIX})
    public static boolean recordWorldEventFix = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean spawnersIgnoreInvisiblePlayers = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean disableKelpNaturalGrowth = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean silkTouchBuddingAmethyst = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean silkTouchFrostedIce = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean iceLikeMagmaBlocks = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean disableNyliumDecay = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean fakePlayerIgnoreThornsDamage = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean invisibleItemFrames = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean fixedItemFrames = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean easyWaterloggedBlockPlacement = false;

    @Rule(categories = {ICE, SURVIVAL})
    public static boolean disablePlayerAttackingTamedMobs = false;

    @Rule(categories = {ICE, SURVIVAL})
    public static boolean phantomSpawnWarning = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean neutralPhantoms = false;

    @Rule(
            categories = {ICE, COMMAND},
            options = {"false", "true", "ops", "0", "1", "2", "3", "4"},
            validators = Validators.CommandLevel.class
    )
    public static String commandKillItem = "ops";

    @Rule(
            categories = {ICE, COMMAND, SURVIVAL},
            options = {"false", "true", "ops", "0", "1", "2", "3", "4"},
            validators = Validators.CommandLevel.class
    )
    public static String commandMachineStatus = "ops";

    @Rule(categories = {ICE, SURVIVAL})
    public static boolean machineStatusRollbackWarning = false;

    @Rule(
            categories = {ICE},
            options = {"#none", "[Bot]"},
            strict = false
    )
    public static String botTabListNamePrefix = "#none";

    @Rule(
            categories = {ICE},
            options = {"#none", "[Fake]"},
            strict = false
    )
    public static String botTabListNameSuffix = "#none";
}
