package net.unfamily.iskautils.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.ModBlocks;
import net.unfamily.iskautils.block.VectorBlock;
import net.unfamily.iskautils.block.entity.EntropicSpawnerBlockEntity;
import net.unfamily.iskautils.particle.ModParticles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CrystalCageItem extends Item {

    private static final String NBT_ENTITY_DATA = "EntityData";
    private static final String NBT_ENTITY_TYPE = "EntityType";
    private static final String NBT_HAS_ENTITY  = "HasEntity";

    public CrystalCageItem(Properties properties) {
        super(properties);
    }

    // ── Capture on right-click entity ─────────────────────────────────────────

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack,
                                                           @NotNull Player player,
                                                           @NotNull LivingEntity target,
                                                           @NotNull InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (isFilled(stack)) return InteractionResult.PASS;
        if (target instanceof Player) return InteractionResult.PASS;
        if (target.isRemoved()) return InteractionResult.PASS;
        if (isCaptureBlacklisted(target)) {
            player.displayClientMessage(Component.translatable("message.iska_utils.crystal_cage.blacklisted"), true);
            return InteractionResult.FAIL;
        }

        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        ItemStack filledCage = capture(stack, target, serverLevel);
        handleStackResult(player, hand, stack, filledCage);

        serverLevel.playSound(null, target.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 1.0F, 1.3F);
        spawnPurpleFireBurst(serverLevel, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
        target.discard();
        return InteractionResult.SUCCESS;
    }

    // ── Release on right-click block (also handles plate conversion) ───────────

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        if (player == null) return InteractionResult.PASS;

        // Empty cage + plate_base_block → convert to crystal trap plate
        if (!isFilled(stack) && level.getBlockState(clickedPos).is(ModBlocks.PLATE_BASE_BLOCK.get())) {
            var existingState = level.getBlockState(clickedPos);
            var trapState = ModBlocks.CRYSTAL_CAGE_TRAP_PLATE.get().defaultBlockState()
                    .setValue(VectorBlock.FACING,   existingState.getValue(VectorBlock.FACING))
                    .setValue(VectorBlock.VERTICAL,  existingState.getValue(VectorBlock.VERTICAL));
            level.setBlock(clickedPos, trapState, 3);
            serverLevel.playSound(null, clickedPos,
                    SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.PLAYERS, 1.0F, 0.9F);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResult.SUCCESS;
        }

        // Filled cage on spawner / trial / entropic → act like a spawn egg
        if (isFilled(stack) && tryApplyAsSpawnEgg(stack, player, serverLevel, clickedPos)) {
            return InteractionResult.SUCCESS;
        }

        // Filled cage + any block face → release mob above the face
        if (isFilled(stack)) {
            BlockPos releasePos = clickedPos.relative(context.getClickedFace());
            release(stack, releasePos, serverLevel);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // ── Air click: do not release (require a block target) ────────────────────

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level,
                                                           @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        // Releasing into thin air would spawn the mob on the player — refuse for safety.
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static ItemStack capture(ItemStack original, LivingEntity entity, ServerLevel level) {
        CompoundTag entityNbt = new CompoundTag();
        entity.save(entityNbt);

        ItemStack filled = original.copyWithCount(1);
        CustomData existing = filled.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = existing.copyTag();
        nbt.put(NBT_ENTITY_DATA, entityNbt);
        nbt.putString(NBT_ENTITY_TYPE,
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        nbt.putBoolean(NBT_HAS_ENTITY, true);
        filled.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        return filled;
    }

    private static void release(ItemStack stack, BlockPos pos, ServerLevel level) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = customData.copyTag();
        if (!nbt.getBoolean(NBT_HAS_ENTITY)) return;

        String typeId = nbt.getString(NBT_ENTITY_TYPE);
        Optional<EntityType<?>> typeOpt = BuiltInRegistries.ENTITY_TYPE
                .getOptional(ResourceLocation.parse(typeId));
        if (typeOpt.isEmpty()) return;

        Entity entity = typeOpt.get().create(level);
        if (entity == null) return;

        entity.load(nbt.getCompound(NBT_ENTITY_DATA));
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(entity);

        // Clear cage data (remove component when empty so model predicate sees empty)
        nbt.remove(NBT_ENTITY_DATA);
        nbt.remove(NBT_ENTITY_TYPE);
        nbt.remove(NBT_HAS_ENTITY);
        nbt.remove("CustomModelData");
        if (nbt.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }

        level.playSound(null, pos,
                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        spawnPurpleFireBurst(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    /** Entropic flame burst used on capture and release. */
    public static void spawnPurpleFireBurst(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ModParticles.ENTROPIC_FLAME.get(), x, y, z, 28, 0.35D, 0.45D, 0.35D, 0.02D);
    }

    /**
     * Like a spawn egg on Spawner / Trial Spawner / Entropic Spawner when config 501 is enabled.
     * @return true if the click was handled as a spawn-egg application
     */
    public static boolean tryApplyAsSpawnEgg(ItemStack stack, Player player, Level level, BlockPos pos) {
        if (level.isClientSide()) return false;
        if (!Config.crystalCageActsAsSpawnEggOnSpawners) return false;
        if (!isFilled(stack)) return false;
        EntityType<?> type = getStoredEntityType(stack);
        if (type == null) return false;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Spawner spawner) {
            spawner.setEntityId(type, level.getRandom());
            be.setChanged();
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
            consumeAsSpawnEgg(stack, player);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        if (be instanceof EntropicSpawnerBlockEntity entropic && entropic.setSpawnEntityType(type)) {
            consumeAsSpawnEgg(stack, player);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    /** Empty one cage use like consuming a spawn egg (creative keeps the filled cage). */
    public static void consumeAsSpawnEgg(ItemStack stack, Player player) {
        if (player != null && player.getAbilities().instabuild) return;
        if (stack.getCount() > 1) {
            stack.shrink(1);
        } else {
            clearStoredEntity(stack);
        }
    }

    public static void clearStoredEntity(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = customData.copyTag();
        nbt.remove(NBT_ENTITY_DATA);
        nbt.remove(NBT_ENTITY_TYPE);
        nbt.remove(NBT_HAS_ENTITY);
        nbt.remove("CustomModelData");
        if (nbt.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    @Nullable
    public static EntityType<?> getStoredEntityType(ItemStack stack) {
        if (!isFilled(stack)) return null;
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        String typeId = customData.copyTag().getString(NBT_ENTITY_TYPE);
        if (typeId.isEmpty()) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(typeId)).orElse(null);
    }

    private static void handleStackResult(Player player, InteractionHand hand,
                                           ItemStack original, ItemStack filled) {
        if (original.getCount() == 1) {
            // Replace the single stack in hand
            player.setItemInHand(hand, filled);
        } else {
            original.shrink(1);
            if (!player.addItem(filled)) {
                player.drop(filled, false);
            }
        }
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @NotNull TooltipContext context,
                                @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (isFilled(stack)) {
            CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            String typeId = customData.copyTag().getString(NBT_ENTITY_TYPE);
            if (!typeId.isEmpty()) {
                try {
                    Optional<EntityType<?>> opt = BuiltInRegistries.ENTITY_TYPE
                            .getOptional(ResourceLocation.parse(typeId));
                    opt.ifPresent(t ->
                            tooltip.add(Component.translatable(
                                    "tooltip.iska_utils.crystal_cage.contains",
                                    t.getDescription())));
                } catch (Exception ignored) {}
            }
        } else {
            tooltip.add(Component.translatable("tooltip.iska_utils.crystal_cage.empty"));
        }
    }

    // ── Static helpers ────────────────────────────────────────────────────────

    public static boolean isFilled(ItemStack stack) {
        CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return cd.copyTag().getBoolean(NBT_HAS_ENTITY);
    }

    /** True when this living entity cannot be captured (config blacklist: tags #… or entity ids). */
    public static boolean isCaptureBlacklisted(LivingEntity entity) {
        if (entity == null) return true;
        EntityType<?> type = entity.getType();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        List<String> list = Config.crystalCageCaptureBlacklist;
        if (list == null || list.isEmpty()) return false;
        for (String entry : list) {
            if (entry == null || entry.isBlank()) continue;
            String trimmed = entry.trim();
            if (trimmed.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(trimmed.substring(1));
                if (tagId == null) continue;
                TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, tagId);
                if (type.is(tag)) return true;
            } else if (typeId != null) {
                ResourceLocation id = ResourceLocation.tryParse(trimmed);
                if (id != null && typeId.equals(id)) return true;
            }
        }
        return false;
    }

    /**
     * Creates a filled crystal cage ItemStack containing the given entity.
     * Used by CrystalCageTrapPlateBlock to produce the drop.
     * Returns empty if the entity is blacklisted.
     */
    public static ItemStack createFilledCage(LivingEntity entity, ServerLevel level) {
        if (isCaptureBlacklisted(entity)) return ItemStack.EMPTY;
        ItemStack base = new ItemStack(net.unfamily.iskautils.item.ModItems.CRYSTAL_CAGE.get());
        return capture(base, entity, level);
    }
}
