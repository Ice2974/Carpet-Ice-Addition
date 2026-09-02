package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizer;
import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;

import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.FindPointOfInterestTask;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.poi.PointOfInterestType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.4）：FindPointOfInterestTask 工厂标记（找工作点 / 找聚会点变体）。
 * FindPointOfInterestTask 是纯工厂 holder；1.21.4 起 create 扩展为 4 参 / 5 参（验床 BiPredicate）/
 * 6 参三个重载，其中 4 参与 5 参（HOME / MEETING_POINT 调用）都会委托进入 6 参方法体，
 * 因此不能只按重载定向，必须注入“最宽重载”（6 参）RETURN 并以 MemoryModuleType
 * 参数判定作 guard：JOB_SITE 变体（JOB_SITE 与 POTENTIAL_JOB_SITE 两个不同实例）以
 * 引用不等命中，MEETING_POINT 变体（MEETING_POINT 同一实例传两遍）以目标 memory 与
 * MEETING_POINT 常量引用相等命中；HOME 变体（HOME 同一实例传两遍，验床 BiPredicate）
 * 两个判定均不命中，永远不标记——认床能力保持原版，HOME 已认领时该任务本就被
 * absent 门控闲置。被否决的两个变体使命名村民不再周期性发起找工作点 / 找聚会点的
 * 48 格 POI 扫描与寻路；无钟铁塔中 MEETING_POINT 永远缺失，该变体原本每 20-40t
 * 触发一次全量 POI 查询。HOME / MEETING 不被 jobSite 判定标记的行为由
 * IronGolemVillagerOptimizerTest 单测在代码级验证。6 参重载的 descriptor 在
 * 1.21.4-1.21.11 间完全一致（1.21.5-1.21.11 副本见 mc1215-12111 档；
 * 1.21.4 平台不引入该共享档，故此处独立存放一份）。
 */
@Mixin(FindPointOfInterestTask.class)
public abstract class FindPointOfInterestTaskIronGolemOptimizationMixin {

    @Inject(
            method = "create(Ljava/util/function/Predicate;Lnet/minecraft/entity/ai/brain/MemoryModuleType;Lnet/minecraft/entity/ai/brain/MemoryModuleType;ZLjava/util/Optional;Ljava/util/function/BiPredicate;)Lnet/minecraft/entity/ai/brain/task/Task;",
            at = @At("RETURN")
    )
    private static void carpetIceAddition$markJobSiteVariantForIronGolemOptimization(
            Predicate<RegistryEntry<PointOfInterestType>> poiTypes,
            MemoryModuleType<GlobalPos> poiPosModule,
            MemoryModuleType<GlobalPos> potentialPoiPosModule,
            boolean onlyRunIfChild,
            Optional<Byte> entityStatus,
            BiPredicate<ServerWorld, BlockPos> poiPosPredicate,
            CallbackInfoReturnable<Task<PathAwareEntity>> cir) {
        if (!IronGolemVillagerOptimizer.isJobSitePoiVariant(poiPosModule, potentialPoiPosModule)
                && !IronGolemVillagerOptimizer.isMeetingPoiVariant(poiPosModule, MemoryModuleType.MEETING_POINT)) {
            return;
        }
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "FindPointOfInterestTask.create(jobSite|meetingPoint)");
    }
}
