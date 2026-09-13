package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.EnhancedTridentRearmEpoch;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentState;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * enhancedTrident 规则的 grounded-rearm 授予点：三叉戟真正从插地状态转为下落的
 * 瞬间，授予一次「重新移动后可攻击」的资格。
 *
 * <p>{@code AbstractArrow#startFalling}（yarn {@code fall}，private、无参，全部
 * 受支持版本 javap 核实同名同签名）是服务端 inGround true→false 转换的唯一汇入
 * 点，仅两个调用点：tick 插地分支的 {@code lastState != blockState && shouldFall()}
 * （支撑方块被移除 / 冲蚀，且被 {@code !noPhysics} 门控）与 {@code move()} 的
 * {@code type != SELF && shouldFall()}（活塞等外力推离支撑）。唯一旁路
 * {@code lerpMotion} 仅存在于客户端。因此在方法 TAIL 授予即精确对应「落地静止后
 * 重新开始自由下落」，且天然排除忠诚返回——返回段 {@code setNoPhysics(true)} 严格
 * 先于 {@code super.tick()}，全程 inGround 保持 true、不经过本方法；本处 noPhysics
 * 门控再兜底一次（move() 路径本身无 noPhysics 门控）。
 *
 * <p>授予记录当前规则代际（{@link EnhancedTridentRearmEpoch}），规则
 * true→false→true 快速切换后旧资格因代际不匹配失效。资格由 R1 门控
 * （{@code EnhancedTridentMixin}）读取、任何非空扫掠建轮（飞行轮或活塞轮）消费；
 * 本 Mixin 不写 vanilla 任何字段、不写 NBT。规则关闭时不做任何状态写入
 * （fail-closed，首行直接返回）。
 */
@Mixin(AbstractArrow.class)
public abstract class EnhancedTridentGroundedRearmMixin {

    @Inject(method = "startFalling", at = @At("TAIL"))
    private void carpetIceAddition$grantGroundedRearm(CallbackInfo ci) {
        if (!CarpetIceAdditionSettings.enhancedTrident
                || !((Object) this instanceof ThrownTrident self)
                || self.level().isClientSide()
                || self.noPhysics) {
            return;
        }
        ((EnhancedTridentState) self).carpetIceAddition$grantGroundedRearm(EnhancedTridentRearmEpoch.current());
    }
}
