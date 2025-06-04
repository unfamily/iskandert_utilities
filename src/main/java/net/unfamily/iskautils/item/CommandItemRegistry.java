package net.unfamily.iskautils.item;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.command.CommandItemDefinition;
import net.unfamily.iskautils.command.CommandItemLoader;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry per gli item di comando registrati fisicamente come item separati
 * seguendo l'approccio usato per le potion plate
 */
public class CommandItemRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Registro differito per gli item
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
            Registries.ITEM, IskaUtils.MOD_ID);
    
    // Registro differito per i tab creativi
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB, IskaUtils.MOD_ID);
    
    // Tab creativo per gli item di comando
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> COMMAND_ITEMS_TAB =
            CREATIVE_TABS.register("command_items", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.iska_utils.command_items"))
                    .icon(() -> new ItemStack(net.minecraft.world.item.Items.COMMAND_BLOCK))
                    .displayItems((parameters, output) -> {
                        // Gli item saranno aggiunti dall'event handler
                    })
                    .build());
    
    // Mappa per memorizzare gli item di comando registrati
    private static final Map<String, DeferredHolder<Item, CommandItem>> REGISTERED_ITEMS = new HashMap<>();
    
    // Mappa per item virtuali (caricati da datapack esterni come KubeJS)
    private static final Map<String, CommandItemDefinition> VIRTUAL_ITEMS = new HashMap<>();
    
    /**
     * Registra questo registry con l'event bus
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        CREATIVE_TABS.register(eventBus);
        
        // Registra l'event handler per aggiungere item al tab creativo
        eventBus.addListener(CommandItemRegistry::onBuildCreativeModeTabContents);
    }
    
    /**
     * Event handler per aggiungere item ai tab creativi
     */
    private static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        // Non aggiungiamo più elementi al tab dei command item poiché ora
        // sono tutti visualizzati nel tab principale di IskaUtils
        // Il tab viene comunque mantenuto per compatibilità, ma vuoto
    }
    
    /**
     * Inizializza gli item di comando basati sulle definizioni caricate
     */
    public static void initializeItems() {
        // Carica le definizioni degli item da file JSON
        CommandItemLoader.scanConfigDirectory();
        
        // Registra gli item per ogni definizione
        for (CommandItemDefinition definition : CommandItemLoader.getAllCommandItems().values()) {
            registerCommandItem(definition);
        }
        
        LOGGER.info("Registrati {} item di comando fisici", REGISTERED_ITEMS.size());
    }
    
    /**
     * Registra un item di comando dalla sua definizione
     */
    private static void registerCommandItem(CommandItemDefinition definition) {
        String id = definition.getId();
        String registryName = id.replace("-", "_").toLowerCase();
        
        // Crea le proprietà dell'item
        Item.Properties properties = new Item.Properties()
                .stacksTo(definition.getMaxStackSize());
        
        // Registra l'item
        DeferredHolder<Item, CommandItem> registeredItem = ITEMS.register(
                registryName, () -> new CommandItem(properties, definition));
        
        // Memorizza nel registry
        REGISTERED_ITEMS.put(id, registeredItem);
        
        LOGGER.info("Registrato item di comando: {} (registryName: {}, stackSize: {})",
                id, registryName, definition.getMaxStackSize());
    }
    
    /**
     * Ottiene un item di comando registrato per ID
     */
    public static DeferredHolder<Item, CommandItem> getCommandItem(String id) {
        return REGISTERED_ITEMS.get(id);
    }
    
    /**
     * Registra un item di comando dinamicamente a runtime (per supporto KubeJS)
     * Poiché i registry NeoForge sono congelati a runtime, usiamo item virtuali
     */
    public static void registerCommandItemDynamic(String id, CommandItemDefinition definition) {
        // Controlla se è già registrato come item reale
        if (REGISTERED_ITEMS.containsKey(id)) {
            LOGGER.warn("L'item di comando {} è già registrato come item reale, salto la registrazione virtuale", id);
            return;
        }
        
        // Controlla se è già registrato come item virtuale
        if (VIRTUAL_ITEMS.containsKey(id)) {
            LOGGER.warn("L'item di comando virtuale {} è già registrato, aggiorno la definizione", id);
        }
        
        // Registra come item virtuale (solo configurazione)
        VIRTUAL_ITEMS.put(id, definition);
        LOGGER.info("Registrato item di comando virtuale: {} (solo configurazione, nessun item fisico)", id);
    }
    
    /**
     * Ottiene una definizione di item di comando virtuale per ID
     */
    public static CommandItemDefinition getVirtualDefinition(String id) {
        return VIRTUAL_ITEMS.get(id);
    }
    
    /**
     * Ottiene tutte le definizioni degli item di comando virtuali
     */
    public static Map<String, CommandItemDefinition> getAllVirtualItems() {
        return new HashMap<>(VIRTUAL_ITEMS);
    }
    
    /**
     * Controlla se un item di comando è registrato (reale o virtuale)
     */
    public static boolean isRegistered(String id) {
        return REGISTERED_ITEMS.containsKey(id) || VIRTUAL_ITEMS.containsKey(id);
    }
    
    /**
     * Controlla se un item di comando è registrato come item reale
     */
    public static boolean isRealItem(String id) {
        return REGISTERED_ITEMS.containsKey(id);
    }
    
    /**
     * Controlla se un item di comando è registrato come item virtuale
     */
    public static boolean isVirtualItem(String id) {
        return VIRTUAL_ITEMS.containsKey(id);
    }
    
    /**
     * Ottiene il numero di item di comando registrati (reali + virtuali)
     */
    public static int getRegisteredCount() {
        return REGISTERED_ITEMS.size() + VIRTUAL_ITEMS.size();
    }
    
    /**
     * Ottiene il numero di item di comando reali registrati
     */
    public static int getRealItemCount() {
        return REGISTERED_ITEMS.size();
    }
    
    /**
     * Ottiene il numero di item di comando virtuali registrati
     */
    public static int getVirtualItemCount() {
        return VIRTUAL_ITEMS.size();
    }
    
    /**
     * Ottiene tutti gli item di comando registrati
     */
    public static Map<String, DeferredHolder<Item, CommandItem>> getAllItems() {
        return new HashMap<>(REGISTERED_ITEMS);
    }
    
    /**
     * Ricarica le definizioni degli item di comando
     * Nota: Gli item non possono essere registrati/deregistrati dinamicamente dopo l'avvio del gioco.
     * Questo aggiorna solo le definizioni virtuali per gli item già registrati.
     */
    public static void reloadDefinitions() {
        LOGGER.info("Ricaricamento definizioni degli item di comando...");
        
        // Ricarica le definizioni dai file JSON
        CommandItemLoader.reloadAllDefinitions();
        
        // Aggiorna gli item virtuali
        for (String id : CommandItemLoader.getAllCommandItems().keySet()) {
            if (!REGISTERED_ITEMS.containsKey(id) && !VIRTUAL_ITEMS.containsKey(id)) {
                // Nuova definizione trovata, registra come virtuale
                CommandItemDefinition definition = CommandItemLoader.getCommandItem(id);
                if (definition != null) {
                    registerCommandItemDynamic(id, definition);
                }
            }
        }
        
        LOGGER.info("Definizioni degli item di comando ricaricate - le modifiche si applicheranno agli item virtuali");
        LOGGER.info("Gli item reali non possono essere aggiunti/rimossi dopo l'avvio del gioco");
    }
} 