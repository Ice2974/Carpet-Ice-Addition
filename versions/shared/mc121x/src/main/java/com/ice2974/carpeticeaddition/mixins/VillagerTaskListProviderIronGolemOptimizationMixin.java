package com.ice2974.carpeticeaddition.mixins;

import com.google.common.collect.ImmutableList;
import com.ice2974.carpeticeaddition.rules.IronGolemSkipMarked;
import com.mojang.datafixers.util.Pair;

import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.entity.ai.brain.task.VillagerTaskListProvider;
import net.minecraft.entity.passive.VillagerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.x）：构建期标记层。
 * 在村民每次建脑（初建 / 职业变更 / 成长 / NBT 读入 / reinitializeBrain）都会重走的
 * WORK / PLAY 任务列表构建点，对顶层任务打可跳过标记；标记与规则开关、村民名称完全解耦，
 * 之后任意时刻改名或开关规则都能即时生效。
 * 优先级 99 的日程切换项（ScheduleActivityTask 的匿名 SingleTickTask）必须排除：
 * 若被否决，村民将永久停留在 WORK / PLAY，无法按日程切换到 REST 睡眠，破坏铁傀儡生成链。
 * 仅按方法名匹配注入，兼容 1.21.3 / 1.21.4 的 VillagerProfession 与 1.21.5+ 的
 * RegistryEntry&lt;VillagerProfession&gt; 参数差异。
 */
@Mixin(VillagerTaskListProvider.class)
public abstract class VillagerTaskListProviderIronGolemOptimizationMixin {

    private static final int ACTIVITY_SCHEDULE_UPDATE_PRIORITY = 99;

    private static final Logger LOGGER = LoggerFactory.getLogger("CarpetIceAddition/IronGolemSpawningOptimization");

    private static final Set<String> REPORTED_UNMARKABLE_TASKS = ConcurrentHashMap.newKeySet();

    @Inject(method = "createWorkTasks", at = @At("RETURN"))
    private static void carpetIceAddition$markWorkTasksForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>>> cir) {
        carpetIceAddition$markSkippedTopLevelTasks(cir.getReturnValue(), "createWorkTasks");
    }

    @Inject(method = "createPlayTasks", at = @At("RETURN"))
    private static void carpetIceAddition$markPlayTasksForIronGolemOptimization(CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>>> cir) {
        carpetIceAddition$markSkippedTopLevelTasks(cir.getReturnValue(), "createPlayTasks");
    }

    private static void carpetIceAddition$markSkippedTopLevelTasks(ImmutableList<Pair<Integer, ? extends Task<? super VillagerEntity>>> tasks, String source) {
        for (Pair<Integer, ? extends Task<? super VillagerEntity>> pair : tasks) {
            if (pair.getFirst() == ACTIVITY_SCHEDULE_UPDATE_PRIORITY) {
                continue;
            }
            Task<?> task = pair.getSecond();
            if (task instanceof IronGolemSkipMarked marked) {
                marked.carpetIceAddition$markIronGolemOptimizationSkipped();
            } else if (REPORTED_UNMARKABLE_TASKS.add(task.getClass().getName())) {
                LOGGER.debug("Skipping iron golem optimization marking for unmarkable task {} in {}", task.getClass().getName(), source);
            }
        }
    }
}
