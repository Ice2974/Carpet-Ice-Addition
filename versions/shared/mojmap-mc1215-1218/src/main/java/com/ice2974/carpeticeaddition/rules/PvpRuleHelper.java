package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.level.ServerLevel;

public final class PvpRuleHelper {
    private PvpRuleHelper() {
    }

    public static boolean isPvpEnabled(ServerLevel world) {
        return world.getServer().isPvpAllowed();
    }
}
