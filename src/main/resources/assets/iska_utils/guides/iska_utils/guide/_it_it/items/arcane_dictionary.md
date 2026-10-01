---
navigation:
  title: Dizionario arcano
  icon: iska_utils:arcane_dictionary
  parent: hubs/entropy_materials.md
  position: 13
item_ids:
  - iska_utils:arcane_dictionary
categories:
  - Entropy materials
---
# Dizionario arcano

<ItemImage id="iska_utils:arcane_dictionary" />

## Come ottenerlo

- **Consegna sospetta** — uno dei possibili esiti aprendo un pacco. Vedi <ItemImage id="iska_utils:suspicious_delivery" /> **Consegna sospetta**.
- Riciclabile in **tre** <ItemImage id="iska_utils:drop_of_entropy" /> **Gocce di entropia** come gli altri artefatti da consegna.

## A cosa serve

Il **Dizionario arcano** è un **Curio arcano** (cintura o amuleto) che conserva **tratti arcani** casuali (fino a diversi tratti, ciascuno con un livello indicato in numeri romani). Con **esattamente un** dizionario equipaggiato in Curios e con tratti assegnati, quei tratti si applicano come effetti passivi.

**Due o più** dizionari equipaggiati in Curios contemporaneamente **annullano tutti gli effetti dei tratti**.

Layout del tooltip: righe di flavor **grigie**, righe meccaniche **verde lime**, intestazione artefatto arcano **viola**; i nomi dei tratti usano i **colori per tratto** del datapack. **Tieni premuto Shift** per brevi descrizioni dei tratti e suggerimenti sul reroll con catalizzatore.

## Cariche di entropia (carburante per tratti attivi)

Separato dal reroll con XP. Con **esattamente un** dizionario in **Curios**, **assorbe automaticamente** <ItemImage id="iska_utils:drop_of_entropy" /> **Gocce di entropia** dall’**inventario** (non dagli slot Curios), fino a un ampio buffer interno. I dizionari solo in inventario non assorbono.

Quando i tratti sono **attivi**, l’entropia immagazzinata viene **consumata nel tempo** in base al consumo di entropia di ogni tratto (datapack `ent_cha`; alcuni tratti non costano nulla). Se il buffer si esaurisce, gli effetti si spengono finché non assorbi altre gocce.

## Reroll dei tratti (solo XP)

**Non** usa le cariche di entropia.

1. Tieni il dizionario nella **mano principale**.
2. **Shift + click destro** — spende parte della tua esperienza. Solo una certa quantità di XP viene prelevata per reroll; il resto resta a te.
3. **Più XP offri, migliore tende a essere il tiro** — più tratti, livelli più alti o entrambi — ma ogni reroll resta **casuale** finché non raggiungi il budget di livello pieno; a quel punto ottieni sempre il numero massimo di tratti e livelli.
4. Metti un **catalizzatore** corrispondente nella **mano secondaria** (ad esempio lapislazzuli per **Lucky**) per **aumentare** la probabilità di quel tratto — il tooltip del tratto con Shift mostra la **nuova probabilità di estrazione** con quel catalizzatore. Un catalizzatore viene consumato per reroll; **non** garantisce il tratto. Ogni tratto in un tiro è **unico** quando ci sono abbastanza voci; i duplicati compaiono solo se il tiro chiede più tratti di quelli disponibili.

## Fortuna e Sfortuna

Quali tratti **specifici** compaiono in ogni slot è influenzato separatamente dalla qualità dell’XP.

- Dopo la prima estrazione pesata, l’effetto vanilla **Luck** può rifare il tiro lontano dalle voci a basso `luck` nel pool; **Bad Luck** può rifare il tiro lontano da quelle ad alto `luck`. Effetti pozione più forti contano di più.
- Il tratto **Lucky** applica **Luck**; il tratto **Unlucky** applica **Bad Luck** — quindi possono spostare anche i reroll successivi.
- I catalizzatori nella mano secondaria **aumentano** il peso di estrazione dei tratti corrispondenti (probabilità di reroll nel tooltip); non annullano del tutto la fortuna né garantiscono un tratto.

I datapack possono etichettare le voci con quanto fortunate o sfortunate sono; gli autori del pack decidono quali tratti stanno da che parte della scala.

## Tratti arcani

Ogni reroll pesca dal pool dei tratti. Dettagli (effetti, livelli, catalizzatori) compaiono anche nei tooltip quando ispezioni il dizionario con Shift.

Alcuni tratti compaiono solo quando certe mod o fasi di progressione sono disponibili. I tratti che non puoi ottenere vengono saltati nel tiro; un tratto già sul dizionario non fa nulla finché i requisiti non sono soddisfatti.

## Suggerimenti

- Un dizionario **vuoto** (senza tratti) può impilarsi in un **Deep Drawer** come altri oggetti a stack singolo senza tratti.
- Equipaggiarlo conta comunque come **artefatto arcano** per <ItemImage id="iska_utils:busted_crown" /> **Corona rotta**, anche se è vuoto.
