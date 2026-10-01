---
navigation:
  title: Mietitrice di mob
  icon: iska_utils:mob_reaper
  parent: hubs/world_and_machines.md
  position: 45
item_ids:
  - iska_utils:mob_reaper
categories:
  - World and machines
---
# Mietitrice di mob

<ItemImage id="iska_utils:mob_reaper" />

## Scopo

La Mietitrice di mob è una **macchina da combattimento automatica**. A intervalli fissi danneggia le entità viventi nelle vicinanze usando il credito uccisione di un **fake player** (loot table, uccisioni giocatore, enchantment). Abbinala a una **Collecting Crate** a valle per aspirare drop ed XP.

## Posizionamento

- **Pavimento**: guarda verso il giocatore al piazzamento; **accovacciato + piazza** inverte la direzione.
- **Parete**: clic su una faccia laterale; `FACING` punta **lontano dalla parete** (direzione del colpo). Alcuni server disabilitano il piazzamento a parete.
- **Piastra vector**: su un **Vector Block** sotto, la mietitrice **si muove con la piastra** e non cade come oggetto quando la piastra viene rotta.

La lama gira e infligge danno solo mentre è **attiva** (vedi redstone).

## GUI

Le icone fantasma mostrano i moduli validi quando uno slot è vuoto. Statistiche live: **danno**, **% decapitazione**, **fortuna**, **moltiplicatore XP** e **letale attivo** quando applicabile.

| Controllo | Azione |
| ------- | ------ |
| **Tipo bersaglio** (lato destro) | Cicla: **Solo mob** → **Mob e giocatori** → **Solo giocatori**. Click sinistro avanti, destro indietro. |
| **Filtro età** (estrema destra) | Cicla: **Tutte le età** → **Solo adulti** → **Solo cuccioli**. Salta mob piccoli o adulti in base al filtro. |
| **Modalità redstone** | Stessa famiglia delle altre macchine: **Ignore**, **Low**, **High**, **Disabled** (click sinistro / destro). |
| **✕** | Chiudi |

## Moduli

Usa **un solo tipo di modulo danno** alla volta: **Normale** (impilabile) **oppure** **Letale** (singolo). Gli altri upgrade si impilano indipendentemente.

| Modulo | Effetto |
| ------ | ------ |
| Danno normale | Danno base + bonus per modulo impilato. |
| Danno letale | Danno fisso molto alto; sostituisce i moduli danno normale. |
| Enchant | Contiene un'**arma**; gli enchantment si applicano al danno e agli effetti post-attacco. |
| Decapitazione | Probabilità extra di drop di **teschi** per livello modulo. |
| Fortuna | Applica **Fortuna** al fake player per i tiri di loot. |
| Esperienza | Moltiplicatore su **sfere XP bonus** quando un bersaglio muore. |

<ItemGrid>
  <ItemIcon id="iska_utils:normal_damage_module" />
  <ItemIcon id="iska_utils:lethal_damage_module" />
  <ItemIcon id="iska_utils:enchant_module" />
  <ItemIcon id="iska_utils:beheading_module" />
  <ItemIcon id="iska_utils:luck_module" />
  <ItemIcon id="iska_utils:experience_module" />
</ItemGrid>

L'inventario moduli è compatibile con **hopper / tubi**. Vedi **Moduli** → **Moduli per la Mietitrice di mob**.

## Redstone

- **Ignore** (predefinito): attacca sempre quando il cooldown lo consente.
- **Low**: attacca quando **non** c'è segnale redstone.
- **High**: attacca solo **con** redstone.
- **Disabled**: non attacca mai.

## Suggerimenti

- Usa **Solo mob** sui server a meno che non voglia trappole PvP.
- **Modulo enchant** + looting / affilatezza potenzia le farm; **Collecting Crate** nel percorso del loot raccoglie sfere e oggetti.
- Le mietitrici a parete funzionano bene negli elevatori di mob con piastre vector.
