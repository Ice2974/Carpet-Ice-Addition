package com.ice2974.carpeticeaddition.settings;

import carpet.api.settings.Rule;

import static carpet.api.settings.RuleCategory.BUGFIX;
import static carpet.api.settings.RuleCategory.CLIENT;

/**
 * 仅在较低 MC 版本（1.21–1.21.1）生效的规则。
 * 该类只由对应的平台入口 {@code CarpetIceAdditionMod.onGameStarted()} 选择性 parse，
 * 因此未 parse 的平台不会在游戏内注册或显示这些规则。
 */
public final class CarpetIceAdditionLowVersionSettings {
    private CarpetIceAdditionLowVersionSettings() {
    }

    @Rule(categories = {CarpetIceAdditionSettings.ICE, BUGFIX, CLIENT})
    public static boolean carpetSingleplayerExitCrashFix = true;
}
