package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WardenEntity.class)
public abstract class WardenEntityMixin {
    @Shadow
    public abstract Brain<WardenEntity> getBrain();

    // tick() HEAD 位于振动 ticker、brain.tick、angerManagement.tick 与活动切换之前：
    // 规则开启前已存在的玩家攻击目标在这里被主动解除，不依赖 StopAttackingIfTargetInvalid
    // 的被动遗忘。ROAR_TARGET 同步清理，避免残留的咆哮目标继续驱动 ROAR 活动。
    // world 访问器仅由 Entity 侧声明、WardenEntity 未重写，@Shadow 无法解析继承成员，
    // 必须经由 Entity 强转调用；1.21.9+ yarn 中该访问器更名为 getEntityWorld()。
    @Inject(method = "tick", at = @At("HEAD"))
    private void carpetIceAddition$forgetPlayerTargets(CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.wardenNotHostileToPlayers || ((Entity) (Object) this).getEntityWorld().isClient()) {
            return;
        }
        Brain<WardenEntity> brain = this.getBrain();
        if (brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) instanceof PlayerEntity) {
            brain.forget(MemoryModuleType.ATTACK_TARGET);
        }
        if (brain.getOptionalRegisteredMemory(MemoryModuleType.ROAR_TARGET).orElse(null) instanceof PlayerEntity) {
            brain.forget(MemoryModuleType.ROAR_TARGET);
        }
    }

    // ATTACK_TARGET 的唯一写入点（受伤直设与咆哮收尾都经过这里）：
    // 取消后玩家无法经任何 vanilla 路径成为攻击目标。
    @Inject(method = "updateAttackTarget", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$noPlayerTarget(LivingEntity target, CallbackInfo ci) {
        if (CarpetIceAdditionSettings.wardenNotHostileToPlayers && target instanceof PlayerEntity) {
            ci.cancel();
        }
    }
}
