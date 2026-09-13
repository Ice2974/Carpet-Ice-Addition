package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentHelper;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentSweeper;
import com.ice2974.carpeticeaddition.rules.EnhancedTridentState;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;

/**
 * enhancedTrident 规则的 R2（活塞轮）：活塞推动三叉戟产生实际位移时，无视历史
 * {@code dealtDamage} 独立建立攻击轮。
 *
 * <p>原版活塞推动链（{@code PistonMovingBlockEntity#moveEntityByPiston →
 * Entity.move(MoverType.PISTON, …)}，26.x 同名已核实）在 AbstractArrow.move 的
 * override（全部受支持版本存在，super.move + 非 SELF 时 startFalling）中使插地
 * 投射物恢复下落，但不恢复伤害能力——dealtDamage 仍为 true，R1 闸门持续返回 null。
 * 本 WrapMethod 在原版 move 完成后，以「移动前位置/包围盒 + 实际位移段」为扫掠输入：
 *
 * <ul>
 *   <li>只支持 {@code MoverType.PISTON}（SHULKER / SHULKER_BOX 不在本规则范围）；
 *       位移段长不大于 ε（零位移）不建轮、不改速度、不扫描；无候选时不建轮、不写速度。</li>
 *   <li>非空扫掠建轮时消费 grounded-rearm 资格：活塞把三叉戟推离支撑时，
 *       {@code original.call} 内的 vanilla {@code startFalling} 刚授予 R1 资格，
 *       若本次 R2 已命中实体而不消费，下一 tick R1 会对同一推动追加一次攻击；
 *       本次无候选（未建轮）则资格保留，供后续自由下落由 R1 使用。</li>
 *   <li>不对方块做独立的二次裁剪：{@code Entity.move} 对 PISTON 位移先
 *       limitPistonMovement、再 collide → collideBoundingBox 按实体 AABB 对方块
 *       碰撞做扫掠裁剪后才 setPos（1.21.1 与 26.2 字节码两端核实），移动后的 post
 *       就是 vanilla 实际允许的终点——实际发生的 pre→post 段本身不可能穿墙，
 *       直接以 maxT = 1 扫描全段即可；反之，独立 raycast 会在三叉戟仍插在方块内时
 *       把起点方块当作 t≈0 的阻挡，错误取消合法的活塞命中。</li>
 *   <li>入射向量 = 段向量本身（伤害固定 8.0+附魔、与速度无关；击退方向与 deflection
 *       依据 deltaMovement，故派发前临时注入段向量）。队首经 @Invoker
 *       hitTargetOrDeflectSelf 走完整原版链，secondary 由 onHit 包装在同一调用栈内
 *       派发；普通 secondary 后速度恢复为队首响应后的 afterFirst，真 deflection
 *       保留 deflect 写入的原版状态并终止本轮。队首被盾反（onHit 未被调用、轮未
 *       消费）时保留 deflect 写入的原版状态，finally 中清除残留轮。</li>
 *   <li>注入速度的回收以「引用同一性」判定：完整命中链结束后 deltaMovement 仍是
 *       注入的那个 Vec3 实例（如成功伤害 Enderman 的提前 return 路径——vanilla 未做
 *       任何 self-motion 响应）时，注入值只是本规则的临时 artifact，恢复为活塞
 *       move 完成后的真实速度；vanilla 一旦改写过速度，字段必然指向新实例（Vec3
 *       运算全部分配新对象、get/setDeltaMovement 为纯字段读写），保留 vanilla 写入
 *       的结果，三叉戟不会凭空获得段方向的持续速度。</li>
 * </ul>
 *
 * <p>R2 完成后不写 {@code dealtDamage}——onHitEntity 链自身在每次命中时置位；R2
 * 门控自身也不要求 grounded-rearm（保持忽略 dealtDamage），只在非空扫掠建轮时消费
 * 它（见上），下一 tick 的 R1 闸门因此不追加。重新武装只来自下一次实际位移或真正的
 * 重新离地。规则关闭时不做任何快照、扫描或状态写入（首行直接透传）。
 */
@Mixin(AbstractArrow.class)
public abstract class EnhancedTridentPistonMoveMixin {

    @WrapMethod(method = "move")
    private void carpetIceAddition$pistonSweep(MoverType type, Vec3 movement, Operation<Void> original) {
        if (type != MoverType.PISTON
                || !((Object) this instanceof ThrownTrident self)
                || !CarpetIceAdditionSettings.enhancedTrident
                || self.level().isClientSide()
                || self.noPhysics) {
            original.call(type, movement);
            return;
        }
        Vec3 pre = self.position();
        AABB preBox = self.getBoundingBox();
        original.call(type, movement);
        try {
            Vec3 post = self.position();
            Vec3 segment = post.subtract(pre);
            if (!EnhancedTridentHelper.isSignificantSegment(segment.x, segment.y, segment.z)) {
                return;
            }
            // post 已是 vanilla Entity.move 经方块碰撞裁剪后的实际终点（见类 javadoc），
            // 实际发生的 pre→post 段不可能穿墙，直接以该段扫掠
            List<EnhancedTridentSweeper.SweepHit> hits = EnhancedTridentSweeper.collect(
                    self, pre, post, preBox, EnhancedTridentState.sweepMargin());
            if (hits.isEmpty()) {
                return;
            }
            EnhancedTridentSweeper.SweepHit head = hits.get(0);
            List<EnhancedTridentSweeper.SweepHit> secondaries = new ArrayList<>(hits.size() - 1);
            for (int i = 1; i < hits.size(); i++) {
                secondaries.add(hits.get(i));
            }
            EnhancedTridentState state = (EnhancedTridentState) self;
            // 建轮即消费 grounded-rearm（类 javadoc），无候选路径已提前 return
            state.carpetIceAddition$consumeGroundedRearm();
            state.carpetIceAddition$setRound(new EnhancedTridentState.EnhancedTridentRound(
                    self.tickCount, head.entity.getId(), secondaries, segment));
            Vec3 velocityBeforeDispatch = self.getDeltaMovement();
            try {
                self.setDeltaMovement(segment);
                ((EnhancedTridentProjectileAccessor) (Object) this).carpetIceAddition$hitTargetOrDeflectSelf(
                        new EntityHitResult(head.entity, head.location));
            } finally {
                // 已消费时轮已被 onHit 包装取走（此处为 no-op）；未消费（队首盾反等
                // deflection 路径不经过 onHit）时清除残留，速度保持 deflect 的写入
                state.carpetIceAddition$setRound(null);
                if (self.getDeltaMovement() == segment) {
                    self.setDeltaMovement(velocityBeforeDispatch);
                }
            }
        } catch (Throwable throwable) {
            if ((Object) this instanceof EnhancedTridentState state) {
                state.carpetIceAddition$setRound(null);
            }
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("enhancedTrident", throwable);
        }
    }
}
