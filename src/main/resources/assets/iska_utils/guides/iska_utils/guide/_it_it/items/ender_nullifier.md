---
navigation:
  title: Nullificatore Ender
  icon: iska_utils:ender_nullifier
  parent: hubs/world_and_machines.md
  position: 22
item_ids:
  - iska_utils:ender_nullifier
categories:
  - World and machines
---
# Nullificatore Ender

<ItemImage id="iska_utils:ender_nullifier" />

## A cosa serve

Blocco che **annulla i teletrasporti** dentro un **raggio** cubico attorno a sé su ogni asse (impostato nella GUI).

Le entità nella lista di esclusione configurata (predefinita: `#c:bosses`) sono esenti e possono continuare a teletrasportarsi nella zona.

## GUI

Click destro sul blocco per aprire la GUI di controllo:

- **Raggio**: usa i pulsanti **-** e **+** per regolare il raggio di protezione. Sono mostrati valore attuale e massimo.
- **Bersaglio** (pulsante icona, lato destro): cicla il gruppo interessato. Click sinistro avanza, click destro torna indietro.
  - *Solo mob* (predefinito): interessa le entità non-giocatore.
  - *Solo giocatori*: interessa i giocatori in Sopravvivenza e Avventura.
  - *Mob e giocatori*: interessa entrambi i gruppi.
- **Modalità redstone** (pulsante icona): cicla Ignore, Low, High, Disabled.
  - *Ignore* (polvere da sparo): sempre attivo se abilitato manualmente, ignora il segnale redstone.
  - *Low*: attivo mentre il segnale redstone è **assente** (e abilitato manualmente).
  - *High*: attivo mentre il segnale redstone è **presente** (e abilitato manualmente).
  - *Disabled*: mai attivo.
- **Shift + click destro**: attiva/disattiva manualmente senza aprire la GUI (feedback nella action bar).
- **Show / Hide** (sotto il pulsante redstone): mostra/nasconde un bordo di anteprima intorno all'area interessata.
- **Slot Range Module** (in alto a sinistra): inserisci Range Module per aumentare il raggio massimo raggiungibile. Vedi **Moduli** → **Range Module**.

## Suggerimenti

- Posizionalo al centro dell'area da proteggere.
- Utile vicino a farm dove le entità che si teletrasportano rompono il contenimento.
- Alla piazzatura parte **attivo** (modalità manuale, abilitato manualmente).
- Vedi anche: [Nullificatore del vagabondo](wander_nullifier.md), [Nullificatore delle Anime](soul_nullifier.md).
