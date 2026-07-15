> Doc Version: `v2.7.0`

# Logger List

## villagerEvents

Logs actual villager deaths, successful zombie-villager conversions, and successful lightning-to-witch conversions. The default option is `all`.

| Option | Events |
| --- | --- |
| `all` | Death, zombification, and witch conversion. |
| `death` | Actual deaths only. |
| `zombified` | Actual zombie-villager conversions only. |
| `witch` | Actual lightning-to-witch conversions only. |

`/log villagerEvents` is equivalent to `/log villagerEvents all`. Messages use `[VillagerEvents] <event> | <dimension> | X, Y, Z`. Baby, nitwit, unemployed adult, and employed adult villagers are distinguished; named villagers use `"Name" (Identity)`.

The vanilla dimensions are displayed as Overworld, Nether, and End. This is a server-only Logger; clients do not need Carpet Ice Addition. Death output pauses while server language resources load; once ready, only later deaths are logged. If the required language resource fails to load, death output remains unavailable for that server session; restore network access or provide a valid cache and restart the server. Mixed-language client translations are never sent.
