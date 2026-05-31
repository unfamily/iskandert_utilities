package net.unfamily.iskautils.arcane;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class ArcaneDictionaryTraitDispatcher {
    private ArcaneDictionaryTraitDispatcher() {}

    public static void dispatchPlayerTick(ServerPlayer player, ItemStack dictionary, boolean curioFull) {
        long gameTime = player.level().getGameTime();
        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (!traitApplies(player, trait, curioFull)) {
                continue;
            }
            ArcaneDictionaryEffect effect = ArcaneDictionaryEffectRegistry.get(trait.id());
            if (effect == null) {
                continue;
            }
            ArcaneDictionaryEffectContext ctx =
                    new ArcaneDictionaryEffectContext(player, dictionary, trait.id(), trait.level());
            effect.onPlayerTick(ctx);
        }
        if (curioFull) {
            dispatchMimicTick(player, dictionary, gameTime);
        }
    }

    public static void dispatchPlayerTick(ServerPlayer player, ItemStack dictionary) {
        dispatchPlayerTick(player, dictionary, true);
    }

    public static void dispatchVictimDamaged(
            ServerPlayer player, ItemStack dictionary, LivingIncomingDamageEvent event, boolean curioFull) {
        long gameTime = player.level().getGameTime();
        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (!traitApplies(player, trait, curioFull)) {
                continue;
            }
            ArcaneDictionaryEffect effect = ArcaneDictionaryEffectRegistry.get(trait.id());
            if (effect == null) {
                continue;
            }
            effect.onVictimDamaged(
                    new ArcaneDictionaryEffectContext(player, dictionary, trait.id(), trait.level()),
                    event);
        }
        if (curioFull) {
            dispatchMimicVictimDamaged(player, dictionary, gameTime, event);
        }
    }

    public static void dispatchVictimDamaged(ServerPlayer player, ItemStack dictionary, LivingIncomingDamageEvent event) {
        dispatchVictimDamaged(player, dictionary, event, true);
    }

    public static void dispatchPlayerAttack(
            ServerPlayer player, ItemStack dictionary, LivingIncomingDamageEvent event, boolean curioFull) {
        long gameTime = player.level().getGameTime();
        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (!traitApplies(player, trait, curioFull)) {
                continue;
            }
            ArcaneDictionaryEffect effect = ArcaneDictionaryEffectRegistry.get(trait.id());
            if (effect == null) {
                continue;
            }
            effect.onPlayerAttack(
                    new ArcaneDictionaryEffectContext(player, dictionary, trait.id(), trait.level()),
                    event);
        }
        if (curioFull) {
            dispatchMimicPlayerAttack(player, dictionary, gameTime, event);
        }
    }

    public static void dispatchPlayerAttack(ServerPlayer player, ItemStack dictionary, LivingIncomingDamageEvent event) {
        dispatchPlayerAttack(player, dictionary, event, true);
    }

    public static int adjustExperiencePickup(ServerPlayer player, ItemStack dictionary, int xp, boolean curioFull) {
        if (xp <= 0) {
            return xp;
        }
        long gameTime = player.level().getGameTime();
        int adjusted = xp;
        for (ArcaneDictionaryContents.TraitSlot trait : ArcaneDictionaryContents.getTraits(dictionary)) {
            if (!traitApplies(player, trait, curioFull)) {
                continue;
            }
            ArcaneDictionaryEffect effect = ArcaneDictionaryEffectRegistry.get(trait.id());
            if (effect == null) {
                continue;
            }
            adjusted = effect.adjustExperiencePickup(
                    new ArcaneDictionaryEffectContext(player, dictionary, trait.id(), trait.level()),
                    adjusted);
        }
        if (curioFull) {
            adjusted = dispatchMimicExperiencePickup(player, dictionary, gameTime, adjusted);
        }
        return Math.max(0, adjusted);
    }

    public static int adjustExperiencePickup(ServerPlayer player, ItemStack dictionary, int xp) {
        return adjustExperiencePickup(player, dictionary, xp, true);
    }

    private static boolean traitApplies(
            ServerPlayer player,
            ArcaneDictionaryContents.TraitSlot trait,
            boolean curioFull) {
        if (!ArcaneDictionaryEntryGate.traitActive(player, trait.id())) {
            return false;
        }
        if (curioFull) {
            return true;
        }
        return ArcaneDictionaryActivation.traitApplies(trait.id(), ArcaneDictionaryActivation.Scope.OFF_CURIO);
    }

    private static void dispatchMimicTick(ServerPlayer player, ItemStack dictionary, long gameTime) {
        MimicContext mimic = activeMimic(player, dictionary, gameTime);
        if (mimic == null) {
            return;
        }
        mimic.effect().onPlayerTick(mimic.context());
    }

    private static void dispatchMimicVictimDamaged(
            ServerPlayer player,
            ItemStack dictionary,
            long gameTime,
            LivingIncomingDamageEvent event) {
        MimicContext mimic = activeMimic(player, dictionary, gameTime);
        if (mimic == null) {
            return;
        }
        mimic.effect().onVictimDamaged(mimic.context(), event);
    }

    private static void dispatchMimicPlayerAttack(
            ServerPlayer player,
            ItemStack dictionary,
            long gameTime,
            LivingIncomingDamageEvent event) {
        MimicContext mimic = activeMimic(player, dictionary, gameTime);
        if (mimic == null) {
            return;
        }
        mimic.effect().onPlayerAttack(mimic.context(), event);
    }

    private static int dispatchMimicExperiencePickup(
            ServerPlayer player,
            ItemStack dictionary,
            long gameTime,
            int xp) {
        MimicContext mimic = activeMimic(player, dictionary, gameTime);
        if (mimic == null) {
            return xp;
        }
        return mimic.effect().adjustExperiencePickup(mimic.context(), xp);
    }

    private static MimicContext activeMimic(ServerPlayer player, ItemStack dictionary, long gameTime) {
        if (ArcaneDictionaryShiftingState.shiftingLevel(dictionary) <= 0) {
            return null;
        }
        if (!ArcaneDictionaryShiftingState.isActive(dictionary, gameTime)) {
            return null;
        }
        ArcaneDictionaryShiftingState.MimicState state = ArcaneDictionaryShiftingState.read(dictionary);
        if (state == null) {
            return null;
        }
        if (!ArcaneDictionaryEntryGate.traitActive(player, state.traitId())) {
            return null;
        }
        ArcaneDictionaryEffect effect = ArcaneDictionaryEffectRegistry.get(state.traitId());
        if (effect == null) {
            return null;
        }
        int level = ArcaneDictionaryShiftingState.shiftingLevel(dictionary);
        ArcaneDictionaryEffectContext ctx =
                new ArcaneDictionaryEffectContext(player, dictionary, state.traitId(), level);
        return new MimicContext(effect, ctx);
    }

    private record MimicContext(ArcaneDictionaryEffect effect, ArcaneDictionaryEffectContext context) {}
}
