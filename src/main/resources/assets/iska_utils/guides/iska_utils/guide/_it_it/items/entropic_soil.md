---
navigation:
  title: Terreno entropico
  icon: iska_utils:entropic_soil
  parent: hubs/mobfarm_soils.md
  position: 0
item_ids:
  - iska_utils:entropic_soil
  - iska_utils:entropic_dirt
categories:
  - MobFarm Soils
---
# Terreno entropico

<ItemImage id="iska_utils:entropic_soil" />

Usa <ItemImage id="iska_utils:entropic_agglomeration" /> **Agglomerato entropico** per convertire una patch all’istante — vedi quella pagina. Sotto: comportamento del blocco e propagazione **naturale** con random tick.

## Comportamento

- La **luce** (cielo o luce di blocco) trasforma il terreno entropico in **Terra entropica** (la terra non si propaga sui blocchi vanilla).
- Al **buio**, **ogni** blocco di terreno esegue il proprio timer di spawn. I mob molto resistenti vengono saltati. I mob generati ottengono **Potenziamento entropico**.
- La **redstone** su una **patch collegata** (basta un clock sul bordo) fa passare ogni blocco della patch a timer di spawn rapidi (come la dreadful dirt di Mob Grinding Utils): ogni tile valido prova con il proprio cooldown **20–60 tick** (max **8** ostili nelle vicinanze per blocco). Gli aggiornamenti dei vicini aggiornano il flag della patch e possono scatenare un tentativo immediato.

## Propagazione naturale

- Converte lentamente **erba o terra vanilla** adiacenti (non podzol druidico, non terreno entropico già presente).
- Più veloce quando il tile erba/terra confina con un blocco di **terreno entropico** (espansione del bordo della patch).
- La **Terra entropica** accanto al terreno entropico viene riconvertita in terreno in fretta (molto più veloce della propagazione vanilla).

## Terra entropica

<ItemImage id="iska_utils:entropic_dirt" />

La forma scurita del terreno entropico. Il **Terreno entropico** adiacente la riconverte naturalmente, oppure usa una <ItemImage id="iska_utils:drop_of_entropy" /> **Goccia di entropia** (click destro) per ripristino istantaneo. Dopo **lunga** esposizione alla luce, la terra entropica torna **terra vanilla**; il buio mette in pausa il timer.
