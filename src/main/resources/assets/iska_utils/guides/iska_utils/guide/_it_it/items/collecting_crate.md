---
navigation:
  title: Cassetto raccoglitor
  icon: iska_utils:collecting_crate
  parent: hubs/world_and_machines.md
  position: 46
item_ids:
  - iska_utils:collecting_crate
categories:
  - World and machines
---
# Cassetto raccoglitor

<ItemImage id="iska_utils:collecting_crate" />

## Scopo

Aspira **entità oggetto** e **sfere di esperienza** in un parallelepipedo attorno al blocco. Conserva gli oggetti in un inventario interno e l’XP come **fluido esperienza** (compatibile con i tag fluido esperienza). Ideale sotto farm **Mob Reaper** o in qualsiasi camera di uccisione.

## Area di raccolta

- Il box si estende a **sinistra**, **destra**, **su** e **dietro** rispetto all’orientamento del blocco (non davanti alla faccia del cassetto).
- Un **Range Module** nello slot modulo aumenta le estensioni massime (impila moduli fino al limite). Vedi **Moduli** → **Range Module**.
- **Anteprima** attiva/disattiva un contorno lato client del box attivo.

Regola le estensioni con i pulsanti **+ / −** (Su, Sinistra, Destra, Dietro). Modificatori:

- **Click sinistro**: +1  
- **Click destro**: −1  
- **Shift**: ±10  
- **Alt / Ctrl**: ±5  

## Modalità raccolta

Cicla con il pulsante modalità:

- **XP e oggetti** (predefinito)
- **Solo XP**
- **Solo oggetti**

## Serbatoio XP

- L’XP è immagazzinata come fluido; la GUI mostra i **livelli immagazzinati**.
- **Raccogli tutta l’XP sul giocatore** — preleva l’XP immagazzinata verso di te.
- **Deposita tutta l’XP dal giocatore** — svuota la tua XP nel serbatoio (rispettando la capacità).

## Stoccaggio e automazione

- Handler **oggetti** e **fluidi** sul blocco per tramogge e tubi.

## Redstone

Stesse modalità delle altre macchine: **Ignore**, **Low**, **High**, **Disabled**. Quando disattivata o bloccata, la raccolta si ferma.

## Suggerimenti

- Posiziona il cassetto **dietro** la camera di kill rispetto all’orientamento così il box copre la zona dei drop.
- Usa **solo oggetti** se l’XP va convogliata altrove; usa **solo XP** per farm di esperienza senza ingombro di oggetti.
