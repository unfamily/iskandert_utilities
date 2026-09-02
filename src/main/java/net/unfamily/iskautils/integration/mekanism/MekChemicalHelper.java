package net.unfamily.iskautils.integration.mekanism;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.unfamily.iskautils.util.ModLogger;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Reflection bridge to Mekanism chemicals (optional dependency).
 * <p>
 * Gas support is disabled on NeoForge 26.x until Mekanism ships for that loader.
 */
public final class MekChemicalHelper {

    private static final ModLogger LOGGER = ModLogger.of(MekChemicalHelper.class);

    /** {@code false} on 26.x — enable when Mekanism for this loader is supported. */
    public static final boolean GAS_SUPPORT_ENABLED = false;

    private MekChemicalHelper() {}

    public static boolean isGasSupportEnabled() {
        return GAS_SUPPORT_ENABLED;
    }

    public static boolean isLoaded() {
        return GAS_SUPPORT_ENABLED && ModList.get().isLoaded("mekanism");
    }

    @Nullable
    private static Object chemicalRegistry() {
        if (!isLoaded()) {
            return null;
        }
        try {
            Class<?> api = Class.forName("mekanism.api.MekanismAPI");
            return api.getField("CHEMICAL_REGISTRY").get(null);
        } catch (Throwable t) {
            LOGGER.warn("Could not access Mekanism CHEMICAL_REGISTRY: {}", t.getMessage());
            return null;
        }
    }

    @Nullable
    public static Object createAllValidTank(long capacityMb) {
        if (!isLoaded()) {
            return null;
        }
        try {
            Class<?> tankClass = Class.forName("mekanism.api.chemical.BasicChemicalTank");
            Class<?> listenerClass = Class.forName("mekanism.api.IContentsListener");
            Method create = tankClass.getMethod("createAllValid", long.class, listenerClass);
            return create.invoke(null, capacityMb, null);
        } catch (Throwable t) {
            LOGGER.warn("Could not create Mek chemical tank: {}", t.toString());
            return null;
        }
    }

    public static boolean isEmpty(@Nullable Object chemicalStack) {
        if (chemicalStack == null) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(chemicalStack.getClass().getMethod("isEmpty").invoke(chemicalStack));
        } catch (Throwable e) {
            try {
                return ((Number) chemicalStack.getClass().getMethod("getAmount").invoke(chemicalStack)).longValue() <= 0;
            } catch (Throwable e2) {
                return true;
            }
        }
    }

    public static long getAmount(@Nullable Object chemicalStack) {
        if (chemicalStack == null) {
            return 0;
        }
        try {
            return ((Number) chemicalStack.getClass().getMethod("getAmount").invoke(chemicalStack)).longValue();
        } catch (Throwable e) {
            return 0;
        }
    }

    @Nullable
    public static String getRegistryName(@Nullable Object chemicalStack) {
        return getTypeRegistryName(chemicalStack);
    }

    @Nullable
    public static String getTypeRegistryName(@Nullable Object chemicalStack) {
        if (chemicalStack == null || isEmpty(chemicalStack)) {
            return null;
        }
        try {
            Object rl = chemicalStack.getClass().getMethod("getTypeRegistryName").invoke(chemicalStack);
            return rl != null ? rl.toString() : null;
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isRadioactiveStack(@Nullable Object chemicalStack) {
        if (chemicalStack == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(chemicalStack.getClass().getMethod("isRadioactive").invoke(chemicalStack));
        } catch (Throwable e) {
            return false;
        }
    }

    @Nullable
    public static Object getChemicalInTank(@Nullable Object handler, int tank) {
        if (handler == null) {
            return null;
        }
        try {
            return handler.getClass().getMethod("getChemicalInTank", int.class).invoke(handler, tank);
        } catch (Throwable e) {
            return null;
        }
    }

    public static boolean isRadioactiveInTank(@Nullable Object handler) {
        return isRadioactiveStack(getChemicalInTank(handler, 0));
    }

    public static long getTankAmountLong(@Nullable Object handler) {
        try {
            return Math.max(0L, getAmount(getChemicalInTank(handler, 0)));
        } catch (Throwable e) {
            return 0L;
        }
    }

    public static long getTankCapacityLong(@Nullable Object handler) {
        if (handler == null) {
            return 0L;
        }
        try {
            return Math.max(0L, ((Number) handler.getClass().getMethod("getChemicalTankCapacity", int.class)
                    .invoke(handler, 0)).longValue());
        } catch (Throwable e) {
            return 0L;
        }
    }

    public static void setTankCapacity(@Nullable Object handler, long capacityMb) {
        if (handler == null || capacityMb <= 0) {
            return;
        }
        try {
            handler.getClass().getMethod("setCapacity", long.class).invoke(handler, capacityMb);
        } catch (Throwable ignored) {
            // Some Mek builds expose capacity only at create time
        }
    }

    @Nullable
    public static Object createStack(Identifier chemicalId, long amount) {
        if (!isLoaded() || amount <= 0 || chemicalId == null) {
            return null;
        }
        Object registry = chemicalRegistry();
        if (registry == null) {
            return null;
        }
        try {
            Object holder = resolveHolder(registry, chemicalId);
            if (holder == null) {
                return null;
            }
            Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            Class<?> holderClass = Class.forName("net.minecraft.core.Holder");
            return stackClass.getConstructor(holderClass, long.class).newInstance(holder, amount);
        } catch (Throwable t) {
            LOGGER.debug("createStack failed for {}: {}", chemicalId, t.getMessage());
            return null;
        }
    }

    @Nullable
    public static Object createStackFromId(@Nullable String id, long amount) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Identifier parsed = Identifier.tryParse(id.trim());
        return parsed != null ? createStack(parsed, amount) : null;
    }

    public static int fill(@Nullable Object handler, @Nullable Object stack, boolean simulate) {
        if (handler == null || stack == null || isEmpty(stack)) {
            return 0;
        }
        try {
            Class<?> actionClass = Class.forName("mekanism.api.Action");
            Object action = actionClass.getField(simulate ? "SIMULATE" : "EXECUTE").get(null);
            Class<?> stackClass = stack.getClass();
            Object result;
            try {
                result = handler.getClass().getMethod("insertChemical", stackClass, actionClass)
                        .invoke(handler, stack, action);
            } catch (NoSuchMethodException e) {
                result = handler.getClass().getMethod("insertChemical", int.class, stackClass, actionClass)
                        .invoke(handler, 0, stack, action);
            }
            if (result == null) {
                return 0;
            }
            long remaining = getAmount(result);
            long wanted = getAmount(stack);
            return (int) Math.min(wanted - remaining, Integer.MAX_VALUE);
        } catch (Throwable e) {
            LOGGER.debug("fill failed: {}", e.getMessage());
            return 0;
        }
    }

    public static int extractFromTank(@Nullable Object handler, long amount) {
        if (handler == null || amount <= 0) {
            return 0;
        }
        try {
            Object inTank = getChemicalInTank(handler, 0);
            if (isEmpty(inTank)) {
                return 0;
            }
            long drain = Math.min(amount, getAmount(inTank));
            Class<?> actionClass = Class.forName("mekanism.api.Action");
            Object exec = actionClass.getField("EXECUTE").get(null);
            Object drained = handler.getClass().getMethod("extractChemical", int.class, long.class, actionClass)
                    .invoke(handler, 0, drain, exec);
            return (int) Math.min(getAmount(drained), Integer.MAX_VALUE);
        } catch (Throwable t) {
            LOGGER.debug("extractFromTank failed: {}", t.getMessage());
            return 0;
        }
    }

    public static boolean dumpTank(@Nullable Object handler) {
        if (handler == null || isEmpty(getChemicalInTank(handler, 0))) {
            return false;
        }
        long amount = getTankAmountLong(handler);
        return extractFromTank(handler, amount) > 0;
    }

    public static boolean chemicalIdExists(@Nullable String id) {
        if (!isLoaded() || id == null || id.isBlank()) {
            return false;
        }
        Identifier parsed = Identifier.tryParse(id.trim());
        if (parsed == null) {
            return false;
        }
        Object stack = createStack(parsed, 1);
        return stack != null && !isEmpty(stack);
    }

    public static boolean isRadioactiveGasId(@Nullable String registryName) {
        if (!isLoaded() || registryName == null || registryName.isBlank()) {
            return false;
        }
        return isRadioactiveStack(createStackFromId(registryName, 1));
    }

    public static int getTint(@Nullable Object chemicalStack) {
        if (chemicalStack == null || isEmpty(chemicalStack)) {
            return 0;
        }
        try {
            return (int) chemicalStack.getClass().getMethod("getChemicalTint").invoke(chemicalStack);
        } catch (Throwable e) {
            return 0;
        }
    }

    public static Component getDisplayName(@Nullable Object chemicalStack) {
        if (chemicalStack == null || isEmpty(chemicalStack)) {
            return Component.empty();
        }
        try {
            Object text = chemicalStack.getClass().getMethod("getTextComponent").invoke(chemicalStack);
            if (text instanceof Component component) {
                return component;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object chemical = chemicalStack.getClass().getMethod("getChemical").invoke(chemicalStack);
            if (chemical != null) {
                Object text = chemical.getClass().getMethod("getTextComponent").invoke(chemical);
                if (text instanceof Component component) {
                    return component;
                }
            }
        } catch (Throwable ignored) {
        }
        String id = getTypeRegistryName(chemicalStack);
        return id != null ? Component.literal(id) : Component.empty();
    }

    @Nullable
    private static Object resolveHolder(Object registry, Identifier id) {
        try {
            Object opt = registry.getClass().getMethod("get", Identifier.class).invoke(registry, id);
            if (opt instanceof Optional<?> optional) {
                return optional.orElse(null);
            }
            return opt;
        } catch (Throwable ignored) {
        }
        try {
            Method getHolder = registry.getClass().getMethod("getHolder", Identifier.class);
            Object opt = getHolder.invoke(registry, id);
            if (opt instanceof Optional<?> optional) {
                return optional.orElse(null);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
