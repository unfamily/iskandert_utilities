---
navigation:
  title: Pattern Crafter
  icon: iska_utils:improved_pattern_crafter
  parent: hubs/world_and_machines.md
  position: 37
item_ids:
  - iska_utils:pattern_crafter
  - iska_utils:improved_pattern_crafter
  - iska_utils:pattern_crafter_improver
categories:
  - World and machines
---
# Pattern Crafter

<ItemImage id="iska_utils:improved_pattern_crafter" />

## A cosa serve

Il **Pattern Crafter** (e il **Pattern Crafter Migliorato**) è una macchina da craft automatica guidata da **pattern a lettere**. Assegni lettere a una griglia 3×3, associ ogni lettera a un **filtro oggetto** (una «variabile»), inserisci gli ingredienti nell'inventario della macchina e il crafter ripete i craft delle ricette corrispondenti usando RF/FE.

La variante **Migliorato** supporta un budget upgrade maggiore e più lavoro concorrente; entrambe condividono la stessa GUI e gli stessi controlli.

## Craft

### Pattern Crafter

Cornice di cobblestone e assi attorno a un **Crafter** vanilla (due pattern speculari).

<RecipesFor id="iska_utils:pattern_crafter" />

### Pattern Crafter Migliorato

Stessa cornice di gemme dell'Improver, con un **Pattern Crafter** al centro (gemme: angoli quartz / diamante / prismarine; lati lapis / inchiostro luminoso).

<RecipesFor id="iska_utils:improved_pattern_crafter" />

### Pattern Crafter Improver

Stessa cornice di gemme, ma al centro c'è un `#minecraft:planks` qualsiasi. Serve solo per l'upgrade in-place sotto.

<RecipesFor id="iska_utils:pattern_crafter_improver" />

## Upgrade in-place

Crea un **Pattern Crafter Improver** (ricetta sopra). **Shift+click destro** su un Pattern Crafter normale piazzato con l'Improver per convertirlo in Migliorato **senza perdere** inventario, pattern, filtri, energia o impostazioni. L'Improver viene consumato.

## Pattern

- La colonna sinistra mostra il **pattern corrente**: controlli modalità, browser pattern, griglia lettere 3×3, **Salva** / **Annulla** e **Segna input**.
- Il **Migliorato** parte con **6** slot pattern (configurabile). Ogni **Logic Module** aggiunge **+1** pattern (fino al massimo configurato).
- Ogni cella è una lettera (o vuota). Le lettere si collegano alle **variabili** nell'area filtri.
- Cicla una cella con click sinistro/destro; **Shift+click** la svuota. Puoi anche piazzare un oggetto su una cella per assegnare la lettera corrispondente dalle tue variabili.
- Passa tra i pattern memorizzati con le frecce / etichetta pattern. **Shift+click** sull'etichetta pattern azzera il pattern corrente.
- **Salva** scrive le modifiche pendenti della griglia nello slot pattern selezionato; **Annulla** scarta le modifiche non salvate.

## Variabili (filtri)

Sopra l'inventario macchina ci sono gli slot **variabile** (paginati quando sblocchi molte chiavi):

- Il **Migliorato** parte con **una pagina** da **18** chiavi variabile.
- Ogni **Logic Module** sblocca **una pagina extra** (+18 chiavi). Con il massimo predefinito di **4** Logic Module puoi arrivare a **5 pagine** (90 chiavi), limitato dalla config.
- Piccoli pulsanti **lettera** sopra/sotto ogni slot sbloccano e ciclano la lettera per quella variabile.
- Bloccata (senza lettera), il pulsante grande è inattivo.
- Sbloccata, clic sul pulsante grande apre l'**editor inline** per la stringa filtro di quella variabile.
- I filtri usano lo stesso linguaggio **Valid Keys** dei filtri Deep Drawer (id oggetto, id mod, tag, NBT, macro). Apri **Valid Keys** nell'editor per l'elenco completo.
- I pulsanti freccia paginano le variabili quando i Logic Module sbloccano più di una schermata.

## Modalità craft

Cicla **Entrambi / Solo shaped / Solo shapeless** per limitare quali forme di ricetta la macchina accetta.

## Smistamento risultati (output ricorsivi)

Controlla dove vanno i **risultati** del craft:

