package com.ice2974.carpeticeaddition.rules;

public final class IceLikeMagmaBlocksHelper {
    private IceLikeMagmaBlocksHelper() {
    }

    public static boolean isValidSolidSupport(
            boolean blocksMovement,
            boolean isCobweb,
            boolean isBambooSapling
    ) {
        return blocksMovement && !isCobweb && !isBambooSapling;
    }

    public static boolean hasIceLikeSupport(boolean validSolidSupport, boolean liquidSupport) {
        return validSolidSupport || liquidSupport;
    }

    public static boolean shouldCreateLavaSource(
            boolean ruleEnabled,
            boolean hasSilkTouch,
            boolean hasIceLikeSupport
    ) {
        return ruleEnabled && !hasSilkTouch && hasIceLikeSupport;
    }
}
