package net.unfamily.iskautils.data;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.List;

public record ToolBehaviorDefinition(
        Identifier itemId,
        ToolBehaviorType behavior,
        int range,
        List<TagKey<Block>> harvestTags) {

    public ToolBehaviorDefinition {
        if (range < 0) {
            range = 0;
        }
        harvestTags = harvestTags == null ? List.of() : List.copyOf(harvestTags);
    }
}
