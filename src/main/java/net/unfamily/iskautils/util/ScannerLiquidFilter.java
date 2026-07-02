package net.unfamily.iskautils.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Liquid scan filter for scanner generic target strings ({@code liquid:all}, {@code liquid:<fluid_id>}).
 */
public final class ScannerLiquidFilter {
    public static final String ALL = "liquid:all";
    public static final String PREFIX = "liquid:";

    private ScannerLiquidFilter() {}

    public static boolean isLiquidScanTarget(String genericTarget) {
        return genericTarget != null && genericTarget.startsWith("liquid");
    }

    public static boolean isAllFluids(String genericTarget) {
        return genericTarget == null
                || "liquid".equals(genericTarget)
                || ALL.equals(genericTarget);
    }

    public static Identifier getFluidId(String genericTarget) {
        if (isAllFluids(genericTarget)) {
            return null;
        }
        if (genericTarget.startsWith(PREFIX)) {
            String id = genericTarget.substring(PREFIX.length());
            if (!id.isEmpty()) {
                return normalizeFluidId(Identifier.parse(id));
            }
        }
        return null;
    }

    public static String fluidTarget(Fluid fluid) {
        return PREFIX + normalizeFluidId(BuiltInRegistries.FLUID.getKey(fluid));
    }

    public static Identifier normalizeFluidId(Fluid fluid) {
        return normalizeFluidId(BuiltInRegistries.FLUID.getKey(fluid));
    }

    public static Identifier normalizeFluidId(Identifier fluidId) {
        if (fluidId == null) {
            return fluidId;
        }
        if (fluidId.getPath().equals("flowing_water")) {
            return BuiltInRegistries.FLUID.getKey(Fluids.WATER);
        }
        if (fluidId.getPath().equals("flowing_lava")) {
            return BuiltInRegistries.FLUID.getKey(Fluids.LAVA);
        }
        if (fluidId.getPath().startsWith("flowing_")) {
            String stillPath = fluidId.getPath().substring("flowing_".length());
            Identifier stillId = Identifier.fromNamespaceAndPath(fluidId.getNamespace(), stillPath);
            if (BuiltInRegistries.FLUID.containsKey(stillId)) {
                return stillId;
            }
        }
        return fluidId;
    }

    public static boolean matches(FluidState fluidState, String genericTarget) {
        if (fluidState.isEmpty()) {
            return false;
        }
        if (isAllFluids(genericTarget)) {
            return true;
        }
        Identifier filterId = getFluidId(genericTarget);
        if (filterId == null) {
            return true;
        }
        Identifier fluidId = normalizeFluidId(fluidState.getType());
        return filterId.equals(fluidId);
    }

    public static Component getLocalizedFluidName(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) {
            return Component.empty();
        }
        return Component.translatable(fluid.getFluidType().getDescriptionId());
    }

    public static Component getLocalizedFluidName(Identifier fluidId) {
        if (fluidId == null) {
            return Component.empty();
        }
        Fluid fluid = BuiltInRegistries.FLUID.getValue(fluidId);
        if (fluid != null && fluid != Fluids.EMPTY) {
            return getLocalizedFluidName(fluid);
        }
        return Component.translatable("fluid." + fluidId.getNamespace() + "." + fluidId.getPath());
    }
}
