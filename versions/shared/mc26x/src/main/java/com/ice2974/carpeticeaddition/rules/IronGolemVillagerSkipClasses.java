package com.ice2974.carpeticeaddition.rules;

import java.util.Set;

import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.ai.behavior.GoToPotentialJobSite;
import net.minecraft.world.entity.ai.behavior.JumpOnBed;
import net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;

/**
 * ironGolemSpawningOptimization 规则按具体类否决的村民行为集合（MC 26.x，Mojang 名）。
 * 仅收录经 26.1.2 / 26.2 字节码核对确认：直接继承 Behavior（运行时类即源码类，
 * 由 VillagerGoalPackages 直接 new，非工厂 holder——工厂 holder 类禁止进入本名单，
 * 必须在各工厂标记 Mixin 中标记返回实例）、
 * 且不出现在铁傀儡生成依赖链（CORE / REST / PANIC / MEET / IDLE 保留部分 / RAID / HIDE）中的行为。
 * TradeWithVillager 是 26.x 的闲聊生成触发（Villager#gossip → spawnGolemIfNeeded(5)），
 * 严禁加入本名单；VillagerPanicTrigger（恐慌期 spawnGolemIfNeeded(3)）、SleepInBed
 * 等生成链行为同样严禁加入。
 */
public final class IronGolemVillagerSkipClasses {

    /**
     * JumpOnBed：跳床玩闹（IDLE/PLAY），非 SleepInBed 的实际睡眠。
     * VillagerMakeLove：繁殖（IDLE）。
     * GiveGiftToHero：英雄村庄赠礼（WORK/MEET/IDLE）。
     * ShowTradesToPlayer：向玩家展示交易物品（WORK/MEET/IDLE），交易本身由玩家交互驱动。
     * GoToPotentialJobSite：走向潜在工作站（CORE 职业链），与生成链无关；
     * 类名在 26.1.2 / 26.2 间稳定。
     */
    public static final Set<Class<?>> SKIP_CLASSES = Set.of(
            JumpOnBed.class,
            VillagerMakeLove.class,
            GiveGiftToHero.class,
            ShowTradesToPlayer.class,
            GoToPotentialJobSite.class
    );

    private IronGolemVillagerSkipClasses() {
    }
}
