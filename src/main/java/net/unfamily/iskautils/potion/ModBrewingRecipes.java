package net.unfamily.iskautils.potion;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.unfamily.iskautils.item.ModItems;

@EventBusSubscriber
public final class ModBrewingRecipes {
    private ModBrewingRecipes() {}

    @SubscribeEvent
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();
        builder.addMix(Potions.AWKWARD, ModItems.ENTROPIC_AGGLOMERATION.get(), ModPotions.ENTROPIC);
        builder.addMix(ModPotions.ENTROPIC, Items.REDSTONE, ModPotions.LONG_ENTROPIC);
        builder.addMix(ModPotions.ENTROPIC, Items.GLOWSTONE_DUST, ModPotions.STRONG_ENTROPIC);
    }
}
