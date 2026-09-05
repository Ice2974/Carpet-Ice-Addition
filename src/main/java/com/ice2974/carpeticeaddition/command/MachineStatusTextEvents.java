//#if MC<12111
//$$ package com.ice2974.carpeticeaddition.command;

//$$ import net.minecraft.network.chat.ClickEvent;
//$$ import net.minecraft.network.chat.HoverEvent;
//$$ import net.minecraft.network.chat.Component;

//$$ /**
 //$$ * machineStatus 命令 info 按钮的点击/悬停事件构造（版本边界助手，1.21.1-1.21.10）。
 //$$ */
//$$ public final class MachineStatusTextEvents {
    //$$ private MachineStatusTextEvents() {
    //$$ }

    //$$ public static ClickEvent runCommand(String command) {
//#if MC>=12105
        //$$ return new ClickEvent.RunCommand(command);
//#else
        //$$ return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
//#endif
    //$$ }

    //$$ public static HoverEvent showText(Component text) {
//#if MC>=12105
        //$$ return new HoverEvent.ShowText(text);
//#else
        //$$ return new HoverEvent(HoverEvent.Action.SHOW_TEXT, text);
//#endif
    //$$ }
//$$ }
//#endif
