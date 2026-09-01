package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.VillagerTradingOptimizationAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.GolemSensor;
import net.minecraft.world.entity.ai.sensing.HurtBySensor;
import net.minecraft.world.entity.ai.sensing.NearestBedSensor;
import net.minecraft.world.entity.ai.sensing.NearestItemSensor;
import net.minecraft.world.entity.ai.sensing.PlayerSensor;
import net.minecraft.world.entity.ai.sensing.SecondaryPoiSensor;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.VillagerBabiesSensor;
import net.minecraft.world.entity.ai.sensing.VillagerHostilesSensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * villagerTradingOptimization 规则（MC 26.x）：跳过只为已删除行为服务的村民传感器。
 * 精简任务集中唯一依赖传感器输出的是 MOBS（工作站竞争 / 让位），对应 NearestLivingEntitySensor 保留。
 * 拦截点在 doTick 真正到期触发时，Sensor.tick 自身的倒计时不受影响；非村民实体只付出一次 instanceof。
 */
@Mixin(Sensor.class)
public abstract class SensorTradingOptimizationMixin<T extends LivingEntity> {

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/sensing/Sensor;doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V"
            )
    )
    private void carpetIceAddition$skipUnusedVillagerSensors(ServerLevel level, T entity, Operation<Void> original) {
        if (entity instanceof VillagerTradingOptimizationAccess access
                && access.carpetIceAddition$isTradingOptimizationTarget()
                && carpetIceAddition$isUnusedVillagerSensor()) {
            return;
        }
        original.call(level, entity);
    }

    private boolean carpetIceAddition$isUnusedVillagerSensor() {
        Object sensor = this;
        return sensor instanceof VillagerHostilesSensor
                || sensor instanceof VillagerBabiesSensor
                || sensor instanceof SecondaryPoiSensor
                || sensor instanceof NearestItemSensor
                || sensor instanceof NearestBedSensor
                || sensor instanceof PlayerSensor
                || sensor instanceof HurtBySensor
                || sensor instanceof GolemSensor;
    }
}
