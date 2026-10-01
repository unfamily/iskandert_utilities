---
navigation:
  title: Tavolo Antico
  icon: iska_utils:ancient_table
  parent: hubs/entropy_materials.md
  position: 12
item_ids:
  - iska_utils:ancient_table
categories:
  - Entropy materials---

# Tavola Antica

<ItemImage id="iska_utils:ancient_table" />

WHAT IT DOES

Lo **Ancient Table** gestisce le stesse imbarcazioni speciali del <ItemImage id="iska_utils:ancient_tablet" /> **Tavoletta antica**, ma **automatically**. Ingrediente **order does not matter** e layout sbagliati non distruggono mai i tuoi input.

Il carburante è <ItemImage id="iska_utils:drop_of_entropy" /> **Goccia di entropia**. Ogni goccia posizionata nella fessura del carburante viene convertita in **internal fuel** (nessun articolo NBT). La tabella contiene un grande buffer di carburante interno; un comparatore legge **only** quanto è pieno lo slot di carburante fisico, non il buffer interno.

COME USARE

1. Posiziona gli ingredienti nella⟧ griglia ⟦0 (conteggi di stack per materia dello slot).
2. Mettere **Goccia di entropia** nello⟧ slot ⟦1 (un articolo viene consumato solo quando il buffer può assorbire un'unità di carburante completa).
3. Prendi i risultati dalla⟧ griglia ⟦0 quando finisci di creare.
4. Utilizzare il⟧ pulsante ⟦0 accanto alla fessura del carburante (allineata con la prima riga di ingresso) per controllare quando il tavolo funziona (stesse modalità della fabbrica).

Scorrere le griglie di input e output quando si hanno più di nove slot visibili.

A **comparator** sul blocco esce **0–15** dal **fuel slot stack count only** (slot vuoto = 0, pila piena = 15).⟧ Riflette il carburante interno ⟦o il riempimento di ingresso/uscita — utile per vedere quando la fessura del carburante deve essere ricaricata.

Suggerimenti

- Alcune imbarcazioni, come <ItemImage id="iska_utils:entropy_crystal" /> **Entropy Crystal**, consumano più carburante interno per operazione rispetto a quelle più semplici.
