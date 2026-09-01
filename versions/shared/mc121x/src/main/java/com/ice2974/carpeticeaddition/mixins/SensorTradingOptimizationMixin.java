package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.VillagerTradingOptimizationAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.sensor.GolemLastSeenSensor;
import net.minecraft.entity.ai.brain.sensor.HurtBySensor;
import net.minecraft.entity.ai.brain.sensor.NearestBedSensor;
import net.minecraft.entity.ai.brain.sensor.NearestItemsSensor;
import net.minecraft.entity.ai.brain.sensor.NearestPlayersSensor;
import net.minecraft.entity.ai.brain.sensor.SecondaryPointsOfInterestSensor;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.VillagerBabiesSensor;
import net.minecraft.entity.ai.brain.sensor.VillagerHostilesSensor;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * villagerTradingOptimization 规则：跳过只为已删除行为服务的村民传感器。
 * 精简任务集中唯一依赖传感器输出的是 MOBS（工作站竞争 / 让位），对应 NearestLivingEntitiesSensor 保留。
 * 仅在规则/名称条件成立且 Brain 确为优化构建（isTradingOptimizationBaked）时才跳过，回退原版后传感器照常运行。
 * 拦截点在 sense 真正到期触发时，Sensor.tick 自身的倒计时不受影响；非村民实体只付出一次 instanceof。
 */
@Mixin(Sensor.class)
public abstract class SensorTradingOptimizationMixin<T extends LivingEntity> {

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/ai/brain/sensor/Sensor;sense(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;)V"
            )
    )
    private void carpetIceAddition$skipUnusedVillagerSensors(ServerWorld world, T entity, Operation<Void> original) {
        if (entity instanceof VillagerTradingOptimizationAccess access
                && access.carpetIceAddition$isTradingOptimizationTarget()
                && access.carpetIceAddition$isTradingOptimizationBaked()
                && carpetIceAddition$isUnusedVillagerSensor()) {
            return;
        }
        original.call(world, entity);
    }

    private boolean carpetIceAddition$isUnusedVillagerSensor() {
        Object sensor = this;
        return sensor instanceof VillagerHostilesSensor
                || sensor instanceof VillagerBabiesSensor
                || sensor instanceof SecondaryPointsOfInterestSensor
                || sensor instanceof NearestItemsSensor
                || sensor instanceof NearestBedSensor
                || sensor instanceof NearestPlayersSensor
                || sensor instanceof HurtBySensor
                || sensor instanceof GolemLastSeenSensor;
    }
}
