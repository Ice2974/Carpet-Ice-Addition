package com.ice2974.carpeticeaddition.settings;

import carpet.api.settings.Rule;
import carpet.api.settings.Validators;

import static carpet.api.settings.RuleCategory.BUGFIX;
import static carpet.api.settings.RuleCategory.CLIENT;
import static carpet.api.settings.RuleCategory.COMMAND;
import static carpet.api.settings.RuleCategory.FEATURE;
import static carpet.api.settings.RuleCategory.SURVIVAL;

public final class CarpetIceAdditionSettings {
    public static final String ICE = "CarpetIceAddition";
    public static final String BOT = "BOT";

    private CarpetIceAdditionSettings() {
    }

    @Rule(categories = {ICE, FEATURE})
    public static boolean safeScaffoldingBreak = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean crafterStopsWhenOutputBlocked = false;

    // craftableCoralBlocks 规则定义在各平台模块的 CraftableCoralBlocksSettings：
    // 其 Validator 需引用 MC 类（ServerCommandSource / CommandSourceStack），无法放入 common。

    @Rule(categories = {ICE, BUGFIX})
    public static boolean recordWorldEventFix = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean spawnersIgnoreInvisiblePlayers = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean disableKelpNaturalGrowth = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean disableAmethystGrowth = false;

    @Rule(categories = {ICE, FEATURE, SURVIVAL})
    public static boolean silkTouchBuddingAmethyst = false;

    @Rule(categories = {ICE, FEATURE, SURVIVAL})
    public static boolean silkTouchFrostedIce = false;

    @Rule(categories = {ICE, BUGFIX, SURVIVAL, CLIENT})
    public static boolean frostedIceProperToolFix = false;

    @Rule(categories = {ICE, FEATURE, SURVIVAL})
    public static boolean iceLikeMagmaBlocks = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean disableNyliumDecay = false;

    @Rule(categories = {ICE, FEATURE, SURVIVAL, BOT})
    public static boolean fakePlayerIgnoreThornsDamage = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean invisibleItemFrames = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean fixedItemFrames = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean easyWaterloggedBlockPlacement = false;

    @Rule(categories = {ICE, FEATURE, SURVIVAL})
    public static boolean portableInfiniteWater = false;

    @Rule(categories = {ICE, FEATURE, CLIENT})
    public static boolean disableIllegalTextCharacterCheck = false;

    @Rule(categories = {ICE, FEATURE})
    public static boolean disablePlayerAttackingTamedMobs = false;

    @Rule(categories = {ICE, SURVIVAL})
    public static boolean phantomSpawnWarning = false;

    @Rule(categories = {ICE, FEATURE, SURVIVAL})
    public static boolean neutralPhantoms = false;

    @Rule(
            categories = {ICE, COMMAND},
            options = {"false", "true", "ops", "0", "1", "2", "3", "4"},
            validators = Validators.CommandLevel.class
    )
    public static String commandKillItem = "ops";

    @Rule(
            categories = {ICE, COMMAND},
            options = {"false", "true", "ops", "0", "1", "2", "3", "4"},
            validators = Validators.CommandLevel.class
    )
    public static String commandMachineStatus = "ops";

    @Rule(categories = {ICE})
    public static boolean machineStatusRollbackWarning = false;

    @Rule(
            categories = {ICE, BOT},
            options = {"#none", "[Bot]"},
            strict = false
    )
    public static String botTabListNamePrefix = "#none";

    @Rule(
            categories = {ICE, BOT},
            options = {"#none", "[Fake]"},
            strict = false
    )
    public static String botTabListNameSuffix = "#none";
}
