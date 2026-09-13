package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.BetterTridentDespawnState;
import com.ice2974.carpeticeaddition.rules.BetterTridentDespawnTracker;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * betterTridentDespawnCondition 规则的静止计时状态载体：仅 ThrownTrident 实例
 * 承载状态（普通箭实体零额外字段、零行为变化），实现 duck 接口
 * {@link BetterTridentDespawnState}，全部逻辑委托给纯状态机
 * {@link BetterTridentDespawnTracker.State}。
 *
 * <p>状态（anchor、连续有效静止采样数、采样代际）@Unique、非持久、不写 NBT，
 * 随实体对象生灭：区块卸载 / 重载 / 服务器重启后自下一次有效采样从 1 重新累积；
 * 规则值变化经 {@code BetterTridentDespawnEpoch} 代际失效（见入口类 observer）。
 */
@Mixin(ThrownTrident.class)
public abstract class BetterTridentDespawnConditionMixin implements BetterTridentDespawnState {

    @Unique
    private BetterTridentDespawnTracker.State carpetIceAddition$despawnStationaryState =
            new BetterTridentDespawnTracker.State();

    @Override
    public void carpetIceAddition$sampleGroundedStationary(double x, double y, double z, int currentEpoch) {
        this.carpetIceAddition$despawnStationaryState.onGroundedSample(x, y, z, currentEpoch);
    }

    @Override
    public void carpetIceAddition$onDisplacement() {
        this.carpetIceAddition$despawnStationaryState.onDisplacement();
    }

    @Override
    public int carpetIceAddition$stationaryTicks() {
        return this.carpetIceAddition$despawnStationaryState.stationaryTicks();
    }
}
