package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.NeutralPhantomsRetaliationTracker;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.nbt.NbtCompound;
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
    private static final String CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_UUID_KEY =
            "carpet_ice_addition.neutral_phantoms_target_uuid";
    @Unique
    private static final String CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_ENTITY_ID_KEY =
            "carpet_ice_addition.neutral_phantoms_target_entity_id";

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

        try {
            boolean forgiveDeadPlayers = serverWorld.getGameRules().getBoolean(GameRules.FORGIVE_DEAD_PLAYERS);

            LivingEntity target = this.getTarget();
            if (target instanceof ServerPlayerEntity targetPlayer
                    && targetPlayer.getUuid().equals(this.carpetIceAddition$neutralPhantomsTargetUuid)) {
                if (!targetPlayer.isAlive() || targetPlayer.isRemoved()) {
                    this.setTarget(null);
                    if (forgiveDeadPlayers) {
                        this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
                    } else {
                        this.carpetIceAddition$neutralPhantomsTargetEntityId = -1;
                    }
                    return;
                }
                if (targetPlayer.getWorld() != serverWorld) {
                    this.setTarget(null);
                    return;
                }
                if (forgiveDeadPlayers
                        && this.carpetIceAddition$neutralPhantomsTargetEntityId != -1
                        && targetPlayer.getId() != this.carpetIceAddition$neutralPhantomsTargetEntityId) {
                    this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
                    this.setTarget(null);
                    return;
                }
                this.carpetIceAddition$neutralPhantomsTargetEntityId = targetPlayer.getId();
                return;
            }

            if (target != null) {
                this.setTarget(null);
            }

            ServerPlayerEntity player = serverWorld.getServer()
                    .getPlayerManager()
                    .getPlayer(this.carpetIceAddition$neutralPhantomsTargetUuid);
            if (player == null) {
                return;
            }
            if (!player.isAlive() || player.isRemoved()) {
                if (forgiveDeadPlayers) {
                    this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
                } else {
                    this.carpetIceAddition$neutralPhantomsTargetEntityId = -1;
                }
                return;
            }
            if (player.getWorld() != serverWorld) {
                return;
            }
            if (forgiveDeadPlayers
                    && this.carpetIceAddition$neutralPhantomsTargetEntityId != -1
                    && player.getId() != this.carpetIceAddition$neutralPhantomsTargetEntityId) {
                this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
                return;
            }

            this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
            this.setTarget(player);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("neutralPhantoms", throwable);
        }
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void carpetIceAddition$writeNeutralPhantomsRetaliation(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof PhantomEntity)) {
            return;
        }
        if (this.carpetIceAddition$neutralPhantomsTargetUuid == null) {
            return;
        }
        nbt.putString(
                CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_UUID_KEY,
                this.carpetIceAddition$neutralPhantomsTargetUuid.toString());
        nbt.putInt(
                CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_ENTITY_ID_KEY,
                this.carpetIceAddition$neutralPhantomsTargetEntityId);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void carpetIceAddition$readNeutralPhantomsRetaliation(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof PhantomEntity)) {
            return;
        }
        if (!nbt.contains(CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_UUID_KEY)) {
            return;
        }
        try {
            this.carpetIceAddition$neutralPhantomsTargetUuid =
                    UUID.fromString(nbt.getString(CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_UUID_KEY));
            this.carpetIceAddition$neutralPhantomsTargetEntityId = nbt.contains(
                    CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_ENTITY_ID_KEY)
                    ? nbt.getInt(CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_ENTITY_ID_KEY)
                    : -1;
        } catch (IllegalArgumentException exception) {
            this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
        }
    }

    @Unique
    private void carpetIceAddition$clearNeutralPhantomsRetaliationTarget() {
        this.carpetIceAddition$neutralPhantomsTargetUuid = null;
        this.carpetIceAddition$neutralPhantomsTargetEntityId = -1;
    }
}
