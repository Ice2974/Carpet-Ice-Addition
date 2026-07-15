package com.ice2974.carpeticeaddition.mixins;

import carpet.CarpetServer;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventSnapshot26;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventState26;
import com.ice2974.carpeticeaddition.villagerevents.VillagerEventsRuntime26;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Villager.class)
public abstract class VillagerLightningVillagerEventsMixin {
    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob carpetIceAddition$observeConversion(Villager villager, EntityType<? extends Mob> type, ConversionParams params, ConversionParams.AfterConversion<?> finalizer) {
        VillagerEventState26 state = (VillagerEventState26) villager;
        VillagerEventSnapshot26 snapshot = VillagerEventsRuntime26.snapshot(villager, null);
        state.carpetIceAddition$beginConversion(snapshot);
        Mob result = villager.convertTo(type, params, (ConversionParams.AfterConversion) finalizer);
        if (state.carpetIceAddition$finishConversion(result instanceof Witch) && CarpetServer.minecraft_server != null) VillagerEventsRuntime26.conversion(CarpetServer.minecraft_server, "witch", snapshot);
        return result;
    }
}
