package net.unfamily.iskautils.integration.jei;

import java.util.List;

/**
 * One JEI page: parcel input and up to {@link SuspiciousDeliveryJeiRecipes#OUTPUTS_PER_PAGE} loot entries.
 */
public record SuspiciousDeliveryJeiRecipe(List<SuspiciousDeliveryJeiEntry> entries) {

    public SuspiciousDeliveryJeiRecipe {
        entries = List.copyOf(entries);
    }
}
