---
navigation:
  title: Altare fiammeggiante
  icon: iska_utils:blazing_altar
  parent: hubs/combat_and_travel.md
  position: 23
item_ids:
  - iska_utils:blazing_altar
categories:
  - Combat and travel
---
# Altare fiammeggiante

<ItemImage id="iska_utils:blazing_altar" />

## Panoramica

Controllore d’area per il posizionamento di **Burning Flame** / **Cursed Burning Flame** e, opzionalmente, per bloccare gli **spawn naturali**. Funziona solo in chunk **già caricati** (non carica chunk).

## A cosa serve

- Se **non** inserisci un **Burning Brazier** o una **Candela arcana**, l’altare **impedisce lo spawn naturale dei mob** nell’area (secondo filtro spawn e redstone).
- Inserisci un **Burning Brazier** o una **Candela arcana** per piazzare automaticamente le fiamme corrispondenti (**Burning Flame** / **Cursed Burning Flame**) nel raggio in chunk.
- Con un braciere, la durabilità residua **diminuisce** a ogni posizionamento ma l’oggetto **non** si rompe mai (limitata così da non esaurirsi del tutto).

## GUI

- **Filtro spawn**: Off / All / Hostile / Passive — influisce solo sugli spawn mob `NATURAL` nell’area.
- **Raggio in chunk** (Chebyshev): dimensione dell’area per fiamme e filtro spawn.
- **Solo a terra**: le fiamme solo sopra terreno solido (predefinito attivo). Con “solo a terra” disattivo, le fiamme possono comparire in aria.
- **Blocchi sensibili alla luce**: funghi, terreno entropico e dreadful dirt devono restare al buio sul blocco e nello spazio sopra — il posizionamento fiamme che li illuminerebbe viene rifiutato.
- **Flame Vision**: toggle client globale per vedere i blocchi fiamma della mod (anche con click sinistro in aria o su braciere/candela).
- **Show**: pilastri agli angoli che segnano i chunk estremi dell’area coperta.
- **Redstone**: predefinito **ignorata** (sempre attivo). Le altre modalità come le macchine Factory (niente impulso).
- **Spegnimento / rottura**: rimuovere le fiamme scansiona l’area progressivamente così i raggi grandi non bloccano il server. Rompere l’altare programma anche la pulizia delle fiamme nel raggio. Spegnere può ripristinare la durabilità del braciere quando le fiamme vengono rimosse.

## Moduli

- Slot **Range Module**: ogni modulo aumenta il **raggio in chunk** dell’altare (copertura per fiamme e filtro spawn).

Vedi **Moduli** → **Range Module**.

## Visibilità

I blocchi **Burning Flame** e **Cursed Burning Flame** sono nascosti sul client a meno che **Flame Vision** sia attiva (GUI, o click sinistro in aria / su braciere o candela).
