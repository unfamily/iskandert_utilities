---
navigation:
  title: Timer intelligente
  icon: iska_utils:smart_timer
  parent: hubs/world_and_machines.md
  position: 53
item_ids:
  - iska_utils:smart_timer
categories:
  - World and machines
---
# Timer intelligente

<ItemImage id="iska_utils:smart_timer" />

## Scopo

Il Timer intelligente è una **sorgente redstone ripetuta**. Alterna tra:

1. **Cooldown** — nessun output (il blocco è **spento**).
2. **Impulso** — output redstone a **forza 15** per una durata impostata.

Ottieni una serie di impulsi con una pausa tra uno e l'altro, senza costruire enormi orologi a hopper.

## GUI — due timer

Apri il blocco per regolare **due durate indipendenti** (mostrate in ore, minuti, secondi, più tick residui):

| Etichetta in gioco | Significato |
| ------------- | ------- |
| **Redstone off for:** | Tempo in cui il blocco resta **spento** tra gli impulsi — è il **cooldown** prima della fase **accesa** successiva. |
| **Redstone on for:** | Quanto dura ogni **impulso** **acceso** (forza redstone piena). |

**Predefiniti** (se non modificati): circa **5 secondi** di cooldown, **3 secondi** di impulso (il server usa i tick internamente).

### Regolare i valori

- Righe di pulsanti **+ / −** incrementano per **ore, minuti, secondi** o passi fini (**10 tick / 5 tick** — 0,5 s / 0,25 s a 20 TPS).
- Ogni valore mostra anche i **tick totali** per build precise.
- Durata minima imposta dal server: **5 tick** per fase.

Mentre l'impulso è **attivo**, la modalità redstone viene ignorata — la fase ON termina sempre per intero.

## Output redstone

- Il blocco **emette potenza forte** quando è «acceso»: i comparator vedono **15** dallo stato alimentato.
- Quando è «spento», l'output è **0**.

## Modalità controllo redstone (accanto al pulsante chiudi)

Un'icona piccola cicla **quando il cooldown può avanzare** (la pausa tra impulsi). **Click sinistro** avanti; **destro** indietro.

| Icona (suggerimento) | Modalità | Comportamento durante il cooldown |
| ----------- | ---- | ------------------------- |
| Polvere da sparo | **Ignora redstone** | Il cooldown **avanza sempre**; il timer non si ferma mai. |
| Polvere redstone | **Low** | Il cooldown avanza solo **senza** potenza redstone adiacente (predefinito). |
| Stile torcia redstone | **High** | Il cooldown avanza solo mentre il blocco **è** alimentato dai vicini. |
| Barriera | **Disabled** | Il cooldown **non** avanza — timer congelato finché non cambi modalità. |

Durante l'**impulso** (fase ON), questo gating non si applica; la lunghezza dell'output è fissata dalla **durata segnale**.

## Suggerimenti

- Usa **Low** così una leva può **mettere in pausa** l'orologio quando è alimentata (inverti il cablaggio se ti serve «gira quando alimentato»).
- Usa **Ignore** per un orologio che deve girare indipendentemente da polvere o leve vicine.
- Abbina **comparator** o **blocchi opachi** per diramare i segnali senza caricare la stessa faccia due volte.
