package com.ice2974.carpeticeaddition.rules;

import net.minecraft.server.MinecraftServer;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * 编排核心（{@link RecipePackOrchestrator}）直接触碰的服务器操作的唯一抽象。
 *
 * <p>生产实现包裹当前绑定的真实 {@link MinecraftServer}（由 {@link RecipePackCoordinator}
 * 持有）；单元测试以伪造实现直接驱动生产编排（仓库测试约定为手写桩，不引入 mock 框架）。
 * 三个操作方法的 API 在 root 源码中已被全部支持版本使用，无预处理宏。
 *
 * <p><b>目标一致性</b>：实现持有「当前绑定服务器」的镜像。目标更新只由编排核心在
 * <b>绑定迁移成功 / 解绑成功</b>的确切时点经 {@link #updateTarget(MinecraftServer)} 发出
 * （不变式：{@code target ≡ 编排核心当前绑定的服务器}）；未知规则的规则变化、令牌不匹配的
 * END / 关闭、同令牌重复绑定都不会更新目标——门面不做任何投机性更新，从结构上消除双状态漂移。
 */
interface ServerAdapter {

    /** 当前线程是否为该服务器的服务器线程（{@code server::isSameThread}）。 */
    boolean isServerThread();

    /** 当前选中的数据包 id 集合（{@code server.getPackRepository().getSelectedIds()}）。 */
    Collection<String> selectedPackIds();

    /** 发起一次资源 reload（{@code server.reloadResources(ids)}），调用语义与直接调用一致。 */
    CompletableFuture<Void> reloadResources(Collection<String> selectedIds);

    /** 绑定迁移 / 解绑时同步目标服务器；测试实现默认忽略（不持有目标）。 */
    default void updateTarget(MinecraftServer server) {
    }
}
