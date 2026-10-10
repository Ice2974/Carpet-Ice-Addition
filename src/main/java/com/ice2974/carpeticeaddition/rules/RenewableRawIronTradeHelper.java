package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * renewableRawIron 规则（可再生粗铁）的判定与追加逻辑。
 *
 * <p>语义边界：本规则只影响<b>交易生成</b>，不修改任何已生成的交易列表。
 * 因此“已经是大师级”与“已经生成过大师级交易列表”是两件不同的事：
 * 前者的交易列表若尚未初始化（例如刚从 NBT 载入、或从未被取用），
 * 首次生成时仍会获得本交易；后者不会被追溯追加。
 *
 * <p>识别本规则交易的判据全部取自 {@link MerchantOffer} 的<b>原始成本</b>
 * （{@code getBaseCostA()}，即 {@link ItemCost} 中保存的原始数量）。禁止使用
 * {@code getCostA()} / {@code getCostB()}：它们返回经 demand、priceMultiplier、
 * specialPriceDiff（声望折扣）修饰后的展示副本，会被交易次数与折扣污染，破坏幂等性。
 *
 * <p>判重必须先于随机价格生成：只要已存在等价交易，就直接返回，
 * 既不新增、也不重新掷价、也不改动既有交易状态（uses / demand / specialPriceDiff）。
 */
public final class RenewableRawIronTradeHelper {

    /** 大师级交易价格区间（含两端）：4～8 颗绿宝石购买 1 个粗铁块。 */
    private static final int EMERALD_COST_MIN = 4;
    private static final int EMERALD_COST_MAX = 8;

    /** 区间宽度；{@code nextInt(SPAN)} 覆盖 4/5/6/7/8 五种价格（不可写成 4）。 */
    private static final int EMERALD_COST_SPAN = EMERALD_COST_MAX - EMERALD_COST_MIN + 1;

    /** 与 26.x {@code villager_trade/mason/5/*.json} 的 {@code max_uses} 一致。 */
    private static final int MAX_USES = 12;

    /** 原版大师级交易的交易经验取值（石匠大师级两条原版交易均为 30）。 */
    private static final int TRADE_XP = 30;

    /** 与 26.x 数据包字段 {@code reputation_discount} 一致。 */
    private static final float PRICE_MULTIPLIER = 0.05F;

    private static final int MASTER_LEVEL = 5;
    private static final int RESULT_COUNT = 1;

    private RenewableRawIronTradeHelper() {
    }

    /**
     * 掷出一次基础价格（闭区间 4～8）。
     *
     * <p>只在确认不存在等价交易之后调用，保证既有价格不会被重新随机。
     */
    public static int rollEmeraldCost(RandomSource random) {
        return EMERALD_COST_MIN + random.nextInt(EMERALD_COST_SPAN);
    }

    /**
     * 纯逻辑判定：该交易是否为本规则追加的粗铁块交易。
     *
     * <p>参数全部为原始值，便于单元测试；成本相关的参数必须来自
     * {@link MerchantOffer#getBaseCostA()}。
     */
    static boolean isRenewableRawIronOffer(
            boolean resultIsRawIronBlock, int resultCount,
            boolean costIsEmerald, int baseCostCount,
            boolean hasSecondCost, int maxUses, int xp) {
        return resultIsRawIronBlock
                && resultCount == RESULT_COUNT
                && costIsEmerald
                && baseCostCount >= EMERALD_COST_MIN
                && baseCostCount <= EMERALD_COST_MAX
                && !hasSecondCost
                && maxUses == MAX_USES
                && xp == TRADE_XP;
    }

    /** 纯逻辑判定：规则开启 + 石匠 + 等级恰为大师级。 */
    static boolean isEligibleMasterMason(boolean ruleEnabled, boolean isMason, int level) {
        return ruleEnabled && isMason && level == MASTER_LEVEL;
    }

    /**
     * 在交易生成完成后追加本规则的交易。
     *
     * <p>只从 {@code Villager#updateTrades} 的返回点调用。该调用点保证
     * {@code offers} 已初始化且正是刚被原版追加过的那个列表对象，因此本方法
     * <b>不再调用</b> {@code villager.getOffers()}，不会重入交易生成。
     *
     * <p>只追加：不 {@code setOffers}、不清空、不重建、不修改既有交易。
     *
     * @param villager 目标村民
     * @param offers   当前交易列表（由调用方传入，避免递归取用）
     */
    public static void addMasterMasonOfferIfMissing(Villager villager, MerchantOffers offers) {
        if (offers == null) {
            return;
        }
        if (!CarpetIceAdditionSettings.renewableRawIron) {
            return;
        }

        VillagerData data = villager.getVillagerData();
//#if MC>=12105
        int level = data.level();
        boolean isMason = data.profession().is(VillagerProfession.MASON);
//#else
//$$        int level = data.getLevel();
//$$        boolean isMason = data.getProfession() == VillagerProfession.MASON;
//#endif

        if (!isEligibleMasterMason(true, isMason, level)) {
            return;
        }

        // 判重必须先于随机价格生成：命中即返回，不新增、不重掷、不改动既有交易。
        for (MerchantOffer existing : offers) {
            if (isRenewableRawIronOffer(existing)) {
                return;
            }
        }

        ItemStack result = new ItemStack(Items.RAW_IRON_BLOCK);
        ItemCost cost = new ItemCost(Items.EMERALD, rollEmeraldCost(villager.getRandom()));
        offers.add(new MerchantOffer(cost, result, MAX_USES, TRADE_XP, PRICE_MULTIPLIER));
    }

    private static boolean isRenewableRawIronOffer(MerchantOffer offer) {
        ItemStack result = offer.getResult();
        ItemStack baseCost = offer.getBaseCostA();
        return isRenewableRawIronOffer(
                result.is(Items.RAW_IRON_BLOCK), result.getCount(),
                baseCost.is(Items.EMERALD), baseCost.getCount(),
                offer.getItemCostB().isPresent(),
                offer.getMaxUses(), offer.getXp());
    }
}
