package com.ice2974.carpeticeaddition.mixins;

import carpet.CarpetServer;
import carpet.script.external.Vanilla;
import com.ice2974.carpeticeaddition.settings.CarpetIceAdditionLowVersionSettings;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 修复 Carpet 1.4.147（MC 1.21–1.21.1）的客户端上游崩溃：
 * 玩家先连接过一台 Carpet 多人服务器后，再退出单人世界时，{@code CarpetClient.disconnect()}
 * 会调用 {@code CarpetServer.onServerClosed(null)}；而 {@code onServerClosed} 用静态字段
 * {@code CarpetServer.minecraft_server} 作为进入守卫，方法体内却用形参 {@code server} 调用
 * {@link Vanilla#MinecraftServer_getScriptServer(MinecraftServer)}，导致对 {@code null}
 * 调用接口方法而抛出 {@code NullPointerException}。
 *
 * <p>本 Mixin 在 {@code onServerClosed} 内对 {@code Vanilla.MinecraftServer_getScriptServer}
 * 的<b>两处</b>调用分别做 {@code @ModifyArg}（{@code ordinal = 0} 与 {@code ordinal = 1}，均
 * {@code require = 1}）：当规则开启且传入的 {@code server} 为 {@code null} 时，用已通过守卫
 * 确认非 null 的 {@code CarpetServer.minecraft_server} 替换，避免 NPE，同时完整保留 Carpet 原有的
 * 关闭流程（scarpet、logger、HUD、network handler 等清理逻辑均不改动）。规则关闭时原样返回，补丁不介入。
 *
 * <p>注意：{@code onServerClosed} 为 {@code static} 方法，因此所有 handler 均为 {@code static}。
 *
 * <p>目标类 {@code carpet.CarpetServer} 是 Carpet 本体（非 Minecraft 类），故 Mixin 与注入点均设置
 * {@code remap = false}：Carpet 类名在开发期与运行期一致；形参类型 {@code net.minecraft.server.MinecraftServer}
 * 在 intermediary 与 named 中同名，描述符无需重映射即可在运行期匹配 Carpet 1.4.147。
 */
@Mixin(value = CarpetServer.class, remap = false)
public abstract class CarpetServerOnServerClosedNullFixMixin {

    @ModifyArg(
            method = "onServerClosed",
            at = @At(
                    value = "INVOKE",
                    target = "Lcarpet/script/external/Vanilla;MinecraftServer_getScriptServer(Lnet/minecraft/server/MinecraftServer;)Lcarpet/script/CarpetScriptServer;",
                    ordinal = 0
            ),
            index = 0,
            require = 1
    )
    private static MinecraftServer carpetIceAddition$fixNullScriptServerArg0(MinecraftServer server) {
        return carpetIceAddition$replaceNullScriptServer(server);
    }

    @ModifyArg(
            method = "onServerClosed",
            at = @At(
                    value = "INVOKE",
                    target = "Lcarpet/script/external/Vanilla;MinecraftServer_getScriptServer(Lnet/minecraft/server/MinecraftServer;)Lcarpet/script/CarpetScriptServer;",
                    ordinal = 1
            ),
            index = 0,
            require = 1
    )
    private static MinecraftServer carpetIceAddition$fixNullScriptServerArg1(MinecraftServer server) {
        return carpetIceAddition$replaceNullScriptServer(server);
    }

    private static MinecraftServer carpetIceAddition$replaceNullScriptServer(MinecraftServer server) {
        if (!CarpetIceAdditionLowVersionSettings.carpetSingleplayerExitCrashFix) {
            return server;
        }
        if (server != null) {
            return server;
        }
        // 守卫 if (minecraft_server != null) 已确认能进入方法体，故此处非 null。
        // 用它替换错误的 null 形参，对齐 Carpet 原本以 minecraft_server 作为引用来源的意图。
        return CarpetServer.minecraft_server;
    }
}
