package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.NeutralPhantomsRetaliationTracker;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Phantom.class)
public abstract class PhantomNeutralPhantomsMixin extends Mob implements NeutralMob, NeutralPhantomsRetaliationTracker {
    @Unique
    private EntityReference<LivingEntity> carpetIceAddition$neutralPhantomsTarget;
    @Unique
    private long carpetIceAddition$neutralPhantomsAngerEndTime = -1L;

    protected PhantomNeutralPhantomsMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void carpetIceAddition$recordNeutralPhantomsRetaliationTarget(ServerPlayer player) {
        this.carpetIceAddition$neutralPhantomsTarget = EntityReference.of(player.getUUID());
        this.setTarget(player);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void carpetIceAddition$tickNeutralPhantomsRetaliation(CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.neutralPhantoms || this.carpetIceAddition$neutralPhantomsTarget == null) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = this.getTargetUnchecked();
        if (target instanceof ServerPlayer player && this.carpetIceAddition$neutralPhantomsTarget.matches(player)) {
            if (player.isAlive()) {
                return;
            }
            this.setTarget(null);
            if (carpetIceAddition$shouldForgiveDeadPlayers(serverLevel)) {
                this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
            }
            return;
        }

        ServerPlayer player = serverLevel.getServer()
                .getPlayerList()
                .getPlayer(this.carpetIceAddition$neutralPhantomsTarget.getUUID());
        if (player != null && player.isAlive()) {
            this.setTarget(player);
            return;
        }

        this.setTarget(null);
        if (player != null && carpetIceAddition$shouldForgiveDeadPlayers(serverLevel)) {
            this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
        }
    }

    @Override
    public void playerDied(ServerLevel serverLevel, Player player) {
        if (!CarpetIceAdditionSettings.neutralPhantoms || this.carpetIceAddition$neutralPhantomsTarget == null) {
            return;
        }
        if (!this.carpetIceAddition$neutralPhantomsTarget.matches(player)) {
            return;
        }

        this.setTarget(null);
        if (carpetIceAddition$shouldForgiveDeadPlayers(serverLevel)) {
            this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
        }
    }

    @Override
    public long getPersistentAngerEndTime() {
        return this.carpetIceAddition$neutralPhantomsAngerEndTime;
    }

    @Override
    public void setPersistentAngerEndTime(long angerEndTime) {
        this.carpetIceAddition$neutralPhantomsAngerEndTime = angerEndTime;
    }

    @Override
    public EntityReference<LivingEntity> getPersistentAngerTarget() {
        return this.carpetIceAddition$neutralPhantomsTarget;
    }

    @Override
    public void setPersistentAngerTarget(EntityReference<LivingEntity> target) {
        this.carpetIceAddition$neutralPhantomsTarget = target;
    }

    @Override
    public void startPersistentAngerTimer() {
    }

    @Unique
    private boolean carpetIceAddition$shouldForgiveDeadPlayers(ServerLevel serverLevel) {
        return Boolean.TRUE.equals(serverLevel.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS));
    }

    @Unique
    private void carpetIceAddition$clearNeutralPhantomsRetaliationTarget() {
        this.carpetIceAddition$neutralPhantomsTarget = null;
        this.carpetIceAddition$neutralPhantomsAngerEndTime = -1L;
    }
}
