package com.ice2974.carpeticeaddition.translation;

import java.util.Map;

public final class CarpetIceAdditionTranslations {
    private CarpetIceAdditionTranslations() {
    }

    public static Map<String, String> get(String lang) {
        if (lang != null && lang.toLowerCase().startsWith("zh")) {
            return Map.ofEntries(
                    Map.entry("carpet.category.carpetIceAddition", "Ice"),
                    Map.entry("carpet.rule.safeScaffoldingBreak.name", "\u811a\u624b\u67b6\u9632\u8bef\u89e6"),
                    Map.entry("carpet.rule.safeScaffoldingBreak.desc", "\u53ea\u6709\u4e3b\u624b\u6301\u811a\u624b\u67b6\u6216\u4e3b\u624b\u4e3a\u7a7a\u65f6\uff0c\u73a9\u5bb6\u624d\u80fd\u7834\u574f\u811a\u624b\u67b6\uff0c\u9632\u6b62\u8bef\u62c6\u3002"),
                    Map.entry("carpet.rule.crafterStopsWhenOutputBlocked.name", "\u5408\u6210\u5668\u8f93\u51fa\u963b\u585e\u65f6\u505c\u6b62\u5408\u6210"),
                    Map.entry("carpet.rule.crafterStopsWhenOutputBlocked.desc", "\u5f53\u5408\u6210\u5668\u524d\u65b9\u662f\u53ef\u81ea\u52a8\u63a5\u6536\u4ea7\u7269\u7684\u6709\u6548\u5bb9\u5668\uff0c\u4e14\u65e0\u6cd5\u5b8c\u6574\u63a5\u6536\u672c\u6b21\u4e3b\u4ea7\u7269\u4e0e\u914d\u65b9\u4f59\u7559\u7269\u65f6\uff0c\u53d6\u6d88\u672c\u6b21\u5408\u6210\uff0c\u907f\u514d\u4ea7\u7269\u55b7\u51fa\u3002"),
                    Map.entry("carpet.rule.recordWorldEventFix.name", "\u5531\u7247\u4e16\u754c\u4e8b\u4ef6\u65f6\u5e8f\u4fee\u590d"),
                    Map.entry("carpet.rule.recordWorldEventFix.desc", "\u4fee\u590d\u4e86\u5c06\u5531\u7247\u5feb\u901f\u653e\u5165\u5531\u7247\u673a\u540e\u53c8\u8fc5\u901f\u53d6\u51fa\u65f6\uff0c\u97f3\u4e50\u4ecd\u53ef\u80fd\u7ee7\u7eed\u64ad\u653e\uff0c\u4e14\u591a\u4e2a\u5531\u7247\u97f3\u9891\u53ef\u80fd\u91cd\u53e0\u7684\u95ee\u9898\uff0c\u8be6\u89c1 MC-112245\u3002"),
                    Map.entry("carpet.rule.spawnersIgnoreInvisiblePlayers.name", "\u5237\u602a\u7b3c\u5ffd\u7565\u9690\u8eab\u73a9\u5bb6"),
                    Map.entry("carpet.rule.spawnersIgnoreInvisiblePlayers.desc", "\u666e\u901a\u5237\u602a\u7b3c\u3001\u8bd5\u70bc\u5237\u602a\u7b3c\u548c\u4e0d\u7965\u8bd5\u70bc\u5237\u602a\u7b3c\u5728\u5224\u5b9a\u9644\u8fd1\u73a9\u5bb6\u65f6\u4f1a\u5ffd\u7565\u9690\u8eab\u73a9\u5bb6\u3002"),
                    Map.entry("carpet.rule.disableKelpNaturalGrowth.name", "\u7981\u7528\u6d77\u5e26\u81ea\u7136\u751f\u957f"),
                    Map.entry("carpet.rule.disableKelpNaturalGrowth.desc", "\u7981\u7528\u6d77\u5e26\u7531\u968f\u673a\u523b\u89e6\u53d1\u7684\u81ea\u7136\u751f\u957f\uff0c\u4e0d\u5f71\u54cd\u9aa8\u7c89\u50ac\u719f\u3002"),
                    Map.entry("carpet.rule.canMineBuddingAmethyst.name", "\u53ef\u91c7\u96c6\u7d2b\u6c34\u6676\u6bcd\u5ca9"),
                    Map.entry("carpet.rule.canMineBuddingAmethyst.desc", "\u4f7f\u7528\u5e26\u6709\u7cbe\u51c6\u91c7\u96c6\u7684\u5de5\u5177\u53ef\u4ee5\u91c7\u96c6\u7d2b\u6c34\u6676\u6bcd\u5ca9\u3002"),
                    Map.entry("carpet.rule.fakePlayerIgnoreThornsDamage.name", "\u5047\u4eba\u514d\u75ab\u8346\u68d8\u53cd\u4f24"),
                    Map.entry("carpet.rule.fakePlayerIgnoreThornsDamage.desc", "\u5047\u4eba\u653b\u51fb\u5e26\u6709\u8346\u68d8\u9644\u9b54\u7684\u751f\u7269\u6216\u73a9\u5bb6\u65f6\uff0c\u4e0d\u4f1a\u53d7\u5230\u8346\u68d8\u9020\u6210\u7684\u4f24\u5bb3\u4e0e\u51fb\u9000\u3002"),
                    Map.entry("carpet.rule.botTabListNamePrefix.name", "\u5047\u4ebaTab\u680f\u540d\u79f0\u524d\u7f00"),
                    Map.entry("carpet.rule.botTabListNamePrefix.desc", "\u4e3aTab\u680f\u4e2d\u7684\u5047\u4eba\u6dfb\u52a0\u524d\u7f00\uff0c\u4f7f\u7528&\u6765\u8868\u793a\u6587\u5b57\u989c\u8272\u3002"),
                    Map.entry("carpet.rule.botTabListNameSuffix.name", "\u5047\u4ebaTab\u680f\u540d\u79f0\u540e\u7f00"),
                    Map.entry("carpet.rule.botTabListNameSuffix.desc", "\u4e3aTab\u680f\u4e2d\u7684\u5047\u4eba\u6dfb\u52a0\u540e\u7f00\uff0c\u4f7f\u7528&\u6765\u8868\u793a\u6587\u5b57\u989c\u8272\u3002"),
                    Map.entry("message.carpet-ice-addition.safe_scaffolding_break", "\u4f60\u5fc5\u987b\u624b\u6301\u811a\u624b\u67b6\u6216\u7a7a\u624b\u624d\u80fd\u7834\u574f\u811a\u624b\u67b6")
            );
        }
        return Map.ofEntries(
                Map.entry("carpet.category.carpetIceAddition", "Ice"),
                Map.entry("carpet.rule.safeScaffoldingBreak.desc", "Require holding scaffolding or an empty main hand to break scaffolding."),
                Map.entry("carpet.rule.crafterStopsWhenOutputBlocked.desc", "Prevent the crafter from crafting when its output points to a valid auto-accepting container that cannot fully accept the crafted result and recipe remainders."),
                Map.entry("carpet.rule.recordWorldEventFix.desc", "Fixes the issue where a music disc can keep playing after being quickly inserted into and removed from a jukebox, which may also cause overlapping disc audio. See MC-112245."),
                Map.entry("carpet.rule.spawnersIgnoreInvisiblePlayers.desc", "Normal spawners, trial spawners, and ominous trial spawners ignore invisible players when checking nearby players."),
                Map.entry("carpet.rule.disableKelpNaturalGrowth.desc", "Disable kelp natural growth from random ticks while keeping bonemeal growth unchanged."),
                Map.entry("carpet.rule.canMineBuddingAmethyst.desc", "Budding amethyst can be collected with a Silk Touch tool."),
                Map.entry("carpet.rule.fakePlayerIgnoreThornsDamage.desc", "Bots will not take damage or knockback from Thorns when attacking entities or players equipped with Thorns."),
                Map.entry("carpet.rule.botTabListNamePrefix.desc", "Add a prefix to the Bot in the TabList, using & to represent text color."),
                Map.entry("carpet.rule.botTabListNameSuffix.desc", "Add a suffix to the Bot in the TabList, using & to represent text color."),
                Map.entry("message.carpet-ice-addition.safe_scaffolding_break", "Hold scaffolding or empty your main hand to break scaffolding.")
        );
    }
}
