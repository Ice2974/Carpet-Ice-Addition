package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.network.ServerPlayerEntity;

public final class RealPlayerHelper {
    private static final String CARPET_FAKE_PLAYER_CLASS = "carpet.patches.EntityPlayerMPFake";

    private RealPlayerHelper() {
    }

    public static boolean isFakePlayer(ServerPlayerEntity player) {
        Class<?> clazz = player.getClass();
        while (clazz != null) {
            if (CARPET_FAKE_PLAYER_CLASS.equals(clazz.getName())) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }
}
