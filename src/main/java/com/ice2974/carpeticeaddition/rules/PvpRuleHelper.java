//#if MC>=12105
package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;

public final class PvpRuleHelper {
    private PvpRuleHelper() {
    }

    public static boolean isPvpEnabled(ServerLevel world) {
//#if MC>=12111
        return world.getGameRules().get(GameRules.PVP);
//#elseif MC>=12109
//$$        return world.getGameRules().getBoolean(GameRules.RULE_PVP);
//#else
//$$        return world.getServer().isPvpAllowed();
//#endif

    }
}

//#endif
