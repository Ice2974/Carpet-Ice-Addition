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
 * WORK / PLAY / IDLE / MEET 任务列表构建点，对顶层任务打可跳过标记；标记与规则开关、
 * 村民名称完全解耦，之后任意时刻改名或开关规则都能即时生效。
 * 优先级 99 的日程切换项（ScheduleActivityTask 的匿名 SingleTickTask）必须排除：
 * 若被否决，村民将永久停留在对应活动，无法按日程切换到 REST 睡眠，破坏铁傀儡生成链。
 * IDLE / MEET 在本规则语义（现代恐吓式刷铁机）下整表禁用，不保留 gossip 链：
 * 命名村民白天保持静止，仅经 p99 日程切换在夜间进入 REST 睡眠、或经 CORE 的
 * PanicTask 进入 PANIC 触发铁傀儡生成；命名时已处于 MEET 的村民由保留下来的
 * p99 日程切换自然退出，不会卡死。
 * 另标记 REST / PANIC / WORK / PRE_RAID / RAID / HIDE 共用的 busyFollow 张望组合
 * （createBusyFollowTask 返回的 RandomTask，纯张望行为，与生成链无关）；
 * freeFollow（createFreeFollowTask）只被 IDLE / MEET / PLAY 三个已整表标记的列表使用，
 * 无需单独标记。
 * 仅按方法名匹配注入，兼容 1.21.3 的 VillagerProfession 与 1.21.5+ 的
 * RegistryEntry&lt;VillagerProfession&gt; 参数差异；createIdleTasks / createMeetTasks /
 * createBusyFollowTask 在 1.21.1-1.21.11 全版本保持同名（已逐版本核对）。
 * 设计目标是标记 Vanilla provider 构建出的任务；第三方模组若修改同一活动任务表，
 * 是否被连带标记取决于其实现及 Mixin 顺序，兼容性需单独确认。
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

    @Inject(method = "createIdleTasks", at = @At("RETURN"))
    private static void carpetIceAddition$markIdleTasksForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>>> cir) {
        carpetIceAddition$markSkippedTopLevelTasks(cir.getReturnValue(), "createIdleTasks");
    }

    @Inject(method = "createMeetTasks", at = @At("RETURN"))
    private static void carpetIceAddition$markMeetTasksForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>>> cir) {
        carpetIceAddition$markSkippedTopLevelTasks(cir.getReturnValue(), "createMeetTasks");
    }

    @Inject(method = "createBusyFollowTask", at = @At("RETURN"))
    private static void carpetIceAddition$markBusyFollowTaskForIronGolemOptimization(CallbackInfoReturnable<Pair<Integer, Task<LivingEntity>>> cir) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(cir.getReturnValue().getSecond(), "createBusyFollowTask");
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
