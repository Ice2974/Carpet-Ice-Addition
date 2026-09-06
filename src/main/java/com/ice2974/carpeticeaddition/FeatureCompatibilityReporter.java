package com.ice2974.carpeticeaddition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 功能兼容性问题的去重上报（Phase 4 P4-3 从各平台入口类逐字抽取；判断顺序、异常优先级、
 * 日志输出与 fallback 语义与原实现完全一致，不做结构改写）。Logger 名与入口类 MOD_NAME 保持一致。
 */
public final class FeatureCompatibilityReporter {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");

    private static final AtomicBoolean SAFE_SCAFFOLDING_BREAK_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean CRAFTER_OUTPUT_RULE_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean RECORD_WORLD_EVENT_FIX_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean SPAWNERS_IGNORE_INVISIBLE_PLAYERS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean DISABLE_KELP_NATURAL_GROWTH_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean DISABLE_AMETHYST_GROWTH_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean CAN_MINE_BUDDING_AMETHYST_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean CAN_MINE_FROSTED_ICE_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean FROSTED_ICE_PROPER_TOOL_FIX_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean BEACON_PROPER_TOOL_FIX_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean ICE_LIKE_MAGMA_BLOCKS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean DISABLE_NYLIUM_DECAY_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean BOT_TAB_LIST_NAME_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean DISABLE_PLAYER_ATTACKING_TAMED_MOBS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean FAKE_PLAYER_IGNORE_THORNS_DAMAGE_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean EASY_WATERLOGGED_BLOCK_PLACEMENT_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean PHANTOM_SPAWN_WARNING_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean NEUTRAL_PHANTOMS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean KILLITEM_TEXT_EVENTS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean CRAFTABLE_CORAL_BLOCKS_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean VILLAGER_TRADING_OPTIMIZATION_ERROR_REPORTED = new AtomicBoolean(false);

    public static void reportFeatureCompatibilityIssue(String featureName, Throwable throwable) {
        AtomicBoolean flag;
        if ("safeScaffoldingBreak".equals(featureName)) {
            flag = SAFE_SCAFFOLDING_BREAK_ERROR_REPORTED;
        } else if ("crafterStopsWhenOutputBlocked".equals(featureName)) {
            flag = CRAFTER_OUTPUT_RULE_ERROR_REPORTED;
        } else if ("recordWorldEventFix".equals(featureName)) {
            flag = RECORD_WORLD_EVENT_FIX_ERROR_REPORTED;
        } else if ("spawnersIgnoreInvisiblePlayers".equals(featureName)) {
            flag = SPAWNERS_IGNORE_INVISIBLE_PLAYERS_ERROR_REPORTED;
        } else if ("disableKelpNaturalGrowth".equals(featureName)) {
            flag = DISABLE_KELP_NATURAL_GROWTH_ERROR_REPORTED;
        } else if ("disableAmethystGrowth".equals(featureName)) {
            flag = DISABLE_AMETHYST_GROWTH_ERROR_REPORTED;
        } else if ("silkTouchBuddingAmethyst".equals(featureName)) {
            flag = CAN_MINE_BUDDING_AMETHYST_ERROR_REPORTED;
        } else if ("silkTouchFrostedIce".equals(featureName)) {
            flag = CAN_MINE_FROSTED_ICE_ERROR_REPORTED;
        } else if ("frostedIceProperToolFix".equals(featureName)) {
            flag = FROSTED_ICE_PROPER_TOOL_FIX_ERROR_REPORTED;
        } else if ("beaconProperToolFix".equals(featureName)) {
            flag = BEACON_PROPER_TOOL_FIX_ERROR_REPORTED;
        } else if ("iceLikeMagmaBlocks".equals(featureName)) {
            flag = ICE_LIKE_MAGMA_BLOCKS_ERROR_REPORTED;
        } else if ("disableNyliumDecay".equals(featureName)) {
            flag = DISABLE_NYLIUM_DECAY_ERROR_REPORTED;
        } else if ("botTabListName".equals(featureName)
                || "botTabListNamePrefix".equals(featureName)
                || "botTabListNameSuffix".equals(featureName)) {
            flag = BOT_TAB_LIST_NAME_ERROR_REPORTED;
        } else if ("disablePlayerAttackingTamedMobs".equals(featureName)) {
            flag = DISABLE_PLAYER_ATTACKING_TAMED_MOBS_ERROR_REPORTED;
        } else if ("fakePlayerIgnoreThornsDamage".equals(featureName)) {
            flag = FAKE_PLAYER_IGNORE_THORNS_DAMAGE_ERROR_REPORTED;
        } else if ("easyWaterloggedBlockPlacement".equals(featureName)) {
            flag = EASY_WATERLOGGED_BLOCK_PLACEMENT_ERROR_REPORTED;
        } else if ("phantomSpawnWarning".equals(featureName)) {
            flag = PHANTOM_SPAWN_WARNING_ERROR_REPORTED;
        } else if ("neutralPhantoms".equals(featureName)) {
            flag = NEUTRAL_PHANTOMS_ERROR_REPORTED;
        } else if ("killitemTextEvents".equals(featureName)) {
            flag = KILLITEM_TEXT_EVENTS_ERROR_REPORTED;
        } else if ("craftableCoralBlocks".equals(featureName)) {
            flag = CRAFTABLE_CORAL_BLOCKS_ERROR_REPORTED;
        } else if ("villagerTradingOptimization".equals(featureName)) {
            flag = VILLAGER_TRADING_OPTIMIZATION_ERROR_REPORTED;
        } else {
            LOGGER.warn("[Carpet Ice Addition] Compatibility issue in feature {}: {}", featureName, throwable.toString());
            return;
        }

        if (flag.compareAndSet(false, true)) {
            LOGGER.warn("[Carpet Ice Addition] Compatibility issue in feature {}. Feature will be safely skipped. Cause: {}",
                    featureName, throwable.toString());
        }
    }
}
