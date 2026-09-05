package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.IronGolemVillagerOptimizationHooks;
import net.minecraft.world.entity.ai.behavior.GoToPotentialJobSite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ironGolemSpawningOptimization 规则（MC 1.21.1）：WalkTowardJobSiteTask 构造期自标记。
 * 这是 CORE 职业链中唯一的“具体 MultiTickTask 子类”（由 VillagerTaskListProvider 直接 new），
 * 因此在自身构造器尾部自标记（等价于类名单效果，但类名跨版本漂移，无法并入共享 SkipClasses）。
 * 语义为走向潜在工作站，与铁傀儡生成链无关。WalkTowardJobSiteTask 是 1.21.1 的 Yarn 类名，
 * 1.21.3 起更名为 WalkTowardsJobSiteTask（见 mc1213-12111 档）。
 */
@Mixin(GoToPotentialJobSite.class)
public abstract class WalkTowardJobSiteTaskIronGolemOptimizationMixin {

    @Inject(method = "<init>(F)V", at = @At("TAIL"))
    private void carpetIceAddition$markSelfForIronGolemOptimization(float speed, CallbackInfo ci) {
        IronGolemVillagerOptimizationHooks.markTaskInstance(this, "WalkTowardJobSiteTask");
    }
}
