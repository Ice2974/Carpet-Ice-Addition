package com.ice2974.carpeticeaddition.settings;

import carpet.api.settings.Rule;

import static carpet.api.settings.RuleCategory.FEATURE;

public final class CarpetIceAdditionHighVersionSettings {
    private CarpetIceAdditionHighVersionSettings() {
    }

    @Rule(categories = {CarpetIceAdditionSettings.ICE, FEATURE})
    public static boolean mobsSpawnWithoutSpears = false;
}
