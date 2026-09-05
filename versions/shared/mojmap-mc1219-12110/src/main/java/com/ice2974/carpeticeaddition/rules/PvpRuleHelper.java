package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

public final class PvpRuleHelper {
    private PvpRuleHelper() {
    }

    public static boolean isPvpEnabled(ServerLevel world) {
        return world.getGameRules().getBoolean(GameRules.RULE_PVP);
    }
}
