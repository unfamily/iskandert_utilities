---
navigation:
  title: Spawner entropizzato
  icon: iska_utils:entropic_spawner
  parent: hubs/world_and_machines.md
  position: 37
item_ids:
  - iska_utils:entropic_spawner
categories:
  - World and machines
---
# Spawner entropizzato

<ItemImage id="iska_utils:entropic_spawner" />

## A cosa serve

**Spawner di mob** automatizzato. Quando le condizioni sono soddisfatte evoca il tipo di mob configurato **sul blocco direttamente sopra** la macchina. A differenza di uno spawner vanilla, scegli il mob con un’**uova di spawn** e puoi potenziare tempi, quantità per ciclo e carburante con gli slot modulo nella GUI.

## Impostare il mob

Usa un’**uova di spawn** sul blocco (stessa interazione della configurazione di uno spawner vanilla). L’uova viene consumata salvo in creativa. Apri la GUI per confermare il mob selezionato e il conto alla rovescia al prossimo ciclo.

Con la macchina **attiva**, una piccola **anteprima rotante** del mob configurato appare dentro il blocco.

## Ciclo di spawn

1. Dopo aver impostato un mob, lo spawner attende un **intervallo casuale** prima di ogni ciclo.
2. A fine intervallo prova a generare **un mob**, più **un mob extra per Modulo produzione impilato** nello slot modulo.
3. Lo spawn **si mette in pausa** mentre ci sono **troppe entità viventi** ammassate sopra il blocco (limite server di sovraffollamento). Il timer può comunque avanzare a seconda della modalità redstone, ma non compaiono mob finché abbastanza mob se ne vanno.
4. Se sul mondo vale un **limite di spawn a vita**, la GUI indica quando la macchina l’ha raggiunto e lo spawn si ferma in modo permanente finché non sostituisci il blocco o il limite viene azzerato in altro modo.

I mob compaiono **centrati sopra** il blocco. Solo i blocchi con **collisione solida** impediscono il posizionamento; livello di luce e regole vanilla di spawn sono ignorati.

## Carburante entropico

Metti **Goccia di entropia** (o altro carburante entropico accettato dalla Tavoletta antica) nello **slot carburante**. Gli oggetti si convertono in un **buffer di carica interno** mostrato come percentuale sotto lo slot.

- Lo spawner **genera mob anche senza carica** — il carburante **non** è obbligatorio per la generazione.
- Con carica presente, il **conteggio tra i cicli di spawn è più veloce** (si cumula con la riduzione ritardo dell’Orologio entropico).
- Lo slot carburante fisico si può riempire a mano o con automazione mentre il buffer assorbe altra carica.

## Potenziamenti

| Slot | Oggetto | Effetto |
| ---- | ---- | ------ |
| Orologio | <ItemImage id="iska_utils:entropic_clock" /> **Orologio entropico** | Accorcia il ritardo tra i cicli di spawn (si cumula nello slot). Potenzia anche il **Temporal Overclocker** — vedi **Moduli** → **Orologio entropico**. |
| Modulo | <ItemImage id="iska_utils:production_module" /> **Modulo produzione** | Aggiunge **un mob in più** per ciclo di spawn per ogni modulo nello stack. |

Icone fantasma negli slot vuoti mostrano gli oggetti validi.

## GUI

| Controllo | Scopo |
| ------- | ------- |
| Centro | Nome del mob configurato e **tempo al prossimo spawn** (o messaggio limite a vita). |
| Colonna sinistra | Orologio entropico (in alto), Modulo produzione (al centro). |
| Slot carburante | Carburante entropico; etichetta percentuale sotto. |
| Pulsante **Redstone** (destra) | Cicla la modalità di controllo — click sinistro avanti, destro indietro. |
| **✕** | Chiudi |

## Redstone

Stessa famiglia delle altre macchine Iska, incluso **Pulse** su questo blocco:

| Modalità | Comportamento |
| ---- | --------- |
| **Ignore** | Funziona sempre quando le altre condizioni sono soddisfatte. |
| **Low** | Funziona solo **senza** segnale redstone. |
| **High** | Funziona solo **con** segnale redstone. |
| **Pulse** | Il timer avanza mentre alimentato; **un ciclo di spawn** parte sul **fronte di salita** redstone. |
| **Disabled** | Non genera mai mob. |

## Stati del blocco

- **Inattivo** — nessun mob configurato, redstone blocca l’operazione o limite a vita raggiunto.
- **Attivo** — pronto a contare e generare.
- **In spawn** — breve flash della texture superiore quando i mob vengono creati.

## Automazione

Slot modulo e carburante accettano **hopper e tubi oggetti**. Rompendo il blocco cadono moduli e carburante memorizzati.

## Suggerimenti

- Lascia **spazio libero** sul blocco direttamente sopra lo spawner e tienilo libero da blocchi solidi.
- Abbinalo a **Mob Reaper** e **Cassa raccoglitrice** per farm incustodite.
- Impila **Moduli produzione** per throughput; impila **Orologi entropici** per cicli più rapidi; carburante entropico opzionale accorcia ulteriormente il ritardo.
- Vedi **Moduli** → **Modulo produzione** e **Orologio entropico** per le pagine modulo condivise.
- Usa redstone **High** o **Pulse** per attivare le farm con pulsanti o clock.
