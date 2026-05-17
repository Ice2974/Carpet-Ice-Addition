package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.world.ServerWorld;

public final class PvpRuleHelper {
    private PvpRuleHelper() {
    }

    public static boolean isPvpEnabled(ServerWorld world) {
        return world.getServer().isPvpEnabled();
    }
}
