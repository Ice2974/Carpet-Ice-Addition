package com.ice2974.carpeticeaddition.rules;

import java.util.Set;

import net.minecraft.world.entity.ai.behavior.GoToPotentialJobSite;
import net.minecraft.world.entity.ai.behavior.LookAndFollowTradingPlayerSink;

/**
 * ironGolemSpawningOptimization 规则按具体类否决的村民行为集合（MC 26.x，Mojang 名）。
 * 仅收录经 26.1.2 / 26.2 字节码核对确认：直接继承 Behavior（运行时类即源码类，
 * 由 VillagerGoalPackages 直接 new，非工厂 holder——工厂 holder 类禁止进入本名单，
 * 必须在各工厂标记 Mixin 中标记返回实例）、且村民的全部使用位置都在 CORE
 * （未被整表标记的活动）中的行为——IDLE / WORK / PLAY / MEET 内的具体行为已由
 * VillagerGoalPackages 构建层整表标记覆盖（含 JumpOnBed、VillagerMakeLove、
 * GiveGiftToHero、ShowTradesToPlayer，不再列入本名单形成重复否决路径）。
 * 与生成链相关的行为严禁加入本名单：VillagerPanicTrigger（恐慌期
 * spawnGolemIfNeeded(3)）、SleepInBed 等；gossip 链（TradeWithVillager 等）在
 * 本规则语义（现代恐吓式刷铁机）下由 IDLE / MEET 整表标记抑制，同样无需进入本名单。
 */
public final class IronGolemVillagerSkipClasses {

    /**
     * GoToPotentialJobSite：CORE p7，走向潜在工作站（职业链），与生成链无关。
     * LookAndFollowTradingPlayerSink：CORE p3，跟随 / 张望正在交易的玩家，与生成链无关。
     * 两个类名在 26.1.2 / 26.2 间稳定。
     */
    public static final Set<Class<?>> SKIP_CLASSES = Set.of(
            GoToPotentialJobSite.class,
            LookAndFollowTradingPlayerSink.class
    );

    private IronGolemVillagerSkipClasses() {
    }
}
