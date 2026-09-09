# Logger List

## villagerEvents

Logs actual villager deaths, successful zombie-villager conversions, and successful lightning-to-witch conversions. The default option is `all`.

| Option | Events |
| - | - |
| `all` | Death, zombification, and witch conversion. |
| `death` | Actual deaths only. |
| `zombified` | Actual zombie-villager conversions only. |
| `witch` | Actual lightning-to-witch conversions only. |

`/log villagerEvents` is equivalent to `/log villagerEvents all`. Messages use `[VillagerEvents] <event> | <dimension> | X, Y, Z`. Baby, nitwit, unemployed adult, and employed adult villagers are distinguished; named villagers use `"Name" (Identity)`.

The vanilla dimensions are displayed as Overworld, Nether, and End. This is a server-only Logger; clients do not need Carpet Ice Addition. Vanilla text such as death messages and villager profession names is translated by each subscribing player's client in that client's own language; labels such as baby, nitwit, and unemployed, and the message templates follow the server's Carpet language, so a single message can mix languages when the client language differs from the server language. The server no longer downloads vanilla language files at startup; death output requires no network access and has no loading pause or failure downtime. Vanilla keys the client cannot translate (for example damage types or professions added by other mods) fall back to English text or the raw key.
