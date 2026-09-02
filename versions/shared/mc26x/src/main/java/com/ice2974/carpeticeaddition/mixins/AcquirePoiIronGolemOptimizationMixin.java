package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizer;
import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * ironGolemSpawningOptimization 规则（MC 26.x）：AcquirePoi 工厂标记（仅找工作点变体）。
 * AcquirePoi 是纯工厂 holder（create 返回 BehaviorBuilder 包装的 OneShot）；
 * 存在 4 参（MEETING_POINT 调用）、5 参（HOME 验床调用）与 6 参三个 create 重载，
 * 其中 4 参与 5 参都会委托进入 6 参方法体，因此不能只按重载定向，必须注入“最宽重载”
 * （6 参）RETURN 并以两个 MemoryModuleType 参数引用不等作 guard：只有 JOB_SITE 变体
 * （JOB_SITE 与 POTENTIAL_JOB_SITE 两个不同实例）命中，HOME / MEETING_POINT
 * （同一实例传两遍）永远不标记——该 guard 与原版自身的包装分支条件一致
 * （memoryToAcquire == memoryToValidate ? 内层 : 外层再包 absent(memoryToValidate)），
 * HOME / MEETING 不被标记的行为由 IronGolemVillagerOptimizerTest 单测在代码级验证。
 * 该任务被否决后，命名村民不再周期性发起找工作点的 48 格 POI 扫描与寻路；
 * 认床（HOME）与钟聚（MEETING_POINT）变体不受影响。descriptor 已对 26.1.2 / 26.2
 * 反编译源码与 javap 双重核对，两版本一致。
 */
@Mixin(AcquirePoi.class)
public abstract class AcquirePoiIronGolemOptimizationMixin {

    @Inject(
            method = "create(Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;ZLjava/util/Optional;Ljava/util/function/BiPredicate;)Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;",
            at = @At("RETURN")
    )
    private static void carpetIceAddition$markJobSiteVariantForIronGolemOptimization(
            Predicate<Holder<PoiType>> poiType,
            MemoryModuleType<GlobalPos> memoryToValidate,
            MemoryModuleType<GlobalPos> memoryToAcquire,
            boolean onlyIfAdult,
            Optional<Byte> onPoiAcquisitionEvent,
            BiPredicate<ServerLevel, BlockPos> validPoi,
            CallbackInfoReturnable<BehaviorControl<PathfinderMob>> cir) {
        if (!IronGolemVillagerOptimizer.isJobSitePoiVariant(memoryToValidate, memoryToAcquire)) {
            return;
        }
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "AcquirePoi.create(jobSite)");
    }
}
