package net.unfamily.iskautils.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.unfamily.iskautils.IskaUtils;

public final class ModDamageTypes {

    public static final ResourceKey<DamageType> MOB_REAPER = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(IskaUtils.MOD_ID, "mob_reaper")
    );

    private ModDamageTypes() {
    }
}
