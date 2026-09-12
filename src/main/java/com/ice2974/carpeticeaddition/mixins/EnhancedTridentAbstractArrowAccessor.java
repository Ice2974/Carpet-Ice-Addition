package com.ice2974.carpeticeaddition.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * enhancedTrident 规则对 {@code AbstractArrow#canHitEntity} 的调用入口。
 *
 * <p>扫掠候选过滤必须与 vanilla {@code AbstractArrow#findHitEntity} 使用的谓词完全
 * 一致（含 owner 不可命中窗口等），该方法为 protected 且声明于 AbstractArrow
 * （Mixin 的 @Shadow 不解析父类成员，ThrownTrident 目标类自身未声明它），故经本
 * 接口以 @Invoker 取得。已核实全部受支持版本同名同语义。
 *
 * <p>AbstractArrow 在 1.21.11 起移入 projectile.arrow 子包（与 ThrownTrident 同一条
 * 边），类移动由 versions/mapping-1.21.11-1.21.10.txt 的显式条目沿版本图下推。
 */
@Mixin(AbstractArrow.class)
public interface EnhancedTridentAbstractArrowAccessor {

    @Invoker("canHitEntity")
    boolean carpetIceAddition$canHitEntity(Entity entity);
}
