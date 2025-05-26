package net.unfamily.iskautils.data;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.unfamily.iskautils.IskaUtils;

/**
 * Data provider for generating potion plate models and blockstates during build
 * This replaces runtime generation which doesn't work in JAR files
 */
public class PotionPlateDataProvider {
    
    public static class BlockStates extends BlockStateProvider {
        public BlockStates(PackOutput output, ExistingFileHelper exFileHelper) {
            super(output, IskaUtils.MOD_ID, exFileHelper);
        }

        @Override
        protected void registerStatesAndModels() {
            // Generate models for known potion plates
            // This would need to be expanded based on your specific plates
            
            // Example for slowness plate
            createPotionPlateModel("plate_trap_iska_utils-slowness", "plate_trap_slowness");
            createPotionPlateModel("plate_trap_iska_utils-poison", "plate_trap_poison");
            // Add more as needed...
        }
        
        private void createPotionPlateModel(String blockName, String textureName) {
            ModelFile model = models().withExistingParent(blockName, modLoc("block/plate_base"))
                .texture("top", modLoc("block/potion_plates/iska_utils/" + textureName))
                .texture("bottom", modLoc("block/plate_base"));
                
            simpleBlock(null, model); // We can't reference the block here since it's dynamic
        }
    }
    
    public static class ItemModels extends ItemModelProvider {
        public ItemModels(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, IskaUtils.MOD_ID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            // Generate item models for potion plates
            createPotionPlateItemModel("plate_trap_iska_utils-slowness");
            createPotionPlateItemModel("plate_trap_iska_utils-poison");
            // Add more as needed...
        }
        
        private void createPotionPlateItemModel(String blockName) {
            withExistingParent(blockName, modLoc("block/" + blockName));
        }
    }
} 