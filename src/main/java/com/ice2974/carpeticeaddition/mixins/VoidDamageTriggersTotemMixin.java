package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class VoidDamageTriggersTotemMixin {

    @WrapOperation(
            method = "checkTotemDeathProtection",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"
            )
    )
    private boolean carpetIceAddition$allowVoidDamageToTriggerTotem(
            DamageSource source,
            TagKey<DamageType> tag,
            Operation<Boolean> original
    ) {
        if (CarpetIceAdditionSettings.voidDamageTriggersTotem
                && tag == DamageTypeTags.BYPASSES_INVULNERABILITY
                && source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return false;
        }
        return original.call(source, tag);
    }
}
