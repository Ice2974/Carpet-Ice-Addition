package com.ice2974.carpeticeaddition.command;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * KillItemCommand 的玩家所在世界获取（版本边界助手，1.21.1-1.21.5 形态）。
 * Yarn getEntityWorld() 在 1.21.1-1.21.5 的 officialMojangMappings 名为 getCommandSenderWorld()，
 * 1.21.6 起名为 level()；KillItemCommand 主文件经本助手取得该调用，按版本档位提供实现。
 */
public final class PlayerWorlds {
    private PlayerWorlds() {
    }

    public static ServerLevel serverLevel(ServerPlayer player) {
        return (ServerLevel) player.getCommandSenderWorld();
    }
}
