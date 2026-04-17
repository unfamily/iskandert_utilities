package net.unfamily.iskautils.client.model;

/**
 * Model data keys for the Smart Timer renderer/model.
 *
 * NeoForge 26.x removed/changed the old ModelData/ModelProperty API used by older versions.
 * For now these keys are kept as lightweight, type-safe identifiers that can be wired into
 * the new model/render state system when the Smart Timer model is ported.
 */
public class SmartTimerModelData {
    
    /**
     * Property per la configurazione I/O (byte array di 6 elementi)
     * 0 = BLANK, 1 = INPUT, 2 = OUTPUT
     */
    public static final PropertyKey<byte[]> IO_CONFIG_PROPERTY = new PropertyKey<>("io_config");
    
    /**
     * Property per lo stato degli input (boolean array di 6 elementi)
     * true = segnale attivo, false = nessun segnale
     */
    public static final PropertyKey<boolean[]> INPUT_STATES_PROPERTY = new PropertyKey<>("input_states");

    /**
     * Minimal key type to avoid coupling to a specific model-data implementation.
     */
    public record PropertyKey<T>(String id) {}
}
