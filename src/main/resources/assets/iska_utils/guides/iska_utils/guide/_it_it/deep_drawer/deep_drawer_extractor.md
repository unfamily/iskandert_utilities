---
navigation:
  parent: deep_drawer/deep_drawer-index.md
  title: Estrattore per cassetti profondi
  icon: deep_drawer_extractor
  position: 20
categories:
- storage
item_ids:
- iska_utils:deep_drawer_extractor---


# Estrattore per cassetti profondi

<BlockImage id="deep_drawer_extractor" scale="8" />

L'estrattore a cassetto profondo è un modulo specializzato progettato per estrarre oggetti da un cassetto profondo. Utilizza l'estrazione passiva: gli elementi vengono spostati nel suo buffer interno ed è necessario collegare i tubi o le tramogge degli elementi per estrarre gli elementi dall'Estrattore stesso.

Caratteristiche Chiave

- **Passive Extraction**: sposta automaticamente gli elementi dal Cassetto Profondo collegato al suo buffer interno in base ai filtri. È necessario collegare i tubi o le tramogge degli articoli all'estrattore per ottenere gli articoli dal buffer
- **Advanced Filter System**: supporta più tipi di filtro tra cui ID articolo, tag, ID mod, dati NBT e macro predefinite. L'elenco completo delle chiavi filtro valide è disponibile nella GUI cliccando sul pulsante “Chiavi valide”
- **Dual Filter System**:
- **Allow Filter List**: estrae solo gli elementi che corrispondono ai filtri (modalità whitelist). Quando questo elenco è attivo, il pulsante mostra "Nega elenco" per passare al filtro di rifiuto
- **Deny Filter List**: estrae tutti gli elementi tranne quelli che corrispondono ai filtri (modalità blacklist). Quando questo elenco è attivo, il pulsante mostra "Consenti elenco" per tornare al filtro Consenti
- **Filter Priority**: quando entrambi gli elenchi di filtri hanno voci (casi ibridi), viene applicato prima l'elenco Nega filtro, quindi l'elenco Consenti filtro. Ciò significa che gli elementi che corrispondono all'elenco Nega filtro vengono bloccati prima di controllare l'elenco Consenti filtro
- Passa tra i due elenchi di filtri utilizzando il pulsante "Consenti elenco"/"Nega elenco" nella GUI
- **Filter Concatenation**: ogni riga del filtro ha un pulsante concatenato (`-`, `A`–`Z`). Le linee che condividono la stessa lettera devono corrispondere tutte (AND); lettere diverse o linee autonome si combinano con OR
- **Redstone Control**: Controllo estrazione con segnali redstone
- **Optimized Performance**: Progettato specificamente per l'estrazione per ridurre al minimo l'impatto sulle prestazioni
- **Network Discovery**: trova automaticamente il cassetto attraverso moduli collegati fino a 16 blocchi di distanza

Casi d'uso

- **Selective Item Extraction**: Estrai automaticamente oggetti specifici come attrezzi, strumenti o oggetti incantati da mod specifiche dal cassetto al buffer dell'estrattore
- **Production Lines**: Estrai gli elementi specifici necessari per i sistemi automatizzati di lavorazione o lavorazione

Ricorda: l'estrattore sposta gli elementi dal cassetto al suo buffer interno, ma è necessario collegare i tubi o le tramogge degli elementi all'estrattore stesso per ottenere effettivamente gli elementi dal buffer e indirizzarli verso la destinazione.
