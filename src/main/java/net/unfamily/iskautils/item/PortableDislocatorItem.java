package net.unfamily.iskautils.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class PortableDislocatorItem extends Item {
    
    public PortableDislocatorItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.iska_utils.portable_dislocator.tooltip.main", "P"));
        tooltip.add(Component.translatable("item.iska_utils.portable_dislocator.tooltip.compasses"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}