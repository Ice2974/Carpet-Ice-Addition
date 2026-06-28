package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.NeutralPhantomsRetaliationTracker;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.rule.GameRules;
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
    private void carpetIceAddition$tickNeutralPhantomsRetaliation(ServerWorld serverWorld, CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.neutralPhantoms) {
            return;
        }
        if (!((Object) this instanceof PhantomEntity)) {
            return;
        }

        try {
            // 没有反击目标时，清除通过原版 FindTargetGoal 锁定的普通玩家 target，
            // 使规则开启后已经索敌玩家的幻翼在下一 tick 变中立；不影响非玩家 target。
            if (this.carpetIceAddition$neutralPhantomsTargetUuid == null) {
                if (this.getTarget() instanceof ServerPlayerEntity) {
                    this.setTarget(null);
                }
                return;
            }
            boolean forgiveDeadPlayers = Boolean.TRUE.equals(
                    serverWorld.getGameRules().getValue(GameRules.FORGIVE_DEAD_PLAYERS));

            LivingEntity target = this.getTarget();
            if (target instanceof ServerPlayerEntity targetPlayer
                    && targetPlayer.getUuid().equals(this.carpetIceAddition$neutralPhantomsTargetUuid)) {
                if (!targetPlayer.isAlive()) {
                    this.setTarget(null);
                    if (forgiveDeadPlayers) {
                        this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
                    } else {
                        this.carpetIceAddition$neutralPhantomsTargetEntityId = -1;
                    }
                    return;
                }
                if (targetPlayer.isRemoved()) {
                    this.setTarget(null);
                    return;
                }
                if (targetPlayer.getEntityWorld() != serverWorld) {
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
            if (!player.isAlive()) {
                if (forgiveDeadPlayers) {
                    this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
                } else {
                    this.carpetIceAddition$neutralPhantomsTargetEntityId = -1;
                }
                return;
            }
            if (player.isRemoved()) {
                return;
            }
            if (player.getEntityWorld() != serverWorld) {
                return;
            }

            this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
            this.setTarget(player);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("neutralPhantoms", throwable);
        }
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void carpetIceAddition$writeNeutralPhantomsRetaliation(WriteView view, CallbackInfo ci) {
        if (!((Object) this instanceof PhantomEntity)) {
            return;
        }
        if (this.carpetIceAddition$neutralPhantomsTargetUuid == null) {
            return;
        }
        view.putString(
                CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_UUID_KEY,
                this.carpetIceAddition$neutralPhantomsTargetUuid.toString());
        view.putInt(
                CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_ENTITY_ID_KEY,
                this.carpetIceAddition$neutralPhantomsTargetEntityId);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void carpetIceAddition$readNeutralPhantomsRetaliation(ReadView view, CallbackInfo ci) {
        if (!((Object) this instanceof PhantomEntity)) {
            return;
        }
        String uuidString = view.getString(CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_UUID_KEY, "");
        if (uuidString.isEmpty()) {
            return;
        }
        try {
            this.carpetIceAddition$neutralPhantomsTargetUuid = UUID.fromString(uuidString);
            this.carpetIceAddition$neutralPhantomsTargetEntityId =
                    view.getInt(CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_ENTITY_ID_KEY, -1);
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
