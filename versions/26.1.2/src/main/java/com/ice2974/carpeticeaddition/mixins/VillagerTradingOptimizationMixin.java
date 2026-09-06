package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.VillagerTradingOptimizationAccess;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * villagerTradingOptimization 规则（MC 26.x）：村民侧状态实现。
 * Brain 的活动列表过滤在 BrainProviderTradingOptimizationMixin 中完成，
 * 这里只提供优化目标判定、烘焙状态与原版 refreshBrain 触发。
 */
@Mixin(Villager.class)
public abstract class VillagerTradingOptimizationMixin implements VillagerTradingOptimizationAccess {

    @Unique
    private boolean carpetIceAddition$tradingOptimizationBaked;

    @Unique
    @Override
    public boolean carpetIceAddition$isTradingOptimizationTarget() {
        if (!CarpetIceAdditionSettings.villagerTradingOptimization) {
            return false;
        }
        Component customName = ((Villager) (Object) this).getCustomName();
        return customName != null && CARPET_ICE_ADDITION_TRADE_NAME.equals(customName.getString());
    }

    @Override
    public boolean carpetIceAddition$isTradingOptimizationBaked() {
        return carpetIceAddition$tradingOptimizationBaked;
    }

    @Override
    public void carpetIceAddition$markTradingOptimizationBaked(boolean baked) {
        carpetIceAddition$tradingOptimizationBaked = baked;
    }

    @Unique
    @Override
    public void carpetIceAddition$refreshTradingOptimizationBrain() {
        Villager self = (Villager) (Object) this;
        if (self.level() instanceof ServerLevel serverLevel) {
            self.refreshBrain(serverLevel);
        }
    }
}
