package com.ice2974.carpeticeaddition.command;

import com.ice2974.carpeticeaddition.translation.CarpetIceAdditionTranslations;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * 命令异常本地化与按需创建契约单元测试。
 * <p>
 * 本测试验证：
 * 1. 服务端翻译字典（CarpetIceAdditionTranslations）中命令异常条目的完整性与权威文本对齐；
 * 2. 各命令类的 {@code simpleError} 方法按需构造独立的异常与 {@link SimpleCommandExceptionType} 实例，
 *    证明已消除 {@code <clinit>} 单例静态缓存；
 * 3. 运行期跨语言热切换（如 {@code /carpet language zh_cn <-> en_us}）的实际动态表现由游戏内人工验收清单覆盖，
 *    避免在无头单元测试中操作 JVM 全局静态 Carpet 状态引发污染或环境依赖脆弱。
 */
class CommandExceptionTranslationTest {

    private static final String WEAKLOAD_REMOVED_FAILURE = "command.carpet-ice-addition.weakload.removed.failure";
    private static final String WEAKLOAD_ADDED_FAILURE = "command.carpet-ice-addition.weakload.added.failure";
    private static final String WEAKLOAD_SAVE_FAILED = "command.carpet-ice-addition.weakload.error.config_save_failed";
    private static final String WEAKLOAD_CONFIG_PROTECTED = "command.carpet-ice-addition.weakload.error.config_protected";
    private static final String WEAKLOAD_TICKET_APPLY_FAILED = "command.carpet-ice-addition.weakload.error.ticket_apply_failed";
    private static final String KILLITEM_PLAYER_ONLY = "command.carpet-ice-addition.killitem.error.player_only";
    private static final String KILLITEM_SAVE_FAILED = "command.carpet-ice-addition.killitem.error.config_save_failed";
    private static final String MACHINE_STATUS_SAVE_FAILED = "command.carpet-ice-addition.machine_status.error.config_save_failed";

    @Test
    @DisplayName("验证服务端翻译字典包含全部异常键且与权威文本完全一致")
    void testTranslationDictionaryEntries() {
        Map<String, String> zh = CarpetIceAdditionTranslations.get("zh_cn");
        assertNotNull(zh, "中文翻译字典不应为空");
        assertEquals("没有移除任何弱加载，这些区块未被弱加载或无效", zh.get(WEAKLOAD_REMOVED_FAILURE));
        assertEquals("没有区块被弱加载，这些区块已被弱加载或无效", zh.get(WEAKLOAD_ADDED_FAILURE));
        assertEquals("弱加载数据保存失败，请检查服务器日志", zh.get(WEAKLOAD_SAVE_FAILED));
        assertEquals("弱加载数据已因存储故障进入保护模式，请检查服务器日志并在修复存档文件后重启服务器", zh.get(WEAKLOAD_CONFIG_PROTECTED));
        assertEquals("弱加载票据应用失败，本次变更已回滚，请检查服务器日志", zh.get(WEAKLOAD_TICKET_APPLY_FAILED));
        assertEquals("只有玩家可以执行此命令", zh.get(KILLITEM_PLAYER_ONLY));
        assertEquals("保存 /killitem 配置失败，请查看服务器日志", zh.get(KILLITEM_SAVE_FAILED));
        assertEquals("保存 /machineStatus 数据失败，请查看服务器日志", zh.get(MACHINE_STATUS_SAVE_FAILED));

        Map<String, String> en = CarpetIceAdditionTranslations.get("en_us");
        assertNotNull(en, "英文翻译字典不应为空");
        assertEquals("No weak loading was removed. Chunks are either not weak loaded or invalid", en.get(WEAKLOAD_REMOVED_FAILURE));
        assertEquals("Nothing was weak loaded. Chunks are either already weak loaded or invalid", en.get(WEAKLOAD_ADDED_FAILURE));
        assertEquals("Failed to save /weakload data. Check the server log", en.get(WEAKLOAD_SAVE_FAILED));
        assertEquals("Weak load data is in protection mode after a storage failure. Check the server log and restart the server after fixing the world files", en.get(WEAKLOAD_CONFIG_PROTECTED));
        assertEquals("Failed to apply weak load tickets and the change was rolled back. Check the server log", en.get(WEAKLOAD_TICKET_APPLY_FAILED));
        assertEquals("Only players can run this command", en.get(KILLITEM_PLAYER_ONLY));
        assertEquals("Failed to save the /killitem config. Check the server log", en.get(KILLITEM_SAVE_FAILED));
        assertEquals("Failed to save /machineStatus data. Check the server log", en.get(MACHINE_STATUS_SAVE_FAILED));
    }

    @Test
    @DisplayName("验证 WeakloadCommand.simpleError 按需动态创建异常且无静态单例缓存")
    void testWeakloadCommandOnDemandCreation() {
        CommandSyntaxException ex1 = WeakloadCommand.simpleError(WEAKLOAD_REMOVED_FAILURE);
        CommandSyntaxException ex2 = WeakloadCommand.simpleError(WEAKLOAD_REMOVED_FAILURE);

        assertNotNull(ex1);
        assertNotNull(ex2);
        assertInstanceOf(SimpleCommandExceptionType.class, ex1.getType());
        assertInstanceOf(SimpleCommandExceptionType.class, ex2.getType());

        // 验证每次调用均为独立新实例，证明已彻底消除类加载期单例缓存
        assertNotSame(ex1, ex2);
        assertNotSame(ex1.getType(), ex2.getType());
    }

    @Test
    @DisplayName("验证 KillItemCommand.simpleError 按需动态创建异常且无静态单例缓存")
    void testKillItemCommandOnDemandCreation() {
        CommandSyntaxException ex1 = KillItemCommand.simpleError(KILLITEM_PLAYER_ONLY);
        CommandSyntaxException ex2 = KillItemCommand.simpleError(KILLITEM_PLAYER_ONLY);

        assertNotNull(ex1);
        assertNotNull(ex2);
        assertInstanceOf(SimpleCommandExceptionType.class, ex1.getType());
        assertInstanceOf(SimpleCommandExceptionType.class, ex2.getType());

        assertNotSame(ex1, ex2);
        assertNotSame(ex1.getType(), ex2.getType());
    }

    @Test
    @DisplayName("验证 MachineStatusCommand.simpleError 按需动态创建异常且无静态单例缓存")
    void testMachineStatusCommandOnDemandCreation() {
        CommandSyntaxException ex1 = MachineStatusCommand.simpleError(MACHINE_STATUS_SAVE_FAILED);
        CommandSyntaxException ex2 = MachineStatusCommand.simpleError(MACHINE_STATUS_SAVE_FAILED);

        assertNotNull(ex1);
        assertNotNull(ex2);
        assertInstanceOf(SimpleCommandExceptionType.class, ex1.getType());
        assertInstanceOf(SimpleCommandExceptionType.class, ex2.getType());

        assertNotSame(ex1, ex2);
        assertNotSame(ex1.getType(), ex2.getType());
    }
}
