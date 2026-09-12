package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.BedrockImpalingPortHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * bedrockImpalingPort 规则的注入点。
 *
 * <p>规则语义见 {@link BedrockImpalingPortHelper}。之所以选
 * {@code Enchantment#modifyDamageFilteredValue} 内部对
 * {@code Enchantment#getEffects(DataComponentType)} 的唯一调用点：
 *
 * <ul>
 *   <li>该方法的 descriptor 在全部受支持版本上完全一致；</li>
 *   <li>其内部 {@code getEffects} 只在方法开头调用一次，返回值直接作为后续
 *       {@code applyEffects} 的第一个实参，因此替换该返回值即可精确控制
 *       「哪些 ConditionalEffect 参与本次伤害计算」；</li>
 *   <li>{@code componentType} 与被打目标实体都在同一作用域内可取得，无需额外状态；</li>
 *   <li>相比 {@code ConditionalEffect#matches} 全局入口，本切入点只覆盖
 *       {@code modifyDamage} 路径，且天然排斥其它附魔与其它调用方。</li>
 * </ul>
 *
 * <p>handler 在 {@code Operation} 之后按外层方法的形参顺序完整捕获其全部显式形参前缀。
 * 这是为了在 MixinExtras 的两种可能参数匹配规则下都得到同一绑定：外层方法各形参类型
 * （{@code DataComponentType, ServerLevel, int, ItemStack, Entity, DamageSource,
 * MutableFloat}）互不相同且顺序一致，因此「严格位置前缀匹配」与「按类型顺序匹配」
 * 会给出相同结果。
 *
 * <p>{@code original.call(...)} 在正常路径上恰好执行一次，且位于 try 之外：本模组自身的
 * 判定异常由 helper 内部捕获并去重上报，而原版或其它 Mixin 在 {@code getEffects} 中抛出的
 * 异常不会被吞掉。
 */
@Mixin(Enchantment.class)
public abstract class BedrockImpalingPortMixin {

    @WrapOperation(
            method = "modifyDamageFilteredValue",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;getEffects(Lnet/minecraft/core/component/DataComponentType;)Ljava/util/List;"
            )
    )
    private List<?> carpetIceAddition$bedrockImpalingPort(
            Enchantment self,
            DataComponentType<?> componentType,
            Operation<List<?>> original,
            DataComponentType<?> enclosingComponentType,
            ServerLevel level,
            int enchantLevel,
            ItemStack stack,
            Entity target,
            DamageSource source,
            MutableFloat value) {
        List<?> originalEffects = original.call(self, componentType);
        return BedrockImpalingPortHelper.resolve(originalEffects, self, componentType, level, target);
    }
}
