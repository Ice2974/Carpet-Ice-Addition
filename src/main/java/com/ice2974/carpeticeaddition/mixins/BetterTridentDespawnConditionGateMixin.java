package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.BetterTridentDespawnEpoch;
import com.ice2974.carpeticeaddition.rules.BetterTridentDespawnState;
import com.ice2974.carpeticeaddition.rules.BetterTridentDespawnTracker;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * betterTridentDespawnCondition 规则的门控：只在 vanilla despawn 决策处增加
 * 「连续静止满 1200 个有效采样」资格条件，不修改原版消失时间或 despawn 计时器。
 *
 * <p><strong>注入点核实（全部受支持版本逐一字节码核实）</strong>：
 * {@code AbstractArrow#tickDespawn} 仅被 {@code tick()} 的插地分支在服务端调用，
 * 方法体为 {@code life++; if (life >= 1200) discard();}（唯一一处 discard 与唯一
 * 一处 1200 常量）；{@code ThrownTrident#tickDespawn} 覆写先做忠诚豁免
 * （{@code pickup == ALLOWED && loyalty > 0} 时跳过 super），因此忠诚返回路径
 * 天然不经过本门控。{@code startFalling} 是插地 → 下落转换的唯一汇入点（vanilla
 * 在其中重置 {@code life}）；{@code move(MoverType, Vec3)} 是 MoverType 型外力
 * （活塞等）位移的入口。三者同名同签名于全部受支持版本。
 *
 * <p><strong>为何 WrapMethod 而非拦截 discard 调用点</strong>：tickDespawn 内
 * {@code discard()} 调用的 Methodref owner 是 AbstractArrow 自身，且其 FQCN 在
 * 1.21.11 起从 {@code projectile} 迁移到 {@code projectile.arrow}；捆绑 Mixin 的
 * INVOKE owner 匹配为严格字符串相等（无超类宽松回退），call-site 拦截
 * （@WrapOperation / @WrapWithCondition / @Redirect）的 @At target 字符串将随
 * 版本纪元分叉、须以 per-version override 表达（8 份整文件副本），故弃用。
 *
 * <p><strong>扣留语义与组合风险</strong>：静止不足时（且 vanilla 以当前 life 将要
 * 触发 discard 时）本 wrap 不调用 original，改为复刻执行饱和的 {@code life++}
 * （{@link BetterTridentDespawnTracker#detainedLifeAdvance}，恒不超过阈值）——计时器
 * 照常自增、超过阈值也不暂停且永不越过 NBT {@code (short)} 表示范围，原版阈值 /
 * 重置 / NBT 持久化全部不动。
 * 「预测直通」（{@code life + 1 < VANILLA_DESPAWN_THRESHOLD} 时原样调用 original）
 * 把跳过窗口收窄到「vanilla 想删而本规则扣留」的 tick：其余 tick 上方法体内其他
 * Mixin 的变换（HEAD/TAIL 注入等）全部照常执行。已知残余风险：扣留 tick 上被跳过
 * 的是整个（已含第三方变换的）tickDespawn 方法体。非三叉戟箭、规则关闭、静止已
 * 满阈值的三叉戟均与无本 mod 行为完全一致。
 *
 * <p><strong>未来版本移植要求</strong>：{@link BetterTridentDespawnTracker#VANILLA_DESPAWN_THRESHOLD}
 * 与 tickDespawn 方法体结构是对当前全部受支持版本核实的不变式，新增 / 移植
 * Minecraft 版本时必须重新核实两者，不得假设继续成立。
 *
 * <p><strong>移动判定（双通道，仅作用于三叉戟）</strong>：通道 A = 每个
 * tickDespawn 调用（即每个服务端插地 tick）以坐标数值精确比较采样（插地无外力时
 * 整条 tick 链不写坐标，任何数值变化即真实位移）；通道 B = {@code move()} 观测器
 * 以实际 pre/post 坐标判定单次调用净位移（不使用传入 movement 向量——方块碰撞
 * 可能裁剪实际位移），捕获同一 tick 内「推去又推回」的往返（每段非零净位移各自
 * 即时重置）。已知边界：经非 move() 通路在同一 tick 内完成且数值精确归位的往返
 * 位移无法检测（无已知原版机制可达）。
 *
 * <p><strong>异常纪律</strong>：try/catch 仅包裹本规则的守卫 / 采样 / 判定逻辑，
 * 失败上报并 fail-open（放行原版）；{@code original.call()} 为唯一调用点且位于
 * catch 范围之外，vanilla / 第三方 Mixin 抛出的异常原样向上传播，绝不因异常重放
 * original。规则关闭时三条路径均首行透传，零额外状态写入。
 */
@Mixin(AbstractArrow.class)
public abstract class BetterTridentDespawnConditionGateMixin {

    @Shadow
    private int life;

    @WrapMethod(method = "tickDespawn")
    private void carpetIceAddition$gateDespawn(Operation<Void> original) {
        boolean passThrough = true;
        try {
            if (CarpetIceAdditionSettings.betterTridentDespawnCondition
                    && ((Object) this instanceof ThrownTrident self)
                    && !self.level().isClientSide()
                    && self instanceof BetterTridentDespawnState state) {
                state.carpetIceAddition$sampleGroundedStationary(
                        self.getX(), self.getY(), self.getZ(), BetterTridentDespawnEpoch.current());
                passThrough = state.carpetIceAddition$stationaryTicks() >= BetterTridentDespawnTracker.STATIONARY_TICKS_REQUIRED
                        || BetterTridentDespawnTracker.vanillaWouldNotDiscard(this.life);
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("betterTridentDespawnCondition", throwable);
            passThrough = true;
        }
        if (passThrough) {
            original.call();
        } else {
            // 扣留：vanilla 本 tick 将要 discard；复刻 life++ 使计时器照常推进（饱和于阈值，
            // 见 Tracker#detainedLifeAdvance），跳过其后的阈值比较与 discard
            // （类 javadoc「扣留语义与组合风险」）
            this.life = BetterTridentDespawnTracker.detainedLifeAdvance(this.life);
        }
    }

    @Inject(method = "startFalling", at = @At("TAIL"))
    private void carpetIceAddition$resetOnLeaveGround(CallbackInfo ci) {
        if (CarpetIceAdditionSettings.betterTridentDespawnCondition
                && ((Object) this instanceof ThrownTrident self)
                && !self.level().isClientSide()
                && self instanceof BetterTridentDespawnState state) {
            // 与 vanilla 同方法重置 life 对称：离地即位移事件，静止计时作废
            state.carpetIceAddition$onDisplacement();
        }
    }

    @WrapMethod(method = "move")
    private void carpetIceAddition$observeExternalMove(MoverType type, Vec3 movement, Operation<Void> original) {
        if (!CarpetIceAdditionSettings.betterTridentDespawnCondition
                || !((Object) this instanceof ThrownTrident self)
                || self.level().isClientSide()) {
            original.call(type, movement);
            return;
        }
        Vec3 pre = self.position();
        original.call(type, movement);
        try {
            Vec3 post = self.position();
            if (!BetterTridentDespawnTracker.samePosition(pre.x, pre.y, pre.z, post.x, post.y, post.z)
                    && self instanceof BetterTridentDespawnState state) {
                state.carpetIceAddition$onDisplacement();
            }
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("betterTridentDespawnCondition", throwable);
        }
    }
}
