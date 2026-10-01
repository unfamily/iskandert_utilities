---
navigation:
  title: Telaio etereo
  icon: iska_utils:ethereal_frame
  parent: hubs/world_and_machines.md
  position: 61
item_ids:
  - iska_utils:ethereal_frame
categories:
  - World and machines
---
# Telaio etereo

<ItemImage id="iska_utils:ethereal_frame" />

## A cosa serve

Il **Telaio etereo** è un blocco filtro avanzato che controlla **quali tipi di entità possono attraversarlo** in base a un elenco consentiti/bloccati che imposti nella GUI.

- **Predefinito**: modalità Consenti con solo `minecraft:player` selezionato (i giocatori passano; tutto il resto è bloccato).
- I telai adiacenti **condividono automaticamente** le modifiche al filtro sulla rete collegata (fino a 64 telai).
- Quando ci sono più reti vicine, vince il telaio con l’**aggiornamento filtro più recente** durante la sincronizzazione periodica e quando posizioni nuovi telai accanto a una rete esistente.

## Interazioni

| Azione | Risultato |
|--------|--------|
| **Click destro (mano vuota)** | Apre la GUI filtro entità |
| **Click destro (blocco pieno, non mimetizzato)** | Applica il blocco come mimetizzazione |
| **Click destro (blocco pieno, già mimetizzato)** | Apre la GUI filtro |
| **Shift + click destro (mimetizzato)** | Restituisce il blocco di mimetizzazione |
| **Shift + click destro (senza mimetizzazione, rete rinforzata)** | Rimuove il rinforzo dalla rete collegata e restituisce i materiali |
| **Click sinistro (materiale di rinforzo in mano)** | Rinforza quanti telai della rete permette lo stack |

## Durabilità

Di default il telaio è **simile al legno** (non resistente a wither / esplosioni). Si può **rinforzare** con materiali come <ItemImage id="iska_utils:wither_proof_block" /> **Blocco resistente al wither**, Vetro oscuro, Vetro etereo oscuro o Barre di netherite (`#c:bars/netherite`) così la rete collegata diventa resistente al wither come quei blocchi. Rompendo un telaio rinforzato recuperi il materiale di rinforzo (e la mimetizzazione se presente).

## Mimetizzazione

Click destro con un **blocco opaco pieno** sul telaio per camuffarlo come quel blocco. Il telaio mantiene il comportamento del filtro — cambia solo l’aspetto. Shift-click per rimuovere la mimetizzazione e recuperare il blocco.

## GUI filtro

La schermata filtro mostra un elenco ricercabile di tipi di entità, tag comuni e chiavi filtro speciali.

Passa da **Lista consentiti / Lista bloccati** per invertire la logica, seleziona le voci e premi **Applica**. Dopo **Annulla**, l’icona vetro / vetro colorato alterna se il telaio **lascia passare o blocca la luce**.

### Voci del filtro

| Tipo voce | Esempio | Significato |
|------------|---------|---------|
| Tipo entità | `minecraft:zombie` | Corrisponde a quel tipo |
| Tag entità | `#minecraft:raiders` | Corrisponde a qualsiasi entità nel tag |
| Chiave speciale | `$is_monster` | Corrisponde alle entità che soddisfano la condizione |

Chiavi speciali: `$have_armor`, `$is_not_have_armor`, `$have_tool`, `$is_not_have_tool`, `$is_baby`, `$is_adult`, `$is_monster`, `$is_animal`, `$is_neutral`, `$on_fire`, `$is_not_on_fire`, `$is_crouching`, `$is_not_crouching`.

- `$is_animal` corrisponde alle entità vanilla `Animal` (mucche, lupi, axolotl, …).
- `$is_neutral` corrisponde alle entità che implementano `NeutralMob` (endermen, api, piglin, golem di ferro, …).
- Baby/adult coprono già l’opposto l’una dell’altra — non esistono `$is_not_baby` / `$is_not_adult`.

Un’entità passa quando **qualsiasi** voce selezionata corrisponde, poi si applica la modalità consenti/nega.
