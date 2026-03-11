package com.ice2974.carpeticeaddition.settings;

import carpet.api.settings.Rule;

@SuppressWarnings("unused")
public final class CarpetIceAdditionSettings {
    public static final String ICE = "Ice";

    private CarpetIceAdditionSettings() {
    }

    @Rule(categories = {ICE})
    public static boolean safeScaffoldingBreak = false;

    @Rule(categories = {ICE})
    public static boolean crafterStopsWhenOutputBlocked = false;
}
