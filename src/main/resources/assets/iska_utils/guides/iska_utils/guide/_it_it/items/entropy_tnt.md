---
navigation:
  title: TNT entropica
  icon: iska_utils:entropy_tnt
  parent: hubs/entropy_materials.md
  position: 4
item_ids:
  - iska_utils:entropy_tnt
categories:
  - Entropy materials
---
# TNT entropica

<ItemImage id="iska_utils:entropy_tnt" />

Blocco esplosivo di entropia altamente instabile. Maneggiare con estrema cautela.

## Comportamento

Piazzato, detona se il blocco riceve energia **redstone** (anche se piazzato già alimentato). Un segnale vicino può attivarlo.

L'esplosione è un **ellissoide progressivo**:

| Asse | Raggio (blocchi) |
|------|------------------|
| Orizzontale (X / Z) | **250** |
| Verticale (Y) | **50** |

Distrugge il terreno in quel volume, inclusi blocchi normalmente indistruttibili (come bedrock). L'esplosione si espande nel tempo invece di liberare l'area all'istante.
