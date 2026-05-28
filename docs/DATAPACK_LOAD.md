# Datapack `load/` paths (dev reference)

IskaUtils JSON lives under `data/<namespace>/load/`. Each subsystem has its own subfolder (or flat `load/*.json` routed by `"type"`).

Reload on server: `/reload` or `/iska_utils_debug reload`.

| Subfolder | JSON `type` | Purpose |
|-----------|-------------|---------|
| `iska_utils_command_items/` | `iska_utils:command_item` | Command items |
| `iska_utils_plates/` | `iska_utils:plates` | Potion plates |
| `iska_utils_structures_monouse/` | `iska_utils:structure_monouse_item` | Structure monouse items |
| `iska_utils_structure_definitions/` | `iska_utils:structure` | Server structure definitions |
| `iska_utils_shop/` | `iska_utils:shop_currency`, `shop_category`, `shop_entry` | Shop |
| `iska_utils_macros/` | `iska_utils:commands_macro` | Command macros |
| `iska_utils_stage_actions/` | `iska_utils:stage_actions` | Stage actions |
| `iska_utils_stage_items/` | `iska_utils:stage_item` | Stage item restrictions |
| `iska_utils_suspicious_delivery/` | `iska_utils:suspicious_delivery` | **Suspicious Delivery** loot table (weighted `entries` on use) |

Factory recipes use `data/<namespace>/recipe/factory/` (not under `load/`).

## Suspicious Delivery

- Default: `data/iska_utils/load/iska_utils_suspicious_delivery/suspicious_delivery.json`
- Alternative: `data/<namespace>/load/<any>.json` with `"type": "iska_utils:suspicious_delivery"`
- Only **one** definition is active at runtime (last matching file in load order replaces the whole table).

Wiki: [Suspicious Delivery](https://github.com/unfamily/Iskandert_Utilities/wiki/Suspicious-Delivery) (packdev).

**Note:** The old folder name `iska_utils_obtaining` is no longer scanned.

### Entry fields

| Field | Default | Description |
|-------|---------|-------------|
| `weight` | `0` | Relative weight (all entries, including hidden, count toward JEI % denominator) |
| `luck` | `0` | Signed threshold for fortune / unluck rerolls (see wiki) |
| `jeimode` | `show` | `show` \| `hidden` \| `mask` |
| `stages` | `[]` | Entry eligibility (same format as command items) |
| `stages_logic` | `AND` | `AND` \| `OR` \| `DEF_AND` \| `DEF_OR` |
| `do` | `[]` | Ordered actions (`execute`, `message`, `delay`, `drop`, `if`, per-action `stages`) |

### `jeimode` and JEI

- `show` — recipe in JEI; output slot cycles through all `drop` stacks in `do`
- `hidden` — no JEI row; weight still counts in percentages
- `mask` — JEI shows `iska_utils:suspicious_delivery_undefined`; real loot hidden

Percent: `100 × weight / sum(weights)` over **all** entries (1 decimal in tooltip).

### Luck reroll (Fortune / Unluck potions)

Net luck: `(LUCK amplifier+1) − (UNLUCK amplifier+1)` (0 if neither).

**Fortune** (`net ≥ 0`): tier1 `min(50, 30+net×5)%` reroll while `entry.luck < 0`; from `net ≥ 5` tier2 `min(100, 50+(net−4)×10)%`; from `net ≥ 10` forced reroll toward `luck ≥ 1000` then `≥ 10000` (max 64 attempts each, fallback if no entry reaches threshold).

**Unluck** (`net < 0`): symmetric with `effective = −net`, rerolling while `luck > 0` and toward `luck ≤ −1000` / `≤ −10000`.
