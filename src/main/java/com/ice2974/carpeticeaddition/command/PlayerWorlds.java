//#if MC<12111
//$$ package com.ice2974.carpeticeaddition.command;

//$$ import net.minecraft.server.level.ServerLevel;
//$$ import net.minecraft.server.level.ServerPlayer;

//$$ /**
 //$$ * KillItemCommand 的玩家所在世界获取（版本边界助手，1.21.1-1.21.10；
 //$$ * Phase 5 起由 preprocess 宏按版本激活，见 docs/refactor-phase5-verification.md §4.1）。
 //$$ */
//$$ public final class PlayerWorlds {
    //$$ private PlayerWorlds() {
    //$$ }

    //$$ public static ServerLevel serverLevel(ServerPlayer player) {
//#if MC>=12106
        //$$ return (ServerLevel) player.level();
//#else
        //$$ return (ServerLevel) player.getCommandSenderWorld();
//#endif
    //$$ }
//$$ }
//#endif
