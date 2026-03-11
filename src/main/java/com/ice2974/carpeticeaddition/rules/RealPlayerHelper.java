package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.network.ServerPlayerEntity;

public final class RealPlayerHelper {
    private static final String[] CARPET_FAKE_PLAYER_CLASS_NAMES = {
            "carpet.patches.EntityPlayerMPFake",
            "carpet.fakes.ServerPlayerEntityInterface$FakePlayer"
    };

    private RealPlayerHelper() {
    }

    public static boolean isFakePlayer(ServerPlayerEntity player) {
        Class<?> clazz = player.getClass();
        while (clazz != null) {
            for (String candidate : CARPET_FAKE_PLAYER_CLASS_NAMES) {
                if (candidate.equals(clazz.getName())) {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }
}
