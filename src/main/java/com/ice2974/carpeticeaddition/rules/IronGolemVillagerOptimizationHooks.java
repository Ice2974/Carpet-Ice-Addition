package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;

/**
 * ironGolemSpawningOptimization 规则的启动否决判定与构建期标记工具（MC 1.21.x）。
 * 否决只在任务启动路径（MultiTickTask / SingleTickTask / CompositeTask 的 tryStarting HEAD）调用，
 * 判定为 true 时以“启动条件不成立”语义跳过该任务；不干预已 RUNNING 任务的 tick 与 stop。
 * 名称命中结果由村民侧 mixin 以 setter/lazy 双驱动缓存维护，热路径只读布尔字段，不做 Text 解析。
 * <p>
 * 钟声唤醒链（BellBlockEntity 写入 HEARD_BELL_TIME → ReactToBell 切 HIDE → SleepInBed.stop / WakeUp 唤醒）
 * 不得加入任何标记或类名单：两条唤醒路径都以“活动离开 REST”为前提，否决 ReactToBell 会让该 Memory 只写不清，
 * 命名村民再也无法被响钟唤醒。HIDE 包内的清理任务在正常 HIDE 流程中承担 HEARD_BELL_TIME / HIDING_PLACE
 * 的清除与日程回切职责（各平台通用描述，不写死类名；1.21.x 早期 Yarn 注释名为 ForgetBellRingTask，
 * 现 12 个平台的重映射源码名均为 SetHiddenState）。
 * 清除/失效边界：正常 HIDE 流程之外，仅实体移除（Brain#clearMemories）会清除该 Memory；
 * NBT 载入因该 Memory 无 codec 不会恢复（属“不恢复”而非“被清除”）。
 * 重建边界跨版本不一致，勿当作通用结论：1.21.x 的 refreshBrain 使用 copyWithoutBehaviors（成长保留全部 Memory），
 * 26.x 使用 Brain.pack（仅打包可序列化 Memory），成长重建同样丢弃 HEARD_BELL_TIME / HIDING_PLACE；
 * 钟控刷铁机每次唤醒依赖的是重新敲钟写入新 Memory，与该重建差异无关。
 */
public final class IronGolemVillagerOptimizationHooks {

    private static final Logger LOGGER = LoggerFactory.getLogger("CarpetIceAddition/IronGolemSpawningOptimization");

    private static final Set<String> REPORTED_UNMARKABLE_TASKS = ConcurrentHashMap.newKeySet();

    private IronGolemVillagerOptimizationHooks() {
    }

    /**
     * 是否应否决该任务的本次启动尝试。
     * 判定顺序按成本从低到高排列，规则关闭时只付出一次静态字段读取。
     */
    public static boolean shouldVetoStart(Object task, LivingEntity entity) {
        if (!CarpetIceAdditionSettings.ironGolemSpawningOptimization) {
            return false;
        }
        if (!(entity instanceof IronGolemVillagerOptimizationAccess access) || !access.carpetIceAddition$isIronGolemOptimizationTarget()) {
            return false;
        }
        if (task instanceof IronGolemSkipMarked marked && marked.carpetIceAddition$isIronGolemOptimizationSkipped()) {
            return true;
        }
        return IronGolemVillagerSkipClasses.SKIP_CLASSES.contains(task.getClass());
    }

    /**
     * 构建期标记入口：把工厂 / 任务列表构建点产生的任务实例标记为可跳过。
     * 标记与规则开关、村民名称完全解耦，否决时才做实体门控；
     * 其它模组经同一工厂创建的实例若被连带标记，也会因实体门控而永不被否决。
     */
    public static void markTaskInstance(Object task, String source) {
        if (task == null) {
            return;
        }
        if (task instanceof IronGolemSkipMarked marked) {
            marked.carpetIceAddition$markIronGolemOptimizationSkipped();
        } else if (REPORTED_UNMARKABLE_TASKS.add(task.getClass().getName())) {
            LOGGER.debug("Skipping iron golem optimization marking for unmarkable task {} in {}", task.getClass().getName(), source);
        }
    }
}
