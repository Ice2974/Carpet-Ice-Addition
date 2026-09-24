package com.ice2974.carpeticeaddition.rules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class EasyCopperOxidationHelper {
    private EasyCopperOxidationHelper() {
    }

    public static boolean isWaterBottle(ItemStack stack) {
        return stack.is(Items.POTION)
                && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
    }

    public static Optional<BlockState> nextState(BlockState state) {
        return WeatheringCopper.getNext(state.getBlock()).map(block -> block.withPropertiesOf(state));
    }

    public static boolean tryOxidize(ServerLevel level, BlockPos pos, Entity actor) {
        BlockState state = level.getBlockState(pos);
        Optional<BlockState> next = nextState(state);
        if (next.isEmpty() || !hasValidPartner(level, pos, state)) {
            return false;
        }

        BlockState nextState = next.get();
        if (!level.setBlock(pos, nextState, Block.UPDATE_ALL_IMMEDIATE)) {
            return false;
        }

        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                5, 0.4, 0.0, 0.4, 0.1);
        level.gameEvent(GameEvent.BLOCK_CHANGE, Vec3.atCenterOf(pos), GameEvent.Context.of(actor, nextState));
        return true;
    }

    private static boolean hasValidPartner(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock) {
            DoubleBlockHalf half = state.getValue(DoorBlock.HALF);
            BlockState other = level.getBlockState(half == DoubleBlockHalf.LOWER ? pos.above() : pos.below());
            return other.is(state.getBlock()) && other.getValue(DoorBlock.HALF) != half;
        }

        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            Direction direction = ChestBlock.getConnectedDirection(state);
            BlockState other = level.getBlockState(pos.relative(direction));
            return other.is(state.getBlock()) && other.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                    && ChestBlock.getConnectedDirection(other) == direction.getOpposite();
        }
        return true;
    }

    public static final class BottleRemainderBehavior extends DefaultDispenseItemBehavior {
        public static final BottleRemainderBehavior INSTANCE = new BottleRemainderBehavior();

        private BottleRemainderBehavior() {
        }

        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack) {
            return consumeWithRemainder(source, stack, new ItemStack(Items.GLASS_BOTTLE));
        }
    }
}
