package net.unfamily.iskautils.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.unfamily.iskautils.events.LivingIncomingDamageEventHandler;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.item.custom.GreedyShieldItem;
import net.unfamily.iskautils.item.custom.artifact.CursedArtifactItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal equip stages for Curios-only activation.
 */
public final class ArtifactEquipStages {
    public static final String OLD_BRICK = "iska_utils_internal-old_brick_equip";
    public static final String CHOSEN_CHEESE = "iska_utils_internal-chosen_cheese_equip";
    public static final String SHARPENED_BONE = "iska_utils_internal-sharpened_bone_equip";
    public static final String THE_ROOTS = "iska_utils_internal-the_roots_equip";
    public static final String MINING_EQUITIZER = "iska_utils_internal-mining_equitizer_equip";
    public static final String TOTEM_OF_PAIN = "iska_utils_internal-totem_of_pain_equip";
    public static final String BUSTED_CROWN = "iska_utils_internal-busted_crown_equip";
    public static final String RITUAL_GAUNTLET = "iska_utils_internal-ritual_gauntlet_equip";
    public static final String THE_DECEPTION = "iska_utils_internal-the_deception_equip";
    public static final String GREEDY_SHIELD = GreedyShieldItem.EQUIP_STAGE;
    public static final String NECRO_CRYSTAL_HEART = LivingIncomingDamageEventHandler.NECRO_CRYSTAL_HEART_EQUIP_STAGE;
    public static final String ANCIENT_STAR = "iska_utils_internal-ancient_star_equip";
    public static final String MINIATURE_TENT = "iska_utils_internal-miniature_tent_equip";
    public static final String RUNIC_DICE = "iska_utils_internal-runic_dice_equip";

    private static final List<String> ALL_STAGES = List.of(
            OLD_BRICK,
            CHOSEN_CHEESE,
            SHARPENED_BONE,
            THE_ROOTS,
            MINING_EQUITIZER,
            TOTEM_OF_PAIN,
            BUSTED_CROWN,
            RITUAL_GAUNTLET,
            THE_DECEPTION,
            GREEDY_SHIELD,
            NECRO_CRYSTAL_HEART,
            ANCIENT_STAR,
            MINIATURE_TENT,
            RUNIC_DICE
    );

    private static final Map<Item, String> STAGE_BY_ITEM = buildStageMap();

    private ArtifactEquipStages() {}

    public static List<String> allStages() {
        return ALL_STAGES;
    }

    public static String stageForItem(Item item) {
        return STAGE_BY_ITEM.get(item);
    }

    public static String stageForCursedArtifact(Item item) {
        if (!(item instanceof CursedArtifactItem)) {
            return null;
        }
        return "iska_utils_internal-" + BuiltInRegistries.ITEM.getKey(item).getPath() + "_equip";
    }

    private static Map<Item, String> buildStageMap() {
        Map<Item, String> map = new HashMap<>();
        map.put(ModItems.OLD_BRICK.get(), OLD_BRICK);
        map.put(ModItems.CHOSEN_CHEESE.get(), CHOSEN_CHEESE);
        map.put(ModItems.SHARPENED_BONE.get(), SHARPENED_BONE);
        map.put(ModItems.THE_ROOTS.get(), THE_ROOTS);
        map.put(ModItems.MINING_EQUITIZER.get(), MINING_EQUITIZER);
        map.put(ModItems.TOTEM_OF_PAIN.get(), TOTEM_OF_PAIN);
        map.put(ModItems.BUSTED_CROWN.get(), BUSTED_CROWN);
        map.put(ModItems.RITUAL_GAUNTLET.get(), RITUAL_GAUNTLET);
        map.put(ModItems.THE_DECEPTION.get(), THE_DECEPTION);
        map.put(ModItems.GREEDY_SHIELD.get(), GREEDY_SHIELD);
        map.put(ModItems.NECROTIC_CRYSTAL_HEART.get(), NECRO_CRYSTAL_HEART);
        map.put(ModItems.ANCIENT_STAR.get(), ANCIENT_STAR);
        map.put(ModItems.MINIATURE_TENT.get(), MINIATURE_TENT);
        map.put(ModItems.RUNIC_DICE.get(), RUNIC_DICE);
        return Collections.unmodifiableMap(map);
    }
}
