---
navigation:
  title: Podzol druidico
  icon: iska_utils:druidic_podzol
  parent: hubs/mobfarm_soils.md
  position: 2
item_ids:
  - iska_utils:druidic_podzol
categories:
  - MobFarm Soils
---
# Podzol druidico

<ItemImage id="iska_utils:druidic_podzol" />

Usa <ItemImage id="iska_utils:druidic_agglomeration" /> **Agglomerato druidico** per la conversione istantanea della patch — vedi quella pagina.

## Comportamento

- Alla **luce**, **ogni** blocco podzol esegue il proprio timer di spawn per **animali del bioma**. Gli animali molto resistenti vengono saltati.
- Al **buio**, il blocco **non fa nulla** — niente spawn, **nessun** cambio blocco (a differenza di Entropic Soil → Entropic Dirt).
- **Redstone** su una **patch connessa** (basta un clock sul bordo) fa passare ogni podzol illuminato della patch a timer di spawn rapidi (come il delightful dirt di Mob Grinding Utils): ogni tile valido prova con cooldown **20–60 tick** (max **8** animali vicini per blocco). Gli aggiornamenti dei vicini refrescano il flag di patch e possono scatenare un tentativo immediato.

## Spread naturale

- Converte lentamente **terra e podzol** adiacenti — non erba, non terreno/dirt entropico.
- Non si espande su podzol druidico già piazzato.

## Drop

Si rompe come il podzol: lascia **terra** salvo estrazione con **Silk Touch** (allora lascia il blocco podzol).

## Confronto con Entropic Soil

Vedi **Entropic Soil** per spawn al buio, decadimento alla luce e ripristino con **Goccia di entropia**. Spread naturale e agglomerato seguono le regole corrottive vs benedette descritte in **MobFarm Soils**.
