# 记录器列表

## villagerEvents

记录村民真正死亡、成功感染为僵尸村民，以及成功因雷击转化为女巫。默认选项为 `all`。

| 选项 | 内容 |
| --- | --- |
| `all` | 死亡、僵尸化和女巫化。 |
| `death` | 仅真正死亡。 |
| `zombified` | 仅实际转化为僵尸村民。 |
| `witch` | 仅实际因雷击转化为女巫。 |

- `/log villagerEvents` 与 `/log villagerEvents all` 等价。
- 消息格式为 `[VillagerEvents] <事件描述> | <维度> | X, Y, Z`。
- 幼年、傻子、无业成年和有职业成年村民会区分显示，命名村民则使用 `“名称”（身份）`。
