# Logger List

## villagerEvents

Logs actual villager deaths, successful zombie-villager conversions, and successful lightning-to-witch conversions. The default option is `all`.

| Option | Events |
| - | - |
| `all` | Death, zombification, and witch conversion. |
| `death` | Actual deaths only. |
| `zombified` | Actual zombie-villager conversions only. |
| `witch` | Actual lightning-to-witch conversions only. |

- `/log villagerEvents` is equivalent to `/log villagerEvents all`.
- Messages use `[VillagerEvents] <event> | <dimension> | X, Y, Z`.
- Baby, nitwit, unemployed adult, and employed adult villagers are distinguished, named villagers use `"Name" (Identity)`.
