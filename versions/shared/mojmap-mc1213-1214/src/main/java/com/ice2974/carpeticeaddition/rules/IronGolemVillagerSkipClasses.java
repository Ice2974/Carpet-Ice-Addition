package com.ice2974.carpeticeaddition.rules;

import java.util.Set;
import net.minecraft.world.entity.ai.behavior.LookAndFollowTradingPlayerSink;

/**
 * ironGolemSpawningOptimization 规则按具体类否决的村民行为集合（MC 1.21.x，Yarn 名）。
 * 仅收录经反编译源码逐调用点核对确认：直接继承 MultiTickTask（运行时类即源码类）、
 * 且村民的全部使用位置都在 CORE（未被整表标记的活动）中的行为——
 * IDLE / WORK / PLAY / MEET 内的具体行为已由 VillagerTaskListProvider 构建层
 * 整表标记覆盖（含 JumpInBedTask、VillagerBreedTask、GiveGiftsToHeroTask、
 * HoldTradeOffersTask，不再列入本名单形成重复否决路径）。
 * 与生成链相关的行为（如 PanicTask、SleepTask、MoveToTargetTask）严禁加入本名单。
 */
public final class IronGolemVillagerSkipClasses {

    /**
     * FollowCustomerTask：CORE p3，跟随 / 张望正在交易的玩家，与铁傀儡生成链无关；
     * 由 provider 直接 new（非工厂 holder），类名在 1.21.1-1.21.11 全版本稳定。
     */
    public static final Set<Class<?>> SKIP_CLASSES = Set.of(
            LookAndFollowTradingPlayerSink.class
    );

    private IronGolemVillagerSkipClasses() {
    }
}
