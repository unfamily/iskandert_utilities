# Sistema Team per Shop Iska Utils

## Panoramica

Il sistema di team per il shop permette ai giocatori di condividere le valute (null_coin) all'interno di un team. Tutti i membri del team possono utilizzare le valute condivise per comprare e vendere oggetti nel shop.

## Comandi Team

### Gestione Team

- `/iska_utils_team create <nome_team>` - Crea un nuovo team (il creatore diventa leader)
- `/iska_utils_team delete <nome_team>` - Elimina un team (solo il leader può farlo)
- `/iska_utils_team add <nome_team> <giocatore>` - Aggiunge un giocatore al team
- `/iska_utils_team remove <nome_team> <giocatore>` - Rimuove un giocatore dal team
- `/iska_utils_team info <nome_team>` - Mostra informazioni su un team
- `/iska_utils_team list` - Lista tutti i team
- `/iska_utils_team balance <nome_team> <valuta>` - Mostra il saldo di una valuta per un team
- `/iska_utils_team balance <nome_team> <valuta> <quantità>` - Aggiunge valute a un team

### Comandi Shop

- `/iska_utils_shop balance` - Mostra il saldo del tuo team
- `/iska_utils_shop buy <item> <quantità>` - Compra un oggetto usando le valute del team
- `/iska_utils_shop sell <item> <quantità>` - Vendi un oggetto e ricevi valute per il team

## Come Funziona

### Creazione Team
1. Un giocatore crea un team con `/iska_utils_team create <nome>`
2. Il creatore diventa automaticamente il leader del team
3. Il leader può aggiungere altri giocatori con `/iska_utils_team add <nome> <giocatore>`

### Gestione Valute
- Le valute sono condivise tra tutti i membri del team
- Quando un giocatore compra qualcosa, le valute vengono sottratte dal saldo del team
- Quando un giocatore vende qualcosa, le valute vengono aggiunte al saldo del team
- Solo i membri del team possono utilizzare le valute condivise

### Permessi
- **Leader**: Può eliminare il team, aggiungere/rimuovere membri
- **Membri**: Possono utilizzare le valute del team per comprare/vendere
- **Nessuno**: Può vedere le informazioni pubbliche dei team

## Esempi di Utilizzo

### Creare un Team
```
/iska_utils_team create SquadraAlpha
```

### Aggiungere Membri
```
/iska_utils_team add SquadraAlpha Steve
/iska_utils_team add SquadraAlpha Alex
```

### Verificare il Saldo
```
/iska_utils_shop balance
```

### Comprare Oggetti
```
/iska_utils_shop buy minecraft:diamond 5
```

### Vendere Oggetti
```
/iska_utils_shop sell minecraft:iron_ingot 10
```

### Aggiungere Valute (per testing)
```
/iska_utils_team balance SquadraAlpha null_coin 1000
```

## Persistenza Dati

I dati dei team vengono salvati automaticamente nel mondo e persistono tra i riavvii del server. I dati includono:
- Membri del team
- Leader del team
- Saldi delle valute per ogni team

## Note Importanti

1. **Un giocatore può essere in un solo team alla volta**
2. **Le valute sono condivise tra tutti i membri del team**
3. **Solo il leader può eliminare il team o rimuovere membri**
4. **I dati vengono salvati automaticamente**
5. **Il sistema supporta multiple valute (attualmente solo null_coin)**

## Integrazione con Shop

Il sistema di team si integra perfettamente con il sistema shop esistente:
- Le transazioni utilizzano automaticamente le valute del team
- I prezzi sono definiti nei file di configurazione JSON
- Il sistema supporta compra/vendita di oggetti
- Tutti i membri del team possono vedere il saldo condiviso 