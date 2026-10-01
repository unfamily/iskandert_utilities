---
navigation:
  title: Annullatore di volo
  icon: iska_utils:flight_nullifier
  parent: hubs/world_and_machines.md
  position: 23
item_ids:
  - iska_utils:flight_nullifier
categories:
  - World and machines
---
# Annullatore di volo

<ItemImage id="iska_utils:flight_nullifier" />

## A cosa serve

Blocca il **volo** dentro un **raggio** cubico attorno al blocco. Quando il bersaglio include le entità aeree, sopprime anche il movimento aereo verso l'alto — Ghast, Phantom, Blaze e simili vengono bloccati a terra nella zona. Stesso raggio e upgrade con Modulo portata dell'[Annullatore dell'End](ender_nullifier.md).

Le entità nella lista di esclusione configurata (predefinita: `#c:bosses`) sono esenti da questo nullificatore.

## GUI

Stesso layout degli altri annullatori: **redstone**, **slot modulo**, riga portata e anteprima area. Inoltre:

- **Bersaglio** (pulsante icona, lato destro): cicla il gruppo interessato. Click sinistro avanza, click destro torna indietro.
  - *Solo mob* (predefinito): sopprime il volo aereo nella zona.
  - *Solo giocatori*: blocca il volo in Sopravvivenza/Avventura per i giocatori.
  - *Mob e giocatori*: interessa entrambi i gruppi.

## Suggerimenti

- Vedi anche: [Nullificatore da arrampicata](climbing_nullifier.md), [Nullificatore Ender](ender_nullifier.md).
