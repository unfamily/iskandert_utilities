---
navigation:
  title: Fonoassorbente
  icon: iska_utils:sound_muffler
  parent: hubs/world_and_machines.md
  position: 52
item_ids:
  - iska_utils:sound_muffler
categories:
  - World and machines
  - Ambience
---
# Fonoassorbente

<ItemImage id="iska_utils:sound_muffler" />

## A cosa serve

- Attenua i **suoni riprodotti** per i giocatori dentro la sua **portata sferica** (minimo **8** blocchi). Il volume è **per categoria**, non un unico controllo master.
- La **musica** non passa da questi slider (la musica client resta separata).

## GUI principale

- **Portata**: ingrandisci o riduci la bolla in cui vale l'attenuazione; altri controlli usano spesso passi +/- (vedi tooltip).
- **Una riga per categoria** (ordine tipico): **Tutti**, **Altro** (suoni non catalogati / strani delle mod), **Dischi**, **Meteo**, **Blocchi**, **Ostili**, **Neutrali**, **Giocatori**, **Ambiente**, **Voce**.
- Ogni categoria ha il proprio livello **0–100%**: **0** = quella categoria è di fatto muta nel raggio, **100** = nessuna riduzione per quella categoria.

## Schermata filtro

- Apri **Filtro** per gestire una **lista di ID suono** (es. `minecraft:entity.creeper.primed`).
- Alterna **lista consentita** e **lista negata** (come la lista interagisce con questo fonoassorbente):
  - **Lista consentita**: gli ID in lista sono **esenti** — questo blocco **non** attenua quei suoni (tutto il resto usa ancora gli slider di categoria).
  - **Lista negata**: solo gli ID in lista sono attenuati da questo blocco; i suoni **non** in lista passano **invariati** da questo fonoassorbente.
- Con la lista **vuota**, il filtro è inattivo e valgono **solo** i volumi per categoria.
- Usa **Cerca** per trovare rapidamente gli ID su pack affollati.
