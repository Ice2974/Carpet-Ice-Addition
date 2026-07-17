package com.ice2974.carpeticeaddition.mixins;

import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionSettings;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.function.Function;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerDisableIllegalChatCharacterCheckMixin {

    @ModifyArg(
            method = "onUpdateSign(Lnet/minecraft/network/packet/c2s/play/UpdateSignC2SPacket;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;",
                    ordinal = 0
            ),
            slice = @Slice(
                    from = @At(
                            value = "INVOKE",
                            target = "Ljava/util/stream/Stream;of([Ljava/lang/Object;)Ljava/util/stream/Stream;",
                            ordinal = 0
                    ),
                    to = @At(
                            value = "INVOKE",
                            target = "Ljava/util/stream/Stream;collect(Ljava/util/stream/Collector;)Ljava/lang/Object;",
                            ordinal = 0
                    )
            ),
            index = 0
    )
    private Function<String, String> carpetIceAddition$preserveSignFormatting(
            Function<String, String> formatter
    ) {
        if (CarpetIceAdditionSettings.disableIllegalChatCharacterCheck) {
            return Function.identity();
        }
        return formatter;
    }
}
