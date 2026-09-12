package com.ice2974.carpeticeaddition.mixins;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * bedrockTridentPort 规则对 AbstractArrow 继承成员的只读访问。
 *
 * <p>ThrownTridentBedrockTridentPortMixin 以 ThrownTrident 为目标类，而 Mixin 的
 * {@code @Shadow} 只解析目标类自身声明的成员；inGroundTime / shakeTime 字段与
 * isNoPhysics() 方法均声明于父类 AbstractArrow（已核实全部受支持版本同名同语义），
 * 其中 inGroundTime 为 protected 且跨包不可直接访问，故统一经本接口只读取得。
 * 本接口不提供任何写入方法，不改变任何原版字段。
 *
 * <p>AbstractArrow 在 1.21.11 起移入 projectile.arrow 子包（与 ThrownTrident 同一条
 * 边），类移动由 versions/mapping-1.21.11-1.21.10.txt 统一下推。
 */
@Mixin(AbstractArrow.class)
public interface AbstractArrowBedrockTridentPortAccessor {

    @Accessor("inGroundTime")
    int carpetIceAddition$getInGroundTime();

    @Accessor("shakeTime")
    int carpetIceAddition$getShakeTime();

    @Invoker("isNoPhysics")
    boolean carpetIceAddition$isNoPhysics();
}
