package com.ice2974.carpeticeaddition.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TheEndGatewayBlockEntity.class)
public interface TheEndGatewayBlockEntityAccessor {
    @Accessor("exitPortal")
    BlockPos carpetIceAddition$getExitPortal();

    @Accessor("exactTeleport")
    boolean carpetIceAddition$isExactTeleport();
}
