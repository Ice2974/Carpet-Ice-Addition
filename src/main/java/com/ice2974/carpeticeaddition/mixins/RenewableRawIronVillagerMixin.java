package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.rules.RenewableRawIronTradeHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * renewableRawIron 规则：在村民交易生成完成后追加一条大师级石匠专属交易。
 *
 * <p>回调只接收 {@link CallbackInfo}（Mixin {@code Inject} javadoc 的 Basic usage：不捕获目标
 * 上下文，官方明确说明该写法适用于「注入目标为多个签名不同的方法」）。借此同一份代码同时适配
 * {@code updateTrades()}（1.21.1–1.21.10）与 {@code updateTrades(ServerLevel)}（1.21.11–26.3），
 * 无需平台 override 文件，也不使用 {@code //#if} 结构分叉。
 *
 * <p>注入点语义：{@code updateTrades} 只在交易生成时执行（{@code getOffers()} 的
 * {@code offers == null} 惰性初始化分支，以及 {@code increaseMerchantCareer} 的升级调用），
 * 不在读取交易列表的路径上。因此本规则只影响此后新生成的交易，不会追溯修改已生成的交易列表。
 *
 * <p>只追加、不替换：不 {@code setOffers}、不清空、不重建；异常必须静默回退原版行为。
 */
@Mixin(Villager.class)
public abstract class RenewableRawIronVillagerMixin {

    @Inject(method = "updateTrades", at = @At("RETURN"))
    private void carpetIceAddition$addRenewableRawIronOffer(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        try {
            // 仅服务端：updateTrades 的三个调用点均在服务端可达（getOffers 的客户端分支会先行抛异常）。
            if (self.level().isClientSide()) {
                return;
            }
            // 此处 getOffers() 必然非 null 且正是原版刚追加过的列表；不会重入交易生成。
            RenewableRawIronTradeHelper.addMasterMasonOfferIfMissing(self, self.getOffers());
        } catch (Throwable throwable) {
            CarpetIceAdditionMod.reportFeatureCompatibilityIssue("renewableRawIron", throwable);
        }
    }
}
