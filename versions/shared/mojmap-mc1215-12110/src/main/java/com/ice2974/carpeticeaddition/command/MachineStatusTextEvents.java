package com.ice2974.carpeticeaddition.command;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/**
 * machineStatus 命令 info 按钮的点击/悬停事件构造（版本边界助手，1.21.5-1.21.10 形态）。
 * 1.21.5 起 ClickEvent 由 Action 枚举构造改为 record 实现类（RunCommand）、
 * HoverEvent 改为 ShowText 嵌套类；命令主文件经本助手构造，按版本档位提供实现。
 */
public final class MachineStatusTextEvents {
    private MachineStatusTextEvents() {
    }

    public static ClickEvent runCommand(String command) {
        return new ClickEvent.RunCommand(command);
    }

    public static HoverEvent showText(Component text) {
        return new HoverEvent.ShowText(text);
    }
}
