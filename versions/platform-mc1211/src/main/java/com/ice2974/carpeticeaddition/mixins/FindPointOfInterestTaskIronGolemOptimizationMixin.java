package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizer;
import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.1）：FindPointOfInterestTask 工厂标记（找工作点 / 找聚会点变体）。
 * FindPointOfInterestTask 是纯工厂 holder；本版本存在 4 参与 5 参两个 create 重载，
 * 其中 4 参（HOME / MEETING_POINT 调用）会委托进入 5 参方法体，因此不能只按重载定向，
 * 必须注入“最宽重载”（5 参）RETURN 并以 MemoryModuleType 参数判定作 guard：
 * JOB_SITE 变体（JOB_SITE 与 POTENTIAL_JOB_SITE 两个不同实例）以引用不等命中，
 * MEETING_POINT 变体（MEETING_POINT 同一实例传两遍）以目标 memory 与
 * MEETING_POINT 常量引用相等命中；HOME 变体（HOME 同一实例传两遍）两个判定均不命中，
 * 永远不标记——认床能力保持原版，HOME 已认领时该任务本就被 absent 门控闲置。
 * 被否决的两个变体使命名村民不再周期性发起找工作点 / 找聚会点的 48 格 POI 扫描与寻路；
 * 无钟铁塔中 MEETING_POINT 永远缺失，该变体原本每 20-40t 触发一次全量 POI 查询。
 * HOME / MEETING 不被 jobSite 判定标记的行为由 IronGolemVillagerOptimizerTest 单测
 * 在代码级验证。5 参重载仅存在于 1.21.1 / 1.21.3（1.21.4 起被带 BiPredicate 的
 * 6 参重载取代，见 mc1214 / mc1215-12111 档）。
 */
@Mixin(AcquirePoi.class)
public abstract class FindPointOfInterestTaskIronGolemOptimizationMixin {

    @Inject(
            method = "create(Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;ZLjava/util/Optional;)Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;",
            at = @At("RETURN")
    )
    private static void carpetIceAddition$markJobSiteVariantForIronGolemOptimization(
            Predicate<Holder<PoiType>> poiTypes,
            MemoryModuleType<GlobalPos> poiPosModule,
            MemoryModuleType<GlobalPos> potentialPoiPosModule,
            boolean onlyRunIfChild,
            Optional<Byte> entityStatus,
            CallbackInfoReturnable<BehaviorControl<PathfinderMob>> cir) {
        if (!IronGolemVillagerOptimizer.isJobSitePoiVariant(poiPosModule, potentialPoiPosModule)
                && !IronGolemVillagerOptimizer.isMeetingPoiVariant(poiPosModule, MemoryModuleType.MEETING_POINT)) {
            return;
        }
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue(), "FindPointOfInterestTask.create(jobSite|meetingPoint)");
    }
}
