package com.ice2974.carpeticeaddition.rules;

import com.ice2974.carpeticeaddition.CarpetIceAdditionMod;
import com.ice2974.carpeticeaddition.translation.TranslationFormatUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
//#if MC>=12111
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
//#else
//$$import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
//$$import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
//#endif
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 配方类内置数据包的共享协调门面：注册、Fabric 生命周期接线与公开 API。
 *
 * <p>编排状态机本体在 {@link RecipePackOrchestrator}（生产核心，可被单元测试直接驱动）；
 * 本类只保留：受管包注册（{@link #register}）、唯一的 START/END 监听器挂载（{@link #initialize}）、
 * 入参 null 守卫与锁定提示的翻译/发送组装，以及对**传入服务器**直接查询的
 * {@link #isBuiltinPackSelected(MinecraftServer, String)}。
 *
 * <p>为什么不每个规则一份控制器：Fabric 的 START/END 是全局事件，两份控制器会互相抢占并各自发起
 * reload，导致状态抢占与 reload 放大。因此计数、静默点、重试预算与收敛判定全部集中在编排核心，
 * 规则只提供钩子（{@link ManagedPack}）。绑定 / 身份 / epoch 状态由编排核心单一持有，本类不保存副本。
 *
 * <p>初始化前安全语义：{@link #isReconcileInFlight()} 在 {@link #initialize()} 之前返回 false
 * （与迁移前一致），不因编排核心延迟创建而抛 NPE；生命周期入口在初始化前为安全 no-op。
 */
public final class RecipePackCoordinator {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");

    /** 受管内置数据包：规则侧只需提供数据与钩子，不接触 reload 生命周期。 */
    public record ManagedPack(
            Identifier packId,
            String ruleName,
            String lockedMessageKey,
            BooleanSupplier locked,
            BooleanSupplier desired,
            Consumer<MinecraftServer> recomputeConflict,
            Consumer<MinecraftServer> syncMenus,
            Consumer<MinecraftServer> onPackDisabled,
            BiConsumer<MinecraftServer, ServerPlayer> onPlayerJoin,
            Runnable resetLockState) {

        public String packIdString() {
            return packId.toString();
        }
    }

    private static final List<ManagedPack> PACKS = new ArrayList<>();

    /**
     * 编排核心：在 {@link #initialize()} 中、全部 {@link #register(ManagedPack)} 完成之后以
     * {@code List.copyOf(PACKS)} 快照创建（初始化后 register 被 fail-closed 拒绝，
     * 因此快照必然包含完整注册列表，不会在静态初始化期复制到空列表）。
     */
    private static RecipePackOrchestrator orchestrator;

    private static boolean initialized;

    private RecipePackCoordinator() {
    }

    // ------------------------------------------------------------------ 注册与初始化

    /** 注册一个受管内置数据包（必须在 {@link #initialize()} 之前调用）。 */
    public static void register(ManagedPack pack) {
        if (initialized) {
            throw new IllegalStateException("RecipePackCoordinator already initialized");
        }
        PACKS.add(pack);
    }

    /** 注册全部内置数据包并挂载唯一的 START/END 监听器（幂等）。 */
    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        var container = FabricLoader.getInstance()
                .getModContainer(CarpetIceAdditionMod.MOD_ID)
                .orElseThrow(() -> new IllegalStateException("Missing " + CarpetIceAdditionMod.MOD_ID + " mod container"));
        // 注册完成后创建编排核心快照（见字段注释）；生产适配器只在此处注入一次。
        orchestrator = new RecipePackOrchestrator(List.copyOf(PACKS), new ProductionServerAdapter());
        for (ManagedPack pack : PACKS) {
            //#if MC>=12111
            boolean registered = ResourceLoader.registerBuiltinPack(pack.packId(), container, PackActivationType.NORMAL);
            //#else
            //$$            boolean registered = ResourceManagerHelper.registerBuiltinResourcePack(pack.packId(), container, ResourcePackActivationType.NORMAL);
            //#endif
            if (!registered) {
                LOGGER.warn("[Carpet Ice Addition] Failed to register builtin datapack {} for rule {}",
                        pack.packId(), pack.ruleName());
            }
        }
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register(
                (minecraftServer, ignored) -> {
                    if (orchestrator != null) {
                        orchestrator.onReloadStart(minecraftServer, minecraftServer);
                    }
                });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(
                (minecraftServer, ignored, success) -> {
                    if (orchestrator != null) {
                        orchestrator.onReloadEnd(minecraftServer, success);
                    }
                });
    }

    // ------------------------------------------------------------------ 生命周期入口

    public static void onServerLoadedWorlds(MinecraftServer server) {
        if (server == null || orchestrator == null) {
            return;
        }
        orchestrator.onServerLoadedWorlds(server, server);
    }

    /** @return true 表示该规则名由本协调器接管（入口类据此结束 observer 分支） */
    public static boolean onRuleChanged(String ruleName, MinecraftServer server) {
        if (server == null || ruleName == null || orchestrator == null) {
            return false;
        }
        return orchestrator.onRuleChanged(ruleName, server, server);
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        if (player == null || orchestrator == null) {
            return;
        }
        orchestrator.onPlayerLoggedIn(player, key -> player.sendSystemMessage(
                Component.literal(TranslationFormatUtil.translate(key))));
    }

    public static void onServerClosed(MinecraftServer server) {
        if (server == null || orchestrator == null) {
            return;
        }
        orchestrator.onServerClosed(server);
    }

    // ------------------------------------------------------------------ 查询

    /** 判断某个内置数据包在**传入服务器**上当前是否被选中（供同 ID 覆盖检测使用；语义与迁移前一致）。 */
    public static boolean isBuiltinPackSelected(MinecraftServer server, String packId) {
        return server != null && server.getPackRepository().getSelectedIds().contains(packId);
    }

    /** 当前是否仍有无法归属的进行中 reload（诊断用；初始化前安全返回 false）。 */
    public static boolean isReconcileInFlight() {
        return orchestrator != null && orchestrator.isReconcileInFlight();
    }

    // ------------------------------------------------------------------ 生产适配器

    /**
     * 生产环境的服务器操作适配器：持有「当前绑定服务器」的镜像，目标只在编排核心的
     * 绑定迁移 / 解绑时点被更新（见 {@link ServerAdapter#updateTarget}）。
     */
    private static final class ProductionServerAdapter implements ServerAdapter {
        private volatile MinecraftServer target;

        @Override
        public void updateTarget(MinecraftServer server) {
            this.target = server;
        }

        @Override
        public boolean isServerThread() {
            MinecraftServer server = target;
            return server != null && server.isSameThread();
        }

        @Override
        public Collection<String> selectedPackIds() {
            return target.getPackRepository().getSelectedIds();
        }

        @Override
        public CompletableFuture<Void> reloadResources(Collection<String> selectedIds) {
            return target.reloadResources(selectedIds);
        }
    }
}
