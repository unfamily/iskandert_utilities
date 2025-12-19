package net.unfamily.iskautils.client.model;

import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * Proprietà ModelData per passare i dati I/O del Smart Timer al modello
 */
public class SmartTimerModelData {
    
    /**
     * Property per la configurazione I/O (byte array di 6 elementi)
     * 0 = BLANK, 1 = INPUT, 2 = OUTPUT
     */
    public static final ModelProperty<byte[]> IO_CONFIG_PROPERTY = new ModelProperty<>();
    
    /**
     * Property per lo stato degli input (boolean array di 6 elementi)
     * true = segnale attivo, false = nessun segnale
     */
    public static final ModelProperty<boolean[]> INPUT_STATES_PROPERTY = new ModelProperty<>();
}
