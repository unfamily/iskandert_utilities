---
navigation:
  title: Scanner e chip
  icon: iska_utils:scanner
  parent: hubs/tools_and_scanner.md
  position: 15
item_ids:
  - iska_utils:scanner
  - iska_utils:scanner_chip
  - iska_utils:scanner_chip_ores
  - iska_utils:scanner_chip_mobs
  - iska_utils:scanner_chip_spawners
  - iska_utils:scanner_chip_loot
  - iska_utils:scanner_chip_liquid
categories:
  - Useful tools
---
# Scanner e chip

<ItemImage id="iska_utils:scanner" />

## Scanner

- Evidenzia blocchi o mob corrispondenti per un tempo limitato; supporta gli Scanner Chip per memorizzare e trasferire i bersagli.

<ItemGrid>
  <ItemIcon id="iska_utils:scanner" />
  <ItemIcon id="iska_utils:scanner_chip" />
  <ItemIcon id="iska_utils:scanner_chip_ores" />
  <ItemIcon id="iska_utils:scanner_chip_mobs" />
  <ItemIcon id="iska_utils:scanner_chip_spawners" />
  <ItemIcon id="iska_utils:scanner_chip_loot" />
  <ItemIcon id="iska_utils:scanner_chip_liquid" />
</ItemGrid>

## Scanner Chip (vuoto)

- **Shift + usa su un blocco** per memorizzare un bersaglio blocco nel chip.
- Per trasferire il bersaglio del chip nello **Scanner**, tieni lo scanner nella **mano principale** e **usa il chip**.

## Scanner Chip (Minerali)

- Preimpostato per scansionare i **minerali**.
- **Shift + usa** per ciclare il filtro livello di scavo (mostrato in chat / tooltip).
- Tieni lo scanner nella **mano principale** e **usa il chip** per trasferire il bersaglio scansione minerali nello scanner.

## Scanner Chip (Mob)

- Preimpostato per scansionare **tutti i mob**.
- **Shift + usa** per ciclare le categorie di mob.
- Tieni lo scanner nella **mano principale** e **usa il chip** per trasferire il bersaglio scansione mob nello scanner.

## Scanner Chip (Spawner)

- Preimpostato per scansionare i **blocchi spawner** (mostri, trial e spawner correlati).
- **Shift + usa** per ciclare: tutti gli spawner, solo spawner mostri o solo spawner trial.
- Trasferimento allo scanner come gli altri chip specializzati.

## Scanner Chip (Loot)

- Preimpostato per trovare **contenitori con loot** (non aperti o con contenuto).
- **Shift + usa** per ciclare le modalità loot:
  1. Contenitori con loot (predefinito)
  2. Contenitori vuoti
  3. Contenitori già aperti che tengono ancora loot per te (se supportato da mod compatibili, es. Lootr)
- Rileva forzieri, barili, shulker, vasi decorati, contenitori moddati e blocchi **Lootr**.
- Le **entità** loot (es. cornici Lootr) usano marcatori billboard.
- I colori dei marcatori blocco differiscono per Lootr vs contenitori vanilla/mod.

## Scanner Chip (Liquido)

- Preimpostato per scansionare **tutti i fluidi** nel raggio.
- **Shift + usa su un blocco fluido** (o la faccia di un blocco adiacente al fluido) per filtrare solo quel fluido. Fluidi correnti e sorgente sono normalizzati (es. `minecraft:water`).
- **Shift + usa su qualsiasi altro blocco** per resettare il filtro a tutti i fluidi.
- Trasferimento allo scanner come gli altri chip specializzati.
