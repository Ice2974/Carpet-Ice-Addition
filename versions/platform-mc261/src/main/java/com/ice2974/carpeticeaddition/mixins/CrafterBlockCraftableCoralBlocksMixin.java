package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.rules.CraftableCoralBlocksRecipes;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * B3 兜底：Crafter 的 {@code RecipeCache} 命中后不再经过 {@code RecipeManager.getRecipeFor}，
 * 因此 B2 兜不住 Crafter 的重复合成。此处注入 {@link CrafterBlock#getPotentialResults} 的 RETURN，
 * 规则关闭时把珊瑚块配方查询结果过滤为空，绕过缓存。
 *
 * <p>优先按 recipe id 判定（{@link CraftableCoralBlocksRecipes#isCoralRecipeId}），
 * 避免误伤其他 datapack / Mod 添加的同产物珊瑚块配方。
 */
@Mixin(CrafterBlock.class)
public abstract class CrafterBlockCraftableCoralBlocksMixin {
    @Inject(method = "getPotentialResults(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/crafting/CraftingInput;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private static void carpetIceAddition$filterCoral(ServerLevel level, CraftingInput input, CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir) {
        if (CarpetIceAdditionSettings.craftableCoralBlocks) {
            return;
        }
        cir.getReturnValue().ifPresent(holder -> {
            if (CraftableCoralBlocksRecipes.isCoralRecipeId(holder.id().identifier().toString())) {
                cir.setReturnValue(Optional.empty());
            }
        });
    }
}
