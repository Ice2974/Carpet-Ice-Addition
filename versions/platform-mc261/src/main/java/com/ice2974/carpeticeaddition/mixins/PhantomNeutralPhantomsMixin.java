package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.NeutralPhantomsRetaliationTracker;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Phantom.class)
public abstract class PhantomNeutralPhantomsMixin extends Mob implements NeutralPhantomsRetaliationTracker {
    @Unique
    private UUID carpetIceAddition$neutralPhantomsTargetUuid;
    @Unique
    private int carpetIceAddition$neutralPhantomsTargetEntityId = -1;

    protected PhantomNeutralPhantomsMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void carpetIceAddition$recordNeutralPhantomsRetaliationTarget(ServerPlayer player) {
        this.carpetIceAddition$neutralPhantomsTargetUuid = player.getUUID();
        this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
        this.setTarget(player);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void carpetIceAddition$tickNeutralPhantomsRetaliation(CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.neutralPhantoms || this.carpetIceAddition$neutralPhantomsTargetUuid == null) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = this.getTargetUnchecked();
        if (target != null && target.isAlive()) {
            this.carpetIceAddition$neutralPhantomsTargetEntityId = target.getId();
            return;
        }

        this.setTarget(null);
        ServerPlayer player = serverLevel.getServer()
                .getPlayerList()
                .getPlayer(this.carpetIceAddition$neutralPhantomsTargetUuid);
        if (player != null && player.isAlive()) {
            if (player.getId() != this.carpetIceAddition$neutralPhantomsTargetEntityId
                    && carpetIceAddition$shouldForgiveDeadPlayers(serverLevel)) {
                this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
                return;
            }
            this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
            this.setTarget(player);
            return;
        }

        if (player != null && carpetIceAddition$shouldForgiveDeadPlayers(serverLevel)) {
            this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
            return;
        }

        if (target instanceof ServerPlayer && carpetIceAddition$shouldForgiveDeadPlayers(serverLevel)) {
            this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
        } else if (player != null) {
            this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
        }
    }

    @Unique
    private boolean carpetIceAddition$shouldForgiveDeadPlayers(ServerLevel serverLevel) {
        return Boolean.TRUE.equals(serverLevel.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS));
    }

    @Unique
    private void carpetIceAddition$clearNeutralPhantomsRetaliationTarget() {
        this.carpetIceAddition$neutralPhantomsTargetUuid = null;
        this.carpetIceAddition$neutralPhantomsTargetEntityId = -1;
    }
}
