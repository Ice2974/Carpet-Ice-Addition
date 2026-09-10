package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * enchantedGoldenAppleEffectRollback 规则（1.21.3+，minecraft:consumable / consume effects 世代）。
 *
 * <p>附魔金苹果的食用效果由 ApplyStatusEffectsConsumeEffect#apply 承载。只包装该方法内部对
 * LivingEntity#addEffect(MobEffectInstance) 的实际调用：规则开启且食用物品是附魔金苹果时，
 * 仅把 Regeneration / Absorption 的 duration / amplifier 修正为 Java Edition 1.8.x 值
 * （Regeneration V 600 刻 / amplifier 4，Absorption I 2400 刻 / amplifier 0），其余效果
 * （Resistance、Fire Resistance 及自定义效果）与规则关闭、非附魔金苹果路径一样原实例
 * original 放行；vanilla 的 probability、循环、addEffect 返回值与 apply 返回 boolean
 * 及全部控制流保持不变。
 *
 * <p>本文件以 main 态（1.21.11）书写并覆盖 1.21.3～26.2 平台（1.21.1 版本见 1.21.1 平台
 * override）；若 26.x 官方映射下目标类 / 方法发生结构性分叉，按仓库规范另置 26.x 平台
 * override，不使用复杂宏。
 */
@Mixin(ApplyStatusEffectsConsumeEffect.class)
public abstract class EnchantedGoldenAppleEffectRollbackMixin {

    private static final int REGENERATION_DURATION_TICKS_1_8 = 600;
    private static final int REGENERATION_AMPLIFIER_1_8 = 4;
    private static final int ABSORPTION_DURATION_TICKS_1_8 = 2400;
    private static final int ABSORPTION_AMPLIFIER_1_8 = 0;

    @WrapOperation(
            method = "apply(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"
            )
    )
    private boolean carpetIceAddition$rollbackEnchantedGoldenAppleEffect(
            LivingEntity entity, MobEffectInstance instance, Operation<Boolean> original,
            Level level, ItemStack stack) {
        MobEffectInstance toApply = instance;
        if (CarpetIceAdditionSettings.enchantedGoldenAppleEffectRollback
                && stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            try {
                toApply = patch1_8(instance);
            } catch (Throwable throwable) {
                CarpetIceAdditionMod.reportFeatureCompatibilityIssue("enchantedGoldenAppleEffectRollback", throwable);
                toApply = instance;
            }
        }
        return original.call(entity, toApply);
    }

    /**
     * 只修改 Regeneration / Absorption 的 duration / amplifier，保留原实例其余可观察属性；
     * 其它效果返回原实例。新实例仅保留 duration / amplifier 之外可经公开 API 取得的
     * ambient / visible / showIcon（食物效果原型的 hiddenEffect 恒为 null，不参与复制）。
     */
    private static MobEffectInstance patch1_8(MobEffectInstance instance) {
        Holder<MobEffect> effect = instance.getEffect();
        if (effect.value() == MobEffects.REGENERATION.value()) {
            return new MobEffectInstance(effect, REGENERATION_DURATION_TICKS_1_8, REGENERATION_AMPLIFIER_1_8,
                    instance.isAmbient(), instance.isVisible(), instance.showIcon());
        }
        if (effect.value() == MobEffects.ABSORPTION.value()) {
            return new MobEffectInstance(effect, ABSORPTION_DURATION_TICKS_1_8, ABSORPTION_AMPLIFIER_1_8,
                    instance.isAmbient(), instance.isVisible(), instance.showIcon());
        }
        return instance;
    }
}
