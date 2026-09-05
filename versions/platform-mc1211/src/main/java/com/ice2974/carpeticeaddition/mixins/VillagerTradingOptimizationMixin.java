package com.ice2974.carpeticeaddition.mixins;

import com.google.common.collect.ImmutableList;
import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.VillagerTradingOptimizationAccess;
import com.ice2974.carpeticeaddition.rules.VillagerTradingOptimizationTasks;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * villagerTradingOptimization 规则（MC 1.21 ~ 1.21.1）：
 * 在 initBrain 的任务列表注册点做替换——优化目标仅注册精简 CORE/WORK，
 * 其余活动注册为空列表，活动门控条件仍由原版编排写入。
 * 精简任务列表在 initBrain 开始时一次性预构建：任一异常都会让本次构建完整回退原版注册（baked=false），
 * 不会留下半精简半原版状态。
 * 不写 NBT、不清理 Memory；规则关闭或名称变化后由原版 reinitializeBrain 恢复全量任务。
 */
@Mixin(Villager.class)
public abstract class VillagerTradingOptimizationMixin implements VillagerTradingOptimizationAccess {

    @Unique
    private boolean carpetIceAddition$tradingOptimizationBaked;

    @Unique
    private ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> carpetIceAddition$trimmedCoreTasks;

    @Unique
    private ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> carpetIceAddition$trimmedWorkTasks;

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
        if (self.level() instanceof ServerLevel serverWorld) {
            self.refreshBrain(serverWorld);
        }
    }

    /**
     * 本次 initBrain 构建周期的唯一决策点：
     * 目标成立时预先构建全部精简任务列表，CORE 与 WORK 都构建成功才置 baked=true；
     * 任何异常则两个字段保持为 null（本次构建完整回退原版）并置 baked=false。
     * 后续所有包装器只依据字段是否为 null 决定精简或原版，不再存在可失败路径。
     */
    @Inject(method = "registerBrainGoals", at = @At("HEAD"))
    private void carpetIceAddition$prepareTradingOptimizationTasks(CallbackInfo ci) {
        carpetIceAddition$trimmedCoreTasks = null;
        carpetIceAddition$trimmedWorkTasks = null;
        if (!carpetIceAddition$isTradingOptimizationTarget()) {
            carpetIceAddition$markTradingOptimizationBaked(false);
            return;
        }
        try {
            VillagerProfession profession = ((Villager) (Object) this).getVillagerData().getProfession();
            carpetIceAddition$trimmedCoreTasks = VillagerTradingOptimizationTasks.createCoreTasks(profession, 0.5F);
            carpetIceAddition$trimmedWorkTasks = VillagerTradingOptimizationTasks.createWorkTasks(0.5F);
            carpetIceAddition$markTradingOptimizationBaked(true);
        } catch (Throwable throwable) {
            carpetIceAddition$trimmedCoreTasks = null;
            carpetIceAddition$trimmedWorkTasks = null;
            carpetIceAddition$markTradingOptimizationBaked(false);
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("villagerTradingOptimization", throwable);
        }
    }

    @WrapOperation(
            method = "registerBrainGoals",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/VillagerGoalPackages;getCorePackage(Lnet/minecraft/world/entity/npc/VillagerProfession;F)Lcom/google/common/collect/ImmutableList;"
            )
    )
    private ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> carpetIceAddition$trimCoreTasks(
            VillagerProfession profession, float speed,
            Operation<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> original) {
        if (carpetIceAddition$trimmedCoreTasks == null) {
            return original.call(profession, speed);
        }
        return carpetIceAddition$trimmedCoreTasks;
    }

    @WrapOperation(
            method = "registerBrainGoals",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/VillagerGoalPackages;getWorkPackage(Lnet/minecraft/world/entity/npc/VillagerProfession;F)Lcom/google/common/collect/ImmutableList;"
            )
    )
    private ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> carpetIceAddition$trimWorkTasks(
            VillagerProfession profession, float speed,
            Operation<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> original) {
        if (carpetIceAddition$trimmedWorkTasks == null) {
            return original.call(profession, speed);
        }
        return carpetIceAddition$trimmedWorkTasks;
    }

    @WrapOperation(
            method = "registerBrainGoals",
            require = 7,
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/ai/behavior/VillagerGoalPackages;getMeetPackage(Lnet/minecraft/world/entity/npc/VillagerProfession;F)Lcom/google/common/collect/ImmutableList;"
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/ai/behavior/VillagerGoalPackages;getRestPackage(Lnet/minecraft/world/entity/npc/VillagerProfession;F)Lcom/google/common/collect/ImmutableList;"
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/ai/behavior/VillagerGoalPackages;getIdlePackage(Lnet/minecraft/world/entity/npc/VillagerProfession;F)Lcom/google/common/collect/ImmutableList;"
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/ai/behavior/VillagerGoalPackages;getPanicPackage(Lnet/minecraft/world/entity/npc/VillagerProfession;F)Lcom/google/common/collect/ImmutableList;"
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/ai/behavior/VillagerGoalPackages;getPreRaidPackage(Lnet/minecraft/world/entity/npc/VillagerProfession;F)Lcom/google/common/collect/ImmutableList;"
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/ai/behavior/VillagerGoalPackages;getRaidPackage(Lnet/minecraft/world/entity/npc/VillagerProfession;F)Lcom/google/common/collect/ImmutableList;"
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/entity/ai/behavior/VillagerGoalPackages;getHidePackage(Lnet/minecraft/world/entity/npc/VillagerProfession;F)Lcom/google/common/collect/ImmutableList;"
                    )
            }
    )
    private ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> carpetIceAddition$dropActivityTasks(
            VillagerProfession profession, float speed,
            Operation<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> original) {
        if (carpetIceAddition$trimmedCoreTasks == null) {
            return original.call(profession, speed);
        }
        return ImmutableList.of();
    }

    @WrapOperation(
            method = "registerBrainGoals",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/behavior/VillagerGoalPackages;getPlayPackage(F)Lcom/google/common/collect/ImmutableList;"
            )
    )
    private ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> carpetIceAddition$dropPlayTasks(
            float speed,
            Operation<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> original) {
        if (carpetIceAddition$trimmedCoreTasks == null) {
            return original.call(speed);
        }
        return ImmutableList.of();
    }
}
