package com.ice2974.carpeticeaddition.rules;

import java.util.Set;

import net.minecraft.entity.ai.brain.task.GiveGiftsToHeroTask;
import net.minecraft.entity.ai.brain.task.HoldTradeOffersTask;
import net.minecraft.entity.ai.brain.task.JumpInBedTask;
import net.minecraft.entity.ai.brain.task.VillagerBreedTask;

/**
 * ironGolemSpawningOptimization 规则按具体类否决的村民行为集合（MC 1.21.x，Yarn 名）。
 * 仅收录经 1.21.1 / 1.21.8 反编译源码确认：直接继承 MultiTickTask（运行时类即源码类）、
 * 且不出现在铁傀儡生成依赖链（CORE / REST / PANIC / MEET / IDLE 保留部分 / RAID / HIDE）中的行为。
 * 与生成相关的行为（如 PanicTask、SleepTask、GatherItemsVillagerTask、TradeWithVillager 对应链路）
 * 严禁加入本名单。
 */
public final class IronGolemVillagerSkipClasses {

    /**
     * JumpInBedTask：跳床玩闹（IDLE/PLAY），非 SleepTask 的实际睡眠。
     * VillagerBreedTask：繁殖（IDLE）。
     * GiveGiftsToHeroTask：英雄村庄赠礼（WORK/MEET/IDLE）。
     * HoldTradeOffersTask：向玩家展示交易物品（WORK/MEET/IDLE），交易本身由玩家交互驱动。
     */
    public static final Set<Class<?>> SKIP_CLASSES = Set.of(
            JumpInBedTask.class,
            VillagerBreedTask.class,
            GiveGiftsToHeroTask.class,
            HoldTradeOffersTask.class
    );

    private IronGolemVillagerSkipClasses() {
    }
}
