package com.ice2974.carpeticeaddition;

import carpet.CarpetServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deltarune 系隐藏彩蛋的统一生命周期入口。当前内含 Kris &amp; Susie 彩蛋：
 * 同一服务器中同时存在名为 Kris 与 Susie 的玩家（忽略大小写，真人或 Carpet 假人）时，
 * 以服务器系统消息广播一句 Susie 的台词，每次服务器运行只触发一次；
 * 状态仅存内存、不落盘、无规则开关。彩蛋增多时再拆分独立实现类。
 * 检查失败仅记 debug 日志，不影响登录流程。
 */
final class DeltaruneEasterEggs {
    private static final Logger LOGGER = LoggerFactory.getLogger("Carpet Ice Addition");

    private static final String KRIS = "Kris";
    private static final String SUSIE = "Susie";
    private static final Component SUSIE_LINE = Component.literal(
            "<Susie> GOD FXXKING damnit KRIS where the FXXK are we!?");

    private static boolean krisAndSusieTriggered;

    private DeltaruneEasterEggs() {
    }

    static void onServerLoaded() {
        krisAndSusieTriggered = false;
    }

    static void onServerClosed() {
        krisAndSusieTriggered = false;
    }

    static void onPlayerLoggedIn(ServerPlayer player) {
        if (krisAndSusieTriggered || CarpetServer.minecraft_server == null) {
            return;
        }
        try {
            String joiningName = player.getScoreboardName();
            // 只有 Kris / Susie 本人加入才可能首次凑齐两人；其余登录（含高频 relog 假人）无需扫描
            String otherName;
            if (KRIS.equalsIgnoreCase(joiningName)) {
                otherName = SUSIE;
            } else if (SUSIE.equalsIgnoreCase(joiningName)) {
                otherName = KRIS;
            } else {
                return;
            }
            for (ServerPlayer online : CarpetServer.minecraft_server.getPlayerList().getPlayers()) {
                if (otherName.equalsIgnoreCase(online.getScoreboardName())) {
                    krisAndSusieTriggered = true;
                    CarpetServer.minecraft_server.getPlayerList().broadcastSystemMessage(SUSIE_LINE, false);
                    return;
                }
            }
        } catch (Exception exception) {
            LOGGER.debug("[Carpet Ice Addition] Deltarune easter egg check skipped", exception);
        }
    }
}
