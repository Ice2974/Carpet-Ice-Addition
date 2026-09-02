package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizer;
import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.FindPointOfInterestTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterestType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.3）：FindPointOfInterestTask 工厂标记（仅找工作点变体）。
 * FindPointOfInterestTask 是纯工厂 holder；本版本存在 4 参与 5 参两个 create 重载，
 * 其中 4 参（HOME / MEETING_POINT 调用）会委托进入 5 参方法体，因此不能只按重载定向，
 * 必须注入“最宽重载”（5 参）RETURN 并以两个 MemoryModuleType 参数引用不等作 guard：
 * 只有 JOB_SITE 变体（JOB_SITE 与 POTENTIAL_JOB_SITE 两个不同实例）命中，
 * HOME / MEETING_POINT（同一实例传两遍）永远不标记——该 guard 与原版自身的包装分支条件一致，
 * HOME / MEETING 不被标记的行为由 IronGolemVillagerOptimizerTest 单测在代码级验证。
 * 该任务被否决后，命名村民不再周期性发起找工作点的 48 格 POI 扫描与寻路；
 * 认床（HOME）与钟聚（MEETING_POINT）变体不受影响。5 参重载仅存在于 1.21.1 / 1.21.3
 * （1.21.4 起被带 BiPredicate 的 6 参重载取代，见 mc1214 / mc1215-12111 档；
 * 1.21.1 副本见 platform-mc1211 档）。
 */
@Mixin(FindPointOfInterestTask.class)
public abstract class FindPointOfInterestTaskIronGolemOptimizationMixin {

    @Inject(
            method = "create(Ljava/util/function/Predicate;Lnet/minecraft/entity/ai/brain/MemoryModuleType;Lnet/minecraft/entity/ai/brain/MemoryModuleType;ZLjava/util/Optional;)Lnet/minecraft/entity/ai/brain/task/Task;",
            at = @At("RETURN")
    )
    private static void carpetIceAddition$markJobSiteVariantForIronGolemOptimization(
            Predicate<RegistryEntry<PointOfInterestType>> poiTypes,
            MemoryModuleType<GlobalPos> poiPosModule,
            MemoryModuleType<GlobalPos> potentialPoiPosModule,
            boolean onlyRunIfChild,
            Optional<Byte> entityStatus,
            CallbackInfoReturnable<Task<PathAwareEntity>> cir) {
        if (!IronGolemVillagerOptimizer.isJobSitePoiVariant(poiPosModule, potentialPoiPosModule)) {
            return;
        }
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "FindPointOfInterestTask.create(jobSite)");
    }
}
