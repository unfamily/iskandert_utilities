package net.unfamily.iskautils.arcane.effects;

import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffect;
import net.unfamily.iskautils.arcane.ArcaneDictionaryEffectContext;
import net.unfamily.iskautils.arcane.ArcaneDictionaryShiftingState;
import net.unfamily.iskautils.arcane.ArcaneDictionaryTraitIds;
import net.unfamily.iskautils.arcane.jei.ArcaneDictionaryJeiContext;
import net.unfamily.iskautils.Config;

import java.util.List;

public final class ShiftingPowerEffect implements ArcaneDictionaryEffect {
    @Override
    public net.minecraft.resources.ResourceLocation id() {
        return ArcaneDictionaryTraitIds.SHIFTING_POWER;
    }

    @Override
    public void onPlayerTick(ArcaneDictionaryEffectContext ctx) {
        ArcaneDictionaryShiftingState.tick(
                ctx.dictionaryStack(),
                ctx.player().getRandom(),
                ctx.player().level().getGameTime());
    }

    @Override
    public void appendJeiDescription(ArcaneDictionaryJeiContext ctx, List<Component> lines) {
        int seconds = Config.arcaneShiftingPowerMimicDurationSeconds;
        lines.add(Component.translatable("jei.iska_utils.arcane_trait.iska_utils.shifting_power.desc", seconds));
    }
}
