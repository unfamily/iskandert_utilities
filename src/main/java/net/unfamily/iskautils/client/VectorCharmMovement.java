package net.unfamily.iskautils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.data.VectorCharmData;
import net.unfamily.iskautils.data.VectorFactorType;
import net.unfamily.iskautils.item.custom.VectorCharmItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestisce il movimento del giocatore quando ha il Vector Charm equipaggiato
 */
public class VectorCharmMovement {
    private static final Logger LOGGER = LoggerFactory.getLogger(VectorCharmMovement.class);
    
    // Flag per tracciare se il giocatore era in hover mode nello scorso tick
    private static boolean wasInHoverMode = false;
    
    // Contatore per l'effetto paracadute
    private static int parachuteTicksLeft = 0;
    
    // Durata dell'effetto paracadute (in tick)
    private static final int PARACHUTE_DURATION = 40; // 2 secondi
    
    // Valore dell'effetto paracadute
    private static final double PARACHUTE_EFFECT = 0.03; // Caduta lenta

    /**
     * Applica il movimento in base ai fattori del Vector Charm
     * @param player Il giocatore a cui applicare il movimento
     */
    public static void applyMovement(Player player) {
        if (player == null) return;

        // Verifica se i charm sono abilitati
        if (!Config.verticalCharmEnabled && !Config.horizontalCharmEnabled) return;
        
        // Verifica se il giocatore ha il Vector Charm equipaggiato
        if (!VectorCharmItem.hasVectorCharm(player)) {
            wasInHoverMode = false;
            parachuteTicksLeft = 0;
            return;
        }

        // Ottieni i fattori del charm
        byte verticalFactorValue = VectorCharmData.getInstance().getVerticalFactor(player);
        byte horizontalFactorValue = VectorCharmData.getInstance().getHorizontalFactor(player);

        // Verifica se è attiva la modalità hover (valore speciale 6)
        boolean isHoverMode = verticalFactorValue == VectorCharmData.HOVER_MODE_VALUE;
        
        // Controlla se è stata appena disattivata la hover mode
        if (wasInHoverMode && !isHoverMode) {
            // Inizia l'effetto paracadute
            parachuteTicksLeft = PARACHUTE_DURATION;
            LOGGER.debug("Hover mode deactivated, parachute effect activated for {} ticks", PARACHUTE_DURATION);
        }
        
        // Aggiorna lo stato dell'hover per il prossimo tick
        wasInHoverMode = isHoverMode;

        // Verifica se ci sono fattori attivi
        boolean hasVerticalFactor = Config.verticalCharmEnabled && 
                                   (verticalFactorValue > 0 || parachuteTicksLeft > 0);
        boolean hasHorizontalFactor = Config.horizontalCharmEnabled && horizontalFactorValue > 0;

        // Se non ci sono fattori attivi, esci
        if (!hasVerticalFactor && !hasHorizontalFactor) return;

        // Gestisce il movimento verticale
        if (hasVerticalFactor || isHoverMode) {
            applyVerticalMovement(player, verticalFactorValue, isHoverMode);
        }

        // Gestisce il movimento orizzontale
        if (hasHorizontalFactor) {
            applyHorizontalMovement(player, horizontalFactorValue);
        }
        
        // Riduce il tempo dell'effetto paracadute se attivo
        if (parachuteTicksLeft > 0) {
            parachuteTicksLeft--;
            
            if (parachuteTicksLeft == 0) {
                LOGGER.debug("Parachute effect has ended");
            }
        }
    }

