package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.VillagerTradingOptimizationAccess;
import com.ice2974.carpeticeaddition.rules.VillagerTradingOptimizationTasks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * villagerTradingOptimization 规则（MC 26.x）：活动列表注册点过滤。
 * Brain.Provider.makeBrain 内对 ActivitySupplier.createActivities 的唯一调用携带实体参数，
 * 在此把优化目标村民的活动列表替换为精简 CORE/WORK/IDLE（原版 Brain 构造器与
 * registerBrainGoals 的编排保持原样运行）。构造器（构造路径）、LivingEntity NBT 读取
 * （makeBrain 路径）与 refreshBrain 三条构建路径全部经过此处。
 */
@Mixin(Brain.Provider.class)
public abstract class BrainProviderTradingOptimizationMixin {

    @WrapOperation(
            method = "makeBrain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/Brain$ActivitySupplier;createActivities(Lnet/minecraft/world/entity/LivingEntity;)Ljava/util/List;"
            )
    )
    private List<ActivityData<Villager>> carpetIceAddition$filterActivities(
            Brain.ActivitySupplier<Villager> supplier, LivingEntity entity,
            Operation<List<ActivityData<Villager>>> original) {
        if (!(entity instanceof VillagerTradingOptimizationAccess access)) {
            return original.call(supplier, entity);
        }
        boolean target = access.carpetIceAddition$isTradingOptimizationTarget();
        access.carpetIceAddition$markTradingOptimizationBaked(target);
        if (!target) {
            return original.call(supplier, entity);
        }
        try {
            return VillagerTradingOptimizationTasks.createActivities((Villager) entity);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("villagerTradingOptimization", throwable);
            access.carpetIceAddition$markTradingOptimizationBaked(false);
            return original.call(supplier, entity);
        }
    }
}
