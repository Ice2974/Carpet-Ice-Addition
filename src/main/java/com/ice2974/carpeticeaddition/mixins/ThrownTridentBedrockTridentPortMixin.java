package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.BedrockTridentPortRuleTracker;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.EntityHitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * bedrockTridentPort 规则：使落地后的三叉戟在重新移动时仍能像基岩版一样对实体造成伤害。
 *
 * <p>Java 原版中 {@code ThrownTrident#findHitEntity} 开头的
 * {@code if (this.dealtDamage) return null;} 是阻止再次伤害的唯一闸门（已对 1.21.1 /
 * 1.21.11 / 26.2 反汇编核实：方法内该字段读取唯一）。本 Mixin 只在该表达式取值上做
 * 「本次视为 false」的临时绕过，任何路径都不写真实 {@code dealtDamage}：其 NBT 键
 * DealtDamage、忠诚返回门控（tick 首部读取）均保持原版；绕过后的实体扫描
 * （super.findHitEntity）与命中处理（onHitEntity 的伤害数值、附魔、伤害来源、击退、
 * 击杀归属）全部原版路径。落地静止时原版 tick 的 inGround 分支结构性早退、不扫实体，
 * 因此插地三叉戟不会对靠近或重叠的实体产生接触伤害。
 *
 * <p>重新命中资格状态机（{@code @Unique}，非持久、不写 NBT，随实体对象生灭）：
 * <ul>
 *   <li>武装（tick TAIL，super.tick 为末句调用）：规则开启且（shakeTime == 7——本 tick
 *       onHitBlock 落地边沿，常量 7 已核实三代一致；或 inGroundTime &gt; 0——本 tick 实际
 *       处于落地态，飞行分支在实体扫描前已将其清零）。每次落地期至多一次资格。</li>
 *   <li>消费（onHitEntity HEAD）：进入原版伤害流程即消费，与原版「首次命中即置位」对称；
 *       消费后直至下一次真实落地前不会重新武装（实体命中与 onHitBlock 每 tick 互斥、
 *       shakeTime 每 tick 严格递减、飞行 tick 的 inGroundTime 恒为 0）。</li>
 *   <li>世代隔离：规则每次变更（任意方向）经 BedrockTridentPortRuleTracker 递增世代，
 *       旧世代武装的资格立即不可见，覆盖「true→false→true 在下一次实体 tick 前完成」的
 *       极端时序；规则为 false 时闸门表达式恒返回原值（完整原版闸门），tick TAIL 另行
 *       主动清零兜底。</li>
 * </ul>
 *
 * <p>忠诚返回（setNoPhysics(true)）不绕过；1.21.1 在 noPhysics 状态下 tick 仍会调用
 * findHitEntity，依赖使用点的 {@code !isNoPhysics()} 防护。
 *
 * <p>本文件以 main 态（1.21.11）书写并覆盖全部 11 个平台：1.21.11 起 ThrownTrident
 * 移入 projectile.arrow 子包，1.21.1～1.21.10 无该子包，该类移动经
 * versions/mapping-1.21.11-1.21.10.txt 的显式条目沿版本图下推；tick /
 * onHitEntity / findHitEntity / dealtDamage / inGroundTime / shakeTime / isNoPhysics
 * 在全部受支持版本名称与语义一致，注入 descriptor 无版本差异。AbstractArrow 的
 * 继承成员经 AbstractArrowBedrockTridentPortAccessor 只读取得（Mixin 的 @Shadow
 * 不解析父类成员）。
 *
 * <p>回退策略：若 {@code @At(value = "FIELD")} 的完整目标串无法织入，改用
 * {@code @ModifyReturnValue(method = "findHitEntity")} 补做扫描——必须先以只读
 * {@code @Shadow private boolean dealtDamage} 确认真实值为 true（即原版 null 确由闸门
 * 产生，而非 dealtDamage == false 时的正常空扫描），再以 ProjectileUtil.getEntityHitResult
 * 原样委托基类实现；严禁对普通空扫描的 null 结果重复扫描，严禁任何路径写 dealtDamage。
 */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentBedrockTridentPortMixin {

    /**
     * AbstractArrow#onHitBlock 落地当 tick 写入 shakeTime 的常量值
     * （1.21.1 / 1.21.11 / 26.2 字节码核实一致；tick 开头先自减，故 TAIL 处 == 7
     * 当且仅当本 tick 发生了新的 onHitBlock 落地）。
     */
    private static final int ON_HIT_BLOCK_SHAKE_TIME = 7;

    @Unique
    private boolean carpetIceAddition$rehitArmed;

    @Unique
    private int carpetIceAddition$rehitGeneration;

    @Unique
    private AbstractArrowBedrockTridentPortAccessor carpetIceAddition$arrowView() {
        // Mixin 的 @Shadow 不解析父类 AbstractArrow 声明的成员，经只读 accessor 接口取得。
        return (AbstractArrowBedrockTridentPortAccessor) (Object) this;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void carpetIceAddition$updateRehitEligibility(CallbackInfo ci) {
        try {
            if (!CarpetIceAdditionSettings.bedrockTridentPort) {
                this.carpetIceAddition$rehitArmed = false;
                return;
            }
            int generation = BedrockTridentPortRuleTracker.generation();
            if (this.carpetIceAddition$rehitArmed && this.carpetIceAddition$rehitGeneration != generation) {
                this.carpetIceAddition$rehitArmed = false;
            }
            AbstractArrowBedrockTridentPortAccessor arrow = this.carpetIceAddition$arrowView();
            if (!this.carpetIceAddition$rehitArmed
                    && (arrow.carpetIceAddition$getShakeTime() == ON_HIT_BLOCK_SHAKE_TIME
                    || arrow.carpetIceAddition$getInGroundTime() > 0)) {
                this.carpetIceAddition$rehitArmed = true;
                this.carpetIceAddition$rehitGeneration = generation;
            }
        } catch (Throwable throwable) {
            this.carpetIceAddition$rehitArmed = false;
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("bedrockTridentPort", throwable);
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void carpetIceAddition$consumeRehitEligibility(EntityHitResult result, CallbackInfo ci) {
        this.carpetIceAddition$rehitArmed = false;
    }

    @ModifyExpressionValue(
            method = "findHitEntity",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/projectile/arrow/ThrownTrident;dealtDamage:Z",
                    opcode = Opcodes.GETFIELD
            )
    )
    private boolean carpetIceAddition$bypassDealtDamageGateForRehit(boolean original) {
        if (!original || !CarpetIceAdditionSettings.bedrockTridentPort
                || this.carpetIceAddition$arrowView().carpetIceAddition$isNoPhysics()) {
            return original;
        }
        try {
            return !(this.carpetIceAddition$rehitArmed
                    && this.carpetIceAddition$rehitGeneration == BedrockTridentPortRuleTracker.generation());
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("bedrockTridentPort", throwable);
            return original;
        }
    }
}
