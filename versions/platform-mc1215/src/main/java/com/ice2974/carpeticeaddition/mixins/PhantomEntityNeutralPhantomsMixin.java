package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.NeutralPhantomsRetaliationTracker;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
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
    public void carpetIceAddition$recordNeutralPhantomsRetaliationTarget(ServerPlayer player) {
        this.carpetIceAddition$neutralPhantomsTargetUuid = player.getUUID();
        this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
        this.setTarget(player);
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void carpetIceAddition$tickNeutralPhantomsRetaliation(ServerLevel serverWorld, CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.neutralPhantoms) {
            return;
        }
        if (!((Object) this instanceof Phantom)) {
            return;
        }

        try {
            // 没有反击目标时，清除通过原版 FindTargetGoal 锁定的普通玩家 target，
            // 使规则开启后已经索敌玩家的幻翼在下一 tick 变中立；不影响非玩家 target。
            if (this.carpetIceAddition$neutralPhantomsTargetUuid == null) {
                if (this.getTarget() instanceof ServerPlayer) {
                    this.setTarget(null);
                }
                return;
            }
            boolean forgiveDeadPlayers = serverWorld.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS);

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
                if (targetPlayer.level() != serverWorld) {
                    this.setTarget(null);
                    return;
                }
                this.carpetIceAddition$neutralPhantomsTargetEntityId = targetPlayer.getId();
                return;
            }

            if (target != null) {
                this.setTarget(null);
            }

            ServerPlayer player = serverWorld.getServer()
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
            if (player.level() != serverWorld) {
                return;
            }

            this.carpetIceAddition$neutralPhantomsTargetEntityId = player.getId();
            this.setTarget(player);
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("neutralPhantoms", throwable);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void carpetIceAddition$writeNeutralPhantomsRetaliation(CompoundTag nbt, CallbackInfo ci) {
        if (!((Object) this instanceof Phantom)) {
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

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void carpetIceAddition$readNeutralPhantomsRetaliation(CompoundTag nbt, CallbackInfo ci) {
        if (!((Object) this instanceof Phantom)) {
            return;
        }
        String uuidString = nbt.getStringOr(CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_UUID_KEY, "");
        if (uuidString.isEmpty()) {
            return;
        }
        try {
            this.carpetIceAddition$neutralPhantomsTargetUuid = UUID.fromString(uuidString);
            this.carpetIceAddition$neutralPhantomsTargetEntityId =
                    nbt.getIntOr(CARPET_ICE_ADDITION$NEUTRAL_PHANTOMS_TARGET_ENTITY_ID_KEY, -1);
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
