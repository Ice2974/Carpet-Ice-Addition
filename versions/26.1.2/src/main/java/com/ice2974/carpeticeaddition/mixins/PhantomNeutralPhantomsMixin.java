package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.NeutralPhantomsRetaliationTracker;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class PhantomNeutralPhantomsMixin implements NeutralPhantomsRetaliationTracker {
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
    public abstract LivingEntity getTargetUnchecked();

    @Shadow
    public abstract void setTarget(LivingEntity target);

    @Override
    public void carpetIceAddition$recordNeutralPhantomsRetaliationTarget(ServerPlayer player) {
        this.carpetIceAddition$neutralPhantomsTargetUuid = player.getUUID();
        this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
        this.setTarget(player);
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"))
    private void carpetIceAddition$tickNeutralPhantomsRetaliation(CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.neutralPhantoms) {
            return;
        }
        if (!((Object) this instanceof Phantom phantom)) {
            return;
        }
        try {
            // 没有反击目标时，清除通过原版 PhantomAttackPlayerTargetGoal 锁定的普通玩家 target，
            // 使规则开启后已经索敌玩家的幻翼在下一 tick 变中立；不影响非玩家 target。
            if (this.carpetIceAddition$neutralPhantomsTargetUuid == null) {
                if (this.getTargetUnchecked() instanceof ServerPlayer) {
                    this.setTarget(null);
                }
                return;
            }
            ServerLevel serverLevel = (ServerLevel) phantom.level();
            boolean forgiveDeadPlayers = carpetIceAddition$shouldForgiveDeadPlayers(serverLevel);

            LivingEntity target = this.getTarget();
            if (target instanceof ServerPlayer targetPlayer
                    && targetPlayer.getUUID().equals(this.carpetIceAddition$neutralPhantomsTargetUuid)) {
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
                if (targetPlayer.level() != serverLevel) {
                    this.setTarget(null);
                    return;
                }
                this.carpetIceAddition$neutralPhantomsTargetEntityId = targetPlayer.getId();
                return;
            }

            LivingEntity uncheckedTarget = this.getTargetUnchecked();
            if (uncheckedTarget != null) {
                this.setTarget(null);
            }

            ServerPlayer player = serverLevel.getServer()
                    .getPlayerList()
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
            if (player.level() != serverLevel) {
                return;
            }

            this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
            this.setTarget(player);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("neutralPhantoms", throwable);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void carpetIceAddition$writeNeutralPhantomsRetaliation(ValueOutput output, CallbackInfo ci) {
        if (!((Object) this instanceof Phantom)) {
            return;
        }
        if (this.carpetIceAddition$neutralPhantomsTargetUuid == null) {
            return;
        }

        output.putString(
                CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_UUID_KEY,
                this.carpetIceAddition$neutralPhantomsTargetUuid.toString()
        );
        output.putInt(
                CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_ENTITY_ID_KEY,
                this.carpetIceAddition$neutralPhantomsTargetEntityId
        );
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void carpetIceAddition$readNeutralPhantomsRetaliation(ValueInput input, CallbackInfo ci) {
        if (!((Object) this instanceof Phantom)) {
            return;
        }

        input.getString(CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_UUID_KEY).ifPresent(uuidString -> {
            try {
                this.carpetIceAddition$neutralPhantomsTargetUuid = UUID.fromString(uuidString);
                this.carpetIceAddition$neutralPhantomsTargetEntityId = input.getIntOr(
                        CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_ENTITY_ID_KEY,
                        -1
                );
            } catch (IllegalArgumentException exception) {
                this.carpetIceAddition$clearNeutralPhantomsRetaliationTarget();
            }
        });
    }

    @Unique
    private boolean carpetIceAddition$shouldForgiveDeadPlayers(ServerLevel serverLevel) {
        return serverLevel.getGameRules().get(GameRules.FORGIVE_DEAD_PLAYERS);
    }

    @Unique
    private void carpetIceAddition$clearNeutralPhantomsRetaliationTarget() {
        this.carpetIceAddition$neutralPhantomsTargetUuid = null;
        this.carpetIceAddition$neutralPhantomsTargetEntityId = -1;
    }
}
