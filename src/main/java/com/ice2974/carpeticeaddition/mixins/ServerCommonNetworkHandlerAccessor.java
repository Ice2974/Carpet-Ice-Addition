package com.ice2974.carpeticeaddition.mixins;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerCommonPacketListenerImpl.class)
public interface ServerCommonNetworkHandlerAccessor {
    @Accessor("server")
    MinecraftServer carpetIceAddition$getServer();
}
