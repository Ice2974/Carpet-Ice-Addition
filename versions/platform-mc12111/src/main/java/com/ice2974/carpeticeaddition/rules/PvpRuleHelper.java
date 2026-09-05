package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

public final class PvpRuleHelper {
    private PvpRuleHelper() {
    }

    public static boolean isPvpEnabled(ServerLevel world) {
        return world.getGameRules().get(GameRules.PVP);
    }
}
