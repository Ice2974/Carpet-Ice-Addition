//#if MC<260000
package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.SetRaidStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.x）：StartRaidTask 工厂标记（CORE p0）。
 * 纯工厂 holder（create 返回匿名 SingleTickTask），不能进类名单。
 * 原版逻辑：CORE 常驻，1/20 概率每 tick 检查 world.getRaidAt，发现袭击时
 * setDefaultActivity + doExclusively 进入 PRE_RAID / RAID，不写任何 memory——
 * 否决本任务即从根源阻止命名村民进入袭击活动，也不会留下需清理的 memory。
 * 命名时已处于 RAID / PRE_RAID 的村民不受影响：两个包本体未标记，
 * 其 p99 的 EndRaidTask 在袭击结束后 setDefaultActivity(IDLE) + refreshActivities 退出。
 * 类名与 create() 签名在 1.21.1-1.21.11 全版本稳定（已逐版本核对）。
 */
@Mixin(SetRaidStatus.class)
public abstract class StartRaidTaskIronGolemOptimizationMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private static void carpetIceAddition$markForIronGolemOptimization(CallbackInfoReturnable<BehaviorControl<LivingEntity>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "StartRaidTask.create");
    }
}
//#endif
