package com.ice2974.carpeticeaddition.rules;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * renewableRawIron 判定逻辑单测（core 平台）。
 *
 * <p>锁定三类关键语义：
 * <ul>
 *   <li>价格区间闭区间 4..8，五种价格都必须可生成（{@code nextInt(5)} 而非 {@code nextInt(4)}）；</li>
 *   <li>判重只读取原始成本（{@code getBaseCostA()}）：入参即基础成本，与 demand / 声望折扣
 *       导致的展示价偏移无关，因此谓词对同一基础价格恒为真（幂等，不会重复追加）；</li>
 *   <li>资格判定为「规则开启 + 石匠 + 等级恰为 5」，等级 4/6 均不成立。</li>
 * </ul>
 */
class RenewableRawIronTradeHelperTest {

    private static final int MIN_COST = 4;
    private static final int MAX_COST = 8;
    private static final int MAX_USES = 12;
    private static final int TRADE_XP = 30;

    // ---- 基础价格区间：闭区间 4..8，五种价格均可生成 ----

    @Test
    void rolledCostAlwaysStaysWithinClosedRange() {
        RandomSource random = RandomSource.create(20260815L);
        for (int i = 0; i < 20_000; i++) {
            int cost = RenewableRawIronTradeHelper.rollEmeraldCost(random);
            assertTrue(cost >= MIN_COST && cost <= MAX_COST, "rolled cost out of range: " + cost);
        }
    }

    @Test
    void everyPriceIncludingBothBoundsIsReachable() {
        RandomSource random = RandomSource.create(1L);
        boolean[] seen = new boolean[MAX_COST + 2];
        for (int i = 0; i < 20_000; i++) {
            seen[RenewableRawIronTradeHelper.rollEmeraldCost(random)] = true;
        }
        for (int cost = MIN_COST; cost <= MAX_COST; cost++) {
            assertTrue(seen[cost], "price " + cost + " was never generated (nextInt span must be 5)");
        }
        assertFalse(seen[MIN_COST - 1], "price " + (MIN_COST - 1) + " must never be generated");
        assertFalse(seen[MAX_COST + 1], "price " + (MAX_COST + 1) + " must never be generated");
    }

    // ---- 判重谓词：读取原始成本 ----

    /** 从真实 {@link MerchantOffer} 中按原始成本路径提取字段后判定。 */
    private static boolean isRawIronOfferOf(MerchantOffer offer) {
        return RenewableRawIronTradeHelper.isRenewableRawIronOffer(
                offer.getResult().is(Items.RAW_IRON_BLOCK),
                offer.getResult().getCount(),
                offer.getBaseCostA().is(Items.EMERALD),
                offer.getBaseCostA().getCount(),
                offer.getItemCostB().isPresent(),
                offer.getMaxUses(),
                offer.getXp());
    }