    /**
     * Applica il movimento verticale
     * @param player Il giocatore a cui applicare il movimento
     * @param factorValue Il valore del fattore verticale
     * @param isHoverMode True se è attiva la modalità hover
     */
    private static void applyVerticalMovement(Player player, byte factorValue, boolean isHoverMode) {
        // Ottieni il movimento attuale
        Vec3 currentMotion = player.getDeltaMovement();

        if (isHoverMode) {
            // Modalità hover: imposta direttamente il valore dal config
            double hoverValue = Config.hoverValue;
            player.setDeltaMovement(
                currentMotion.x,
                hoverValue,
                currentMotion.z
            );
            LOGGER.debug("Applied hover mode with value: {}", hoverValue);
        } else if (parachuteTicksLeft > 0) {
            // Effetto paracadute attivo dopo la disattivazione dell'hover
            
            // Se il giocatore sta scendendo, rallenta la discesa
            if (currentMotion.y < 0) {
                double parachuteStrength = PARACHUTE_EFFECT * ((double) parachuteTicksLeft / PARACHUTE_DURATION);
                
                player.setDeltaMovement(
                    currentMotion.x,
                    Math.max(currentMotion.y, -parachuteStrength), // Limita la velocità di discesa
                    currentMotion.z
                );
                LOGGER.debug("Applied parachute effect with strength: {}, resulting in Y motion: {}", 
                        parachuteStrength, Math.max(currentMotion.y, -parachuteStrength));
            }
        } else if (factorValue > 0) {
            // Modalità normale: ottieni il valore direttamente dal config
            double speed = getVectorSpeed(factorValue);
            
            // Aggiungi il fattore del player dal config invece di moltiplicare
            // In questo modo, emula il comportamento delle vector plates per player
            double verticalBoost = speed + Config.verticalBoostFactor;
            
            // Simula il comportamento della VectorBlock.applyVerticalMovement
            // Usa le stesse costanti e logica per mantenere la coerenza
            double accelerationFactor = 0.0; // Stesso valore di VectorBlock
            
            // Calcola la nuova velocità verticale - non usare Math.max per permettere la discesa
            double targetY = verticalBoost;
            double newY = (currentMotion.y * (1 - accelerationFactor)) + (targetY * accelerationFactor);
            
            player.setDeltaMovement(
                currentMotion.x,
                newY,
                currentMotion.z
            );
            LOGGER.debug("Applied vertical movement with factor: {}, speed: {}, boost: {}, resulting in Y motion: {}", 
                    VectorFactorType.fromByte(factorValue).getName(), speed, verticalBoost, newY);
        }
        
        // Previeni danni da caduta
        player.fallDistance = 0;
        
        // Conferma gli aggiornamenti fisici
        player.hurtMarked = true;
    }

    /**
     * Applica il movimento orizzontale
     * @param player Il giocatore a cui applicare il movimento
     * @param factorValue Il valore del fattore orizzontale
     */
    private static void applyHorizontalMovement(Player player, byte factorValue) {
        // Ottieni il movimento attuale
        Vec3 currentMotion = player.getDeltaMovement();
        
        // Ottieni la direzione in cui il giocatore sta guardando
        Vec3 lookVec = player.getLookAngle();
        
        // Ottieni il valore direttamente dal config
        double speed = getVectorSpeed(factorValue);
        
        // Calcola le nuove componenti di velocità
        double targetX = lookVec.x * speed;
        double targetZ = lookVec.z * speed;
        
        // Applica l'accelerazione graduale - valore preso da VectorBlock
        double accelerationFactor = 0.6;
        double conserveFactor = 0.75; // Keep 75% of lateral velocity - valore preso da VectorBlock
        
        // Applica l'accelerazione graduale
        double newX = (currentMotion.x * (1 - accelerationFactor)) + (targetX * accelerationFactor);
        double newZ = (currentMotion.z * (1 - accelerationFactor)) + (targetZ * accelerationFactor);
        
        // Imposta il nuovo movimento
        player.setDeltaMovement(
            newX,
            currentMotion.y,
            newZ
        );
        
        // Conferma gli aggiornamenti fisici
        player.hurtMarked = true;
        
        LOGGER.debug("Applied horizontal movement with factor: {}, speed: {}", 
                VectorFactorType.fromByte(factorValue).getName(), speed);
    }
    
    /**
     * Ottiene la velocità Vector corrispondente al fattore specificato
     * @param factorValue Il valore del fattore (0-5)
     * @return La velocità corrispondente
     */
    private static double getVectorSpeed(byte factorValue) {
        VectorFactorType factorType = VectorFactorType.fromByte(factorValue);
        return switch (factorType) {
            case SLOW -> Config.slowVectorSpeed;
            case MODERATE -> Config.moderateVectorSpeed;
            case FAST -> Config.fastVectorSpeed;
            case EXTREME -> Config.extremeVectorSpeed;
            case ULTRA -> Config.ultraVectorSpeed;
            default -> 0.0;
        };
    }
} 