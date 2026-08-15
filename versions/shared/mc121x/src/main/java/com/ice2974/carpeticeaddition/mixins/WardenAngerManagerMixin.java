package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.WardenAngerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@Mixin(WardenAngerManager.class)
public abstract class WardenAngerManagerMixin {
    @Shadow
    @Final
    protected ArrayList<Entity> suspects;

    @Shadow
    @Final
    protected Object2IntMap<UUID> suspectUuidsToAngerLevel;

    @Shadow
    public abstract void removeSuspect(Entity entity);

    // 返回 0 而非当前值：调用方 WardenEntity.increaseAngerAt 用返回值计算 angriness，
    // 对玩家必须表现为"无愤怒"，否则可能触发对旧目标的遗忘判断等分支。
    @Inject(method = "increaseAngerAt", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$noPlayerAnger(Entity entity, int amount,
            CallbackInfoReturnable<Integer> cir) {
        if (CarpetIceAdditionSettings.wardenNotHostileToPlayers && entity instanceof PlayerEntity) {
            cir.setReturnValue(0);
        }
    }

    // HEAD 位于 tick 体内的 updateSuspectsMap 之前：已加载玩家的 UUID 条目会在被
    // 转换回实体表之前直接删除，转换路径对玩家不会发生。removeSuspect 会同步维护
    // 实体表与排序列表并重算 primeAnger；先快照再删除以避免并发修改。
    @Inject(method = "tick", at = @At("HEAD"))
    private void carpetIceAddition$clearPlayerAnger(ServerWorld world,
            Predicate<Entity> suspectPredicate, CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.wardenNotHostileToPlayers) {
            return;
        }
        for (Entity entity : List.copyOf(this.suspects)) {
            if (entity instanceof PlayerEntity) {
                this.removeSuspect(entity);
            }
        }
        for (UUID uuid : List.copyOf(this.suspectUuidsToAngerLevel.keySet())) {
            if (world.getEntity(uuid) instanceof PlayerEntity) {
                this.suspectUuidsToAngerLevel.remove(uuid);
            }
        }
    }
}
