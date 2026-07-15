> Doc Version: `v2.6.2`

# Logger List

## villagerEvents

Logs actual villager deaths, successful zombie-villager conversions, and successful lightning-to-witch conversions. The default option is `all`.

| Option | Events |
| --- | --- |
| `all` | Death, zombification, and witch conversion. |
| `death` | Actual deaths only. |
| `zombified` | Actual zombie-villager conversions only. |
| `witch` | Actual lightning-to-witch conversions only. |

`/log villagerEvents` is equivalent to `/log villagerEvents all`. Messages use `[VillagerEvents] <event> | <dimension> | X, Y, Z`. Baby, nitwit, unemployed adult, and employed adult villagers are distinguished; named villagers use `“Name” (Identity)`.

The vanilla dimensions are displayed as Overworld, Nether, and End. This is a server-only Logger; clients do not need Carpet Ice Addition. Death messages are briefly queued while server language resources load and are never sent as mixed-language client translations.
