package com.ice2974.carpeticeaddition.rules;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EasyCopperOxidationHelperTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void everyVanillaOxidationTransitionAdvancesOnceAndPreservesSharedProperties() {
        int checked = 0;
        for (Map.Entry<Block, Block> transition : WeatheringCopper.NEXT_BY_BLOCK.get().entrySet()) {
            for (BlockState state : transition.getKey().getStateDefinition().getPossibleStates()) {
                BlockState next = EasyCopperOxidationHelper.nextState(state).orElseThrow();
                assertEquals(transition.getValue(), next.getBlock());
                for (Property<?> property : state.getProperties()) {
                    if (next.hasProperty(property)) {
                        assertEquals(state.getValue(property), next.getValue(property));
                    }
                }
                checked++;
            }
        }
        assertTrue(checked > 0);
    }

    @Test
    void waxedAndFinalStatesHaveNoNextStage() {
        for (Block waxed : HoneycombItem.WAXABLES.get().values()) {
            assertFalse(EasyCopperOxidationHelper.nextState(waxed.defaultBlockState()).isPresent());
        }
        for (Block previous : WeatheringCopper.NEXT_BY_BLOCK.get().values()) {
            if (!WeatheringCopper.NEXT_BY_BLOCK.get().containsKey(previous)) {
                assertFalse(EasyCopperOxidationHelper.nextState(previous.defaultBlockState()).isPresent());
            }
        }
    }

    @Test
    void onlyOrdinaryWaterBottlesAreAccepted() {
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        assertTrue(EasyCopperOxidationHelper.isWaterBottle(water));
        assertFalse(EasyCopperOxidationHelper.isWaterBottle(
                PotionContents.createItemStack(Items.POTION, Potions.HEALING)));
        assertFalse(EasyCopperOxidationHelper.isWaterBottle(
                PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER)));

        water.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.of(Potions.WATER), Optional.empty(),
                List.of(new MobEffectInstance(MobEffects.SPEED, 100)), Optional.empty()));
        assertFalse(EasyCopperOxidationHelper.isWaterBottle(water));
    }

    @Test
    void dispenserRemainderConsumesExactlyOneBottleAndMergesOutputs() {
        BlockState dispenserState = Blocks.DISPENSER.defaultBlockState();
        DispenserBlockEntity dispenser = new DispenserBlockEntity(BlockPos.ZERO, dispenserState);
        BlockSource source = new BlockSource(null, BlockPos.ZERO, dispenserState, dispenser);

        ItemStack single = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        ItemStack replacement = EasyCopperOxidationHelper.BottleRemainderBehavior.INSTANCE.execute(source, single);
        assertTrue(single.isEmpty());
        assertTrue(replacement.is(Items.GLASS_BOTTLE));
        assertEquals(1, replacement.getCount());
        assertTrue(dispenser.isEmpty());

        dispenser.setItem(0, new ItemStack(Items.GLASS_BOTTLE, 63));
        ItemStack stacked = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        stacked.setCount(3);
        ItemStack remaining = EasyCopperOxidationHelper.BottleRemainderBehavior.INSTANCE.execute(source, stacked);
        assertEquals(2, remaining.getCount());
        assertEquals(64, dispenser.getItem(0).getCount());
    }
}
