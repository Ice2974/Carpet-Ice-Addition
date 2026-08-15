package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.entity.player.Player;
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

@Mixin(AngerManagement.class)
public abstract class WardenAngerManagerMixin {
    @Shadow
    @Final
    protected ArrayList<Entity> suspects;

    @Shadow
    @Final
    protected Object2IntMap<UUID> angerByUuid;

    @Shadow
    public abstract void clearAnger(Entity entity);

    // 返回 0 而非当前值：调用方 Warden.increaseAngerAt 用返回值计算 anger level，
    // 对玩家必须表现为"无愤怒"，否则可能触发对旧目标的遗忘判断等分支。
    @Inject(method = "increaseAnger", at = @At("HEAD"), cancellable = true)
    private void carpetIceAddition$noPlayerAnger(Entity entity, int amount,
            CallbackInfoReturnable<Integer> cir) {
        if (CarpetIceAdditionSettings.wardenNotHostileToPlayers && entity instanceof Player) {
            cir.setReturnValue(0);
        }
    }

    // HEAD 位于 tick 体内的 convertFromUuids 之前：已加载玩家的 UUID 条目会在被
    // 转换回实体表之前直接删除，转换路径对玩家不会发生。clearAnger 会同步维护
    // 实体表与排序列表并重算最高愤怒值；先快照再删除以避免并发修改。
    @Inject(method = "tick", at = @At("HEAD"))
    private void carpetIceAddition$clearPlayerAnger(ServerLevel level,
            Predicate<Entity> suspectFilter, CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.wardenNotHostileToPlayers) {
            return;
        }
        for (Entity entity : List.copyOf(this.suspects)) {
            if (entity instanceof Player) {
                this.clearAnger(entity);
            }
        }
        for (UUID uuid : List.copyOf(this.angerByUuid.keySet())) {
            if (level.getEntity(uuid) instanceof Player) {
                this.angerByUuid.remove(uuid);
            }
        }
    }
}
