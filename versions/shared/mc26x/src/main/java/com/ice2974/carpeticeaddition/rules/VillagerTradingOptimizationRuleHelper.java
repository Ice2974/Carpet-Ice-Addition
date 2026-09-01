package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * villagerTradingOptimization 规则变更 / 世界载入后的对账：
 * 遍历已加载村民，凡优化目标状态与 Brain 实际构建状态不一致的，通过原版重建路径刷新。
 * 未加载区块的村民会在下次载入时由原版 NBT 读取路径自行按新条件构建，无需处理。
 */
public final class VillagerTradingOptimizationRuleHelper {

    private VillagerTradingOptimizationRuleHelper() {
    }

    public static void rebuildMismatchedVillagers(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof VillagerTradingOptimizationAccess access
                        && access.carpetIceAddition$isTradingOptimizationTarget() != access.carpetIceAddition$isTradingOptimizationBaked()) {
                    access.carpetIceAddition$refreshTradingOptimizationBrain();
                }
            }
        }
    }
}