    private static MerchantOffer rawIronOffer(int emeraldCost) {
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCost),
                new ItemStack(Items.RAW_IRON_BLOCK),
                MAX_USES,
                TRADE_XP,
                0.05F);
    }

    @Test
    void baseCostWithinRangeIsRecognised() {
        for (int cost = MIN_COST; cost <= MAX_COST; cost++) {
            assertTrue(isRawIronOfferOf(rawIronOffer(cost)), "base cost " + cost + " must be recognised");
        }
    }

    @Test
    void predicateIsIdempotentAndImmuneToDynamicPriceState() {
        MerchantOffer offer = rawIronOffer(MIN_COST);
        // 判定只依赖原始成本；重复判定必须恒定，且不得因判定而改动交易状态。
        for (int i = 0; i < 1_000; i++) {
            assertTrue(isRawIronOfferOf(offer));
        }
        // demand / specialPriceDiff 属于展示价修饰，不得影响原始成本判重。
        offer.updateDemand();
        offer.setSpecialPriceDiff(-3);
        assertTrue(isRawIronOfferOf(offer), "dynamic price state must not break idempotence");
        assertTrue(offer.getBaseCostA().getCount() >= MIN_COST
                && offer.getBaseCostA().getCount() <= MAX_COST, "base cost must stay untouched");
        assertEquals(0, offer.getUses(), "判重不得消耗交易次数");
    }

    @Test
    void baseCostOutsideRangeIsRejected() {
        assertFalse(RenewableRawIronTradeHelper.isRenewableRawIronOffer(
                true, 1, true, MIN_COST - 1, false, MAX_USES, TRADE_XP));
        assertFalse(RenewableRawIronTradeHelper.isRenewableRawIronOffer(
                true, 1, true, MAX_COST + 1, false, MAX_USES, TRADE_XP));
    }

    @Test
    void mismatchedShapeIsRejected() {
        assertFalse(RenewableRawIronTradeHelper.isRenewableRawIronOffer(
                false, 1, true, MIN_COST, false, MAX_USES, TRADE_XP), "result item must be raw iron block");
        assertFalse(RenewableRawIronTradeHelper.isRenewableRawIronOffer(
                true, 2, true, MIN_COST, false, MAX_USES, TRADE_XP), "result count must be 1");
        assertFalse(RenewableRawIronTradeHelper.isRenewableRawIronOffer(
                true, 1, false, MIN_COST, false, MAX_USES, TRADE_XP), "cost item must be emerald");
        assertFalse(RenewableRawIronTradeHelper.isRenewableRawIronOffer(
                true, 1, true, MIN_COST, true, MAX_USES, TRADE_XP), "must not have a second cost");
        assertFalse(RenewableRawIronTradeHelper.isRenewableRawIronOffer(
                true, 1, true, MIN_COST, false, MAX_USES - 1, TRADE_XP), "maxUses must be 12");
        assertFalse(RenewableRawIronTradeHelper.isRenewableRawIronOffer(
                true, 1, true, MIN_COST, false, MAX_USES, TRADE_XP - 1), "xp must be 30");
    }

    // ---- 资格判定：规则开启 + 石匠 + 大师级 ----

    @Test
    void masterMasonIsEligible() {
        assertTrue(RenewableRawIronTradeHelper.isEligibleMasterMason(true, true, 5));
    }

    @Test
    void ineligibleCombinationsRejected() {
        assertFalse(RenewableRawIronTradeHelper.isEligibleMasterMason(false, true, 5), "rule disabled");
        assertFalse(RenewableRawIronTradeHelper.isEligibleMasterMason(true, false, 5), "not a mason");
        assertFalse(RenewableRawIronTradeHelper.isEligibleMasterMason(true, true, 4), "expert level");
        assertFalse(RenewableRawIronTradeHelper.isEligibleMasterMason(true, true, 6), "level above master");
    }

    // ---- 追加路径（参数化核心）：幂等、不重掷、不改既有交易 ----

    @Test
    void appendsSingleOfferForEligibleMasterMason() {
        MerchantOffers offers = new MerchantOffers();
        RenewableRawIronTradeHelper.addMasterMasonOfferIfMissing(true, true, 5, RandomSource.create(42L), offers);
        assertEquals(1, offers.size());
        assertTrue(isRawIronOfferOf(offers.get(0)));
        int baseCost = offers.get(0).getBaseCostA().getCount();
        assertTrue(baseCost >= MIN_COST && baseCost <= MAX_COST, "appended cost out of range: " + baseCost);
    }

    @Test
    void repeatedInvocationIsIdempotentAndNeverRerolls() {
        MerchantOffers offers = new MerchantOffers();
        RandomSource random = RandomSource.create(7L);
        RenewableRawIronTradeHelper.addMasterMasonOfferIfMissing(true, true, 5, random, offers);
        MerchantOffer first = offers.get(0);
        int firstCost = first.getBaseCostA().getCount();
        // 重复调用：不新增、不重掷（同一交易实例与基础价格保持不变）
        for (int i = 0; i < 100; i++) {
            RenewableRawIronTradeHelper.addMasterMasonOfferIfMissing(true, true, 5, random, offers);
        }
        assertEquals(1, offers.size());
        assertSame(first, offers.get(0));
        assertEquals(firstCost, offers.get(0).getBaseCostA().getCount());
    }

    @Test
    void existingEquivalentOfferSuppressesAppendWithoutTouchingIt() {
        // 判重先于掷价：命中即返回，不新增、不重掷、不改既有交易状态
        MerchantOffers offers = new MerchantOffers();
        MerchantOffer existing = rawIronOffer(MAX_COST);
        offers.add(existing);
        RenewableRawIronTradeHelper.addMasterMasonOfferIfMissing(true, true, 5, RandomSource.create(1L), offers);
        assertEquals(1, offers.size());
        assertSame(existing, offers.get(0));
        assertEquals(MAX_COST, offers.get(0).getBaseCostA().getCount());
        assertEquals(0, existing.getUses());
    }

    @Test
    void unrelatedOffersDoNotSuppressAppend() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, 1), new ItemStack(Items.STONE), 16, 5, 0.05F));
        RenewableRawIronTradeHelper.addMasterMasonOfferIfMissing(true, true, 5, RandomSource.create(1L), offers);
        assertEquals(2, offers.size());
        assertTrue(isRawIronOfferOf(offers.get(1)), "append must land after unrelated offers");
    }

    @Test
    void ineligiblePreconditionsAppendNothing() {
        // 规则关闭 / 非石匠 / 非大师级：均不追加（null offers 亦为安全 no-op）
        MerchantOffers ruleOff = new MerchantOffers();
        RenewableRawIronTradeHelper.addMasterMasonOfferIfMissing(false, true, 5, RandomSource.create(1L), ruleOff);
        assertTrue(ruleOff.isEmpty());

        MerchantOffers notMason = new MerchantOffers();
        RenewableRawIronTradeHelper.addMasterMasonOfferIfMissing(true, false, 5, RandomSource.create(1L), notMason);
        assertTrue(notMason.isEmpty());

        MerchantOffers notMaster = new MerchantOffers();
        RenewableRawIronTradeHelper.addMasterMasonOfferIfMissing(true, true, 4, RandomSource.create(1L), notMaster);
        assertTrue(notMaster.isEmpty());

        RenewableRawIronTradeHelper.addMasterMasonOfferIfMissing(true, true, 5, RandomSource.create(1L), null);
    }
}
