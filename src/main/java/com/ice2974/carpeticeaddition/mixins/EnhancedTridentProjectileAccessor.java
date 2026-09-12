package com.ice2974.carpeticeaddition.mixins;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * enhancedTrident 规则对 {@code Projectile#hitTargetOrDeflectSelf} 的调用入口。
 *
 * <p>该方法是 vanilla 实体命中的顶层统一入口（1.20.5 引入；已对全部受支持版本字节码
 * 核实同名同签名同可见性）：先询问目标 {@code entity.deflection(this)}（盾反等），
 * 非 NONE 时直接执行 deflect 并返回、不调用 {@code onHit}；否则调用 {@code onHit}
 * 走完整命中链并返回 NONE。它为 protected，跨包不可直调，故经本接口以 @Invoker
 * 取得——secondary 目标与活塞轮队首都通过它走与首目标完全一致的原版链路
 * （deflection 判定、onHit / onHitEntity、伤害、附魔、击退、PROJECTILE_LAND）。
 */
@Mixin(Projectile.class)
public interface EnhancedTridentProjectileAccessor {

    @Invoker("hitTargetOrDeflectSelf")
    ProjectileDeflection carpetIceAddition$hitTargetOrDeflectSelf(HitResult hitResult);
}
