package com.ice2974.carpeticeaddition.rules;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ItemFrameInteractionHelper {
    public static final String INVISIBLE_FRAME_PAID_KEY = "CarpetIceAdditionInvisibleFramePaid";
    public static final String FIXED_FRAME_PAID_KEY = "CarpetIceAdditionFixedFramePaid";
    private static final Logger LOGGER = LoggerFactory.getLogger("CarpetIceAddition/ItemFrames");

    private ItemFrameInteractionHelper() {
    }

    public static FrameCustomizationAction resolveAction(
            boolean playerIsSpectator,
            boolean frameHasItem,
            boolean invisibleRuleEnabled,
            boolean fixedRuleEnabled,
            boolean frameInvisible,
            boolean frameFixed,
            boolean holdsPhantomMembrane,
            boolean holdsGlassPane,
            boolean holdsAxe
    ) {
        if (playerIsSpectator || !frameHasItem) {
            return FrameCustomizationAction.NONE;
        }

        if (fixedRuleEnabled && holdsGlassPane) {
            return frameFixed ? FrameCustomizationAction.CONSUME_INTERACTION : FrameCustomizationAction.APPLY_FIXED;
        }

        if (invisibleRuleEnabled && holdsPhantomMembrane) {
            return frameInvisible ? FrameCustomizationAction.CONSUME_INTERACTION : FrameCustomizationAction.APPLY_INVISIBLE;
        }

        if (fixedRuleEnabled && holdsAxe && frameFixed) {
            return FrameCustomizationAction.CLEAR_FIXED;
        }

        return FrameCustomizationAction.NONE;
    }

    public enum FrameCustomizationAction {
        NONE,
        CONSUME_INTERACTION,
        APPLY_INVISIBLE,
        APPLY_FIXED,
        CLEAR_FIXED
    }

    public static void logInvisibleApplied(UUID entityUuid, boolean serverSide, boolean creativeMode, boolean paidBefore, boolean paidAfter) {
        LOGGER.debug(
                "Applied invisible item frame rule: uuid={}, serverSide={}, creativeMode={}, invisiblePaidBefore={}, invisiblePaidAfter={}",
                entityUuid,
                serverSide,
                creativeMode,
                paidBefore,
                paidAfter
        );
    }

    public static void logFixedApplied(UUID entityUuid, boolean serverSide, boolean creativeMode, boolean paidBefore, boolean paidAfter) {
        LOGGER.debug(
                "Applied fixed item frame rule: uuid={}, serverSide={}, creativeMode={}, fixedPaidBefore={}, fixedPaidAfter={}",
                entityUuid,
                serverSide,
                creativeMode,
                paidBefore,
                paidAfter
        );
    }

    public static void logFixedClearAttempt(UUID entityUuid, boolean fixedState, boolean creativeMode, boolean refundTriggered) {
        LOGGER.debug(
                "Clearing fixed item frame: uuid={}, fixedState={}, creativeMode={}, refundTriggered={}",
                entityUuid,
                fixedState,
                creativeMode,
                refundTriggered
        );
    }

    public static void logInvisibleRefundAttempt(UUID entityUuid, boolean invisibleState, boolean creativeMode, boolean refundTriggered) {
        LOGGER.debug(
                "Breaking item frame: uuid={}, invisibleState={}, creativeMode={}, invisibleRefundTriggered={}",
                entityUuid,
                invisibleState,
                creativeMode,
                refundTriggered
        );
    }

    public static void logFixedRefundAttempt(UUID entityUuid, boolean fixedState, boolean creativeMode, boolean refundTriggered) {
        LOGGER.debug(
                "Breaking item frame: uuid={}, fixedState={}, creativeMode={}, fixedRefundTriggered={}",
                entityUuid,
                fixedState,
                creativeMode,
                refundTriggered
        );
    }
}
