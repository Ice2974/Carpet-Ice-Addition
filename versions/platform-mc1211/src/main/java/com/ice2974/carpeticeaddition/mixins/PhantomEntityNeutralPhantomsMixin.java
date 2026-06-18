package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.NeutralPhantomsRetaliationTracker;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class PhantomEntityNeutralPhantomsMixin implements NeutralPhantomsRetaliationTracker {
    @Unique
    private UUID carpetIceAddition$neutralPhantomsTargetUuid;
    @Unique
    private int carpetIceAddition$neutralPhantomsTargetEntityId = -1;

    @Shadow
    public abstract LivingEntity getTarget();

    @Shadow
    public abstract void setTarget(LivingEntity target);

    @Override
    public void carpetIceAddition$recordNeutralPhantomsRetaliationTarget(ServerPlayerEntity player) {
        this.carpetIceAddition$neutralPhantomsTargetUuid = player.getUuid();
        this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
        this.setTarget(player);
    }

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void carpetIceAddition$tickNeutralPhantomsRetaliation(CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.neutralPhantoms || this.carpetIceAddition$neutralPhantomsTargetUuid == null) {
            return;
        }
        if (!((Object) this instanceof PhantomEntity)) {
            return;
        }

        MobEntity mob = (MobEntity) (Object) this;
        if (!(mob.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            this.carpetIceAddition$neutralPhantomsTargetEntityId = target.getId();
            return;
        }

        this.setTarget(null);
        ServerPlayerEntity player = serverWorld.getServer()
                .getPlayerManager()
                .getPlayer(this.carpetIceAddition$neutralPhantomsTargetUuid);
        if (player != null && player.isAlive()) {
            if (player.getId() != this.carpetIceAddition$neutralPhantomsTargetEntityId
                    && serverWorld.getGameRules().getBoolean(GameRules.FORGIVE_DEAD_PLAYERS)) {
                this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
                return;
            }
            this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
            this.setTarget(player);
            return;
        }

        if (player != null && serverWorld.getGameRules().getBoolean(GameRules.FORGIVE_DEAD_PLAYERS)) {
            this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
            return;
        }

        if (target instanceof ServerPlayerEntity && serverWorld.getGameRules().getBoolean(GameRules.FORGIVE_DEAD_PLAYERS)) {
            this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
        } else if (player != null) {
            this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
        }
    }

    @Unique
    private void carpetIceAddition$clearNeutralPhantomsRetaliationTarget() {
        this.carpetIceAddition$neutralPhantomsTargetUuid = null;
        this.carpetIceAddition$neutralPhantomsTargetEntityId = -1;
    }
}