| Modalità | Comportamento |
| ---- | -------- |
| **Res. Eject** | I risultati vanno solo negli slot **output** della macchina. Il craft si ferma se non c'è spazio. |
| **Res. Keep** | I risultati si fondono prima negli **input**; l'overflow va in output. Si ferma se qualcosa non entra. |
| **Res. Smart** | Fonde quanto più possibile del risultato negli input; espelle il resto. Si ferma se una parte andrebbe in overflow. |

## Ingredienti non usati (resti)

Quando un craft lascia input residui (o stack parziali):

| Modalità | Comportamento |
| ---- | -------- |
| **Ing. Keep** | I resti provano prima gli slot **input**, poi gli output. Il craft è bloccato se non entrano. |
| **Ing. Eject** | I resti provano prima gli slot **output**. Il craft è bloccato se non entrano. |

## Protezione strumenti

- **Tools: Protect** — gli strumenti che si romperebbero vengono espulsi in output invece di essere consumati.
- **Tools: Allow Break** — gli strumenti possono essere usati fino a rompersi.

## Output vietati

**Forbidden outputs** apre un elenco di risultati di craft che la macchina non deve mai produrre. Modifica le voci con lo stesso editor filtri (Valid Keys, varianti, applica/cancella). Utile per bloccare sottoprodotti o percorsi ricetta indesiderati.

## Segna input / Segna output

Stessa idea dei segni «imposta inventario» del Structure Placer:

- **Segna input** — ricorda quali oggetti vanno in ogni slot input della macchina (fantasma su slot vuoti). Click / Shift / varianti Ctrl-Alt cancellano o aggiornano i segni.
- **Segna output** — uguale per la griglia output paginata. Gli slot segnati vuoti mostrano anteprime fantasma.
- Doppio click su uno slot segnato vuoto cancella il segno di quello slot.

I segni guidano cosa hopper e giocatori devono mettere dove; non sostituiscono i filtri a lettere.

## Inventario e automazione

- **Input macchina** (9×3): ingredienti per il craft; gli hopper possono inserire.
- **Output** (3×3, paginato): risultati craft e resti/strumenti espulsi.
- **Inventario giocatore** in fondo alla GUI.
- Rompi il blocco per far cadere contenuti e moduli.

## Redstone ed energia

- Richiede **RF/FE** nella barra energia.
- Pulsante modalità redstone: ignore / low / high / disabled (stessa famiglia delle altre macchine).

## Moduli

Tre slot upgrade (icone fantasma se vuoti):

| Slot | Modulo | Effetto |
| ---- | ------ | ------ |
| Logic | <ItemImage id="iska_utils:logic_module" /> **Logic Module** | **+1 pattern** e **+1 pagina variabili** (18 chiavi) ciascuno. Massimo predefinito **4** sul Migliorato. |
| Speed | **Moduli vector** (**Slow** → **Ultra**) | Accorcia il tempo di craft (tier più alti = più veloce). |
| Production | <ItemImage id="iska_utils:production_module" /> **Modulo di produzione** | Aumenta quanti craft girano in parallelo. |

Vedi **Moduli** → **Logic Module**, **Moduli vector** e **Modulo di produzione**.

## JEI

Con la GUI del Pattern Crafter aperta, usa il trasferimento ricetta JEI (**+**) su una ricetta da banco:

- Le **variabili** (filtri / lettere, incluso `#tag` quando la ricetta usa tag) si applicano **subito**.
- La griglia **pattern** e la **modalità craft** restano **in sospeso** finché non confermi.
- **Salva** conferma solo il pattern (e la modalità); **Annulla** scarta le modifiche pattern pendenti (le variabili restano).

Servono slot variabile liberi e lettere non usate per ogni nuovo tipo di ingrediente.

<ItemGrid>
  <ItemIcon id="iska_utils:pattern_crafter" />
  <ItemIcon id="iska_utils:improved_pattern_crafter" />
  <ItemIcon id="iska_utils:pattern_crafter_improver" />
  <ItemIcon id="iska_utils:logic_module" />
  <ItemIcon id="iska_utils:slow_module" />
  <ItemIcon id="iska_utils:production_module" />
</ItemGrid>

## Suggerimenti

- Sblocca le lettere sulle variabili **prima** di modificare i loro filtri.
- Tieni Segna input allineato ai filtri a lettere così l'automazione resta coerente.
- Usa **Forbidden outputs** per fermare sottoprodotti fastidiosi; usa la modalità craft per forzare solo shaped o solo shapeless.
- Abbina Logic Module a pattern multi-ingrediente più complessi; usa Production quando ti serve throughput.
