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
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * enchantedGoldenAppleEffectRollback 规则（1.21.1，food 组件内嵌 effects 世代）。
 *
 * <p>1.21.1 的效果应用收口是私有 LivingEntity#addEatEffect(FoodProperties)（本身不含 ItemStack），
 * 其唯一调用点位于同时持有 ItemStack 与 FoodProperties 的三参
 * eat(Level, ItemStack, FoodProperties) 内。此处包装该调用：规则开启且食用物品是附魔金苹果时，
 * 只把传给 addEatEffect 的 FoodProperties 换成修正副本，其余原样 original。vanilla 的
 * effects 遍历 / 概率 / addEffect 以及 playSound、ItemStack.consume、GameEvent.EAT 等
 * 控制流保持不变；营养与饱和度结算不经过本副本，普通金苹果等其它食物不匹配。
 *
 * <p>1.21.1 的 FoodProperties.PossibleEffect 为 record(MobEffectInstance effect, float probability)，
 * effect() 直接返回实例（不存在 Supplier 语义），因此修正副本按组件重建 PossibleEffect，
 * 仅 Regeneration / Absorption 使用新的 duration / amplifier 实例。
 */
@Mixin(LivingEntity.class)
public abstract class EnchantedGoldenAppleEffectRollbackMixin {

    private static final int REGENERATION_DURATION_TICKS_1_8 = 600;
    private static final int REGENERATION_AMPLIFIER_1_8 = 4;
    private static final int ABSORPTION_DURATION_TICKS_1_8 = 2400;
    private static final int ABSORPTION_AMPLIFIER_1_8 = 0;

    @WrapOperation(
            method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;addEatEffect(Lnet/minecraft/world/food/FoodProperties;)V"
            )
    )
    private void carpetIceAddition$rollbackEnchantedGoldenAppleEffects(
            LivingEntity livingEntity, FoodProperties foodProperties, Operation<Void> original,
            Level level, ItemStack stack) {
        FoodProperties toApply = foodProperties;
        if (CarpetIceAdditionSettings.enchantedGoldenAppleEffectRollback
                && stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
            try {
                toApply = patched(foodProperties);
            } catch (Throwable throwable) {
                CarpetIceAdditionMod.reportFeatureCompatibilityIssue("enchantedGoldenAppleEffectRollback", throwable);
                toApply = foodProperties;
            }
        }
        original.call(livingEntity, toApply);
    }

    /**
     * 复制原 FoodProperties 的全部字段；effects 中 Regeneration / Absorption 换成只改
     * duration / amplifier 的新实例，其它 PossibleEffect 原样保留。
     */
    private static FoodProperties patched(FoodProperties foodProperties) {
        List<FoodProperties.PossibleEffect> patchedEffects = foodProperties.effects().stream()
                .map(EnchantedGoldenAppleEffectRollbackMixin::patchEffect)
                .toList();
        return new FoodProperties(
                foodProperties.nutrition(),
                foodProperties.saturation(),
                foodProperties.canAlwaysEat(),
                foodProperties.eatSeconds(),
                foodProperties.usingConvertsTo(),
                patchedEffects);
    }

    private static FoodProperties.PossibleEffect patchEffect(FoodProperties.PossibleEffect possibleEffect) {
        MobEffectInstance effect = possibleEffect.effect();
        if (effect != null) {
            Holder<MobEffect> holder = effect.getEffect();
            if (holder != null
                    && (holder.value() == MobEffects.REGENERATION.value()
                    || holder.value() == MobEffects.ABSORPTION.value())) {
                return new FoodProperties.PossibleEffect(patchInstance(effect), possibleEffect.probability());
            }
        }
        return possibleEffect;
    }

    /**
     * 只修改 Regeneration / Absorption 的 duration / amplifier，保留原实例其余可观察属性；
     * 其它效果与 null 原样返回。新实例保留可经公开 API 取得的 ambient / visible / showIcon
     * （hiddenEffect 为私有内部字段，食物效果场景不参与复制）。
     */
    private static MobEffectInstance patchInstance(MobEffectInstance instance) {
        if (instance == null) {
            return null;
        }
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
