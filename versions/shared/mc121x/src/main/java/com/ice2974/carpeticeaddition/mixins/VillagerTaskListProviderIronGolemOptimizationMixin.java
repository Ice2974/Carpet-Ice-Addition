package com.ice2974.carpeticeaddition.mixins;

import com.google.common.collect.ImmutableList;
import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;
import com.mojang.datafixers.util.Pair;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.brain.task.VillagerTaskListProvider;
import net.minecraft.entity.passive.VillagerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.x）：构建期标记层。
 * 在村民每次建脑（初建 / 职业变更 / 成长 / NBT 读入 / reinitializeBrain）都会重走的
 * WORK / PLAY 任务列表构建点，对顶层任务打可跳过标记；标记与规则开关、村民名称完全解耦，
 * 之后任意时刻改名或开关规则都能即时生效。
 * 优先级 99 的日程切换项（ScheduleActivityTask 的匿名 SingleTickTask）必须排除：
 * 若被否决，村民将永久停留在 WORK / PLAY，无法按日程切换到 REST 睡眠，破坏铁傀儡生成链。
 * 另标记 IDLE / MEET / PLAY 共用的 freeFollow 张望组合（createFreeFollowTask 返回的
 * RandomTask，即各活动列表中唯一的 priority 5 顶层张望组合，按工厂方法直接标记而非按优先级粗标，
 * 不会波及第三方 Brain 模组加入的其它任务）；REST / PANIC / WORK 的 busyFollow 张望组合
 * （createBusyFollowTask）不标记。仅按方法名匹配注入，兼容 1.21.3 / 1.21.4 的 VillagerProfession
 * 与 1.21.5+ 的 RegistryEntry&lt;VillagerProfession&gt; 参数差异。
 */
@Mixin(VillagerTaskListProvider.class)
public abstract class VillagerTaskListProviderIronGolemOptimizationMixin {

    private static final int ACTIVITY_SCHEDULE_UPDATE_PRIORITY = 99;

    @Inject(method = "createWorkTasks", at = @At("RETURN"))
    private static void carpetIceAddition$markWorkTasksForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>>> cir) {
        carpetIceAddition$markSkippedTopLevelTasks(cir.getReturnValue(), "createWorkTasks");
    }

    @Inject(method = "createPlayTasks", at = @At("RETURN"))
    private static void carpetIceAddition$markPlayTasksForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>>> cir) {
        carpetIceAddition$markSkippedTopLevelTasks(cir.getReturnValue(), "createPlayTasks");
    }

    @Inject(method = "createFreeFollowTask", at = @At("RETURN"))
    private static void carpetIceAddition$markFreeFollowTaskForIronGolemOptimization(CallbackInfoReturnable<Pair<Integer, Task<LivingEntity>>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue().getSecond(), "createFreeFollowTask");
    }

    private static void carpetIceAddition$markSkippedTopLevelTasks(ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> tasks, String source) {
        for (Pair<Integer, ? extends Task<? super VillagerEntity>> pair : tasks) {
            if (pair.getFirst() == ACTIVITY_SCHEDULE_UPDATE_PRIORITY) {
                continue;
            }
            IronGolemVillagerOptimizationHooks.markTaskInstance(pair.getSecond(), source);
        }
    }
}
