package net.unfamily.iskautils.block.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.unfamily.iskautils.Config;
import net.unfamily.iskautils.block.MobReaperBlock;
import net.unfamily.iskautils.damage.ModDamageTypes;
import net.unfamily.iskautils.item.ModItems;
import net.unfamily.iskautils.util.MachineTargetType;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MobReaperBlockEntity extends BlockEntity implements MenuProvider {

    public enum MobAgeFilter {
        BOTH(0, "both"),
        ADULTS(1, "adults"),
        BABIES(2, "babies");

        private final int id;
        private final String name;

        MobAgeFilter(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public static MobAgeFilter fromId(int id) {
            for (MobAgeFilter filter : values()) {
                if (filter.id == id) {
                    return filter;
                }
            }
            return BOTH;
        }

        public MobAgeFilter cycle(boolean backward) {
            MobAgeFilter[] all = values();
            int index = ordinal();
            if (backward) {
                index = (index - 1 + all.length) % all.length;
            } else {
                index = (index + 1) % all.length;
            }
            return all[index];
        }
    }

    private MachineTargetType targetType = MachineTargetType.MOBS_ONLY;
    private MobAgeFilter ageFilter = MobAgeFilter.BOTH;
    private int redstoneMode = 0;
    private int attackCooldown = 0;
    private UUID ownerUuid;
    private boolean mountedOnPlate;

    private transient FakePlayer combatFakePlayer;

    private final ItemStackHandler moduleHandler = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> isValidDamageModule(stack);
                case 1 -> stack.is(ModItems.ENCHANT_MODULE.get());
                case 2 -> stack.is(ModItems.BEHEADING_MODULE.get());
                case 3 -> stack.is(ModItems.LUCK_MODULE.get());
                case 4 -> stack.is(ModItems.EXPERIENCE_MODULE.get());
                default -> false;
            };
        }

        @Override
        public int getSlotLimit(int slot) {
            return switch (slot) {
                case 0 -> {
                    ItemStack existing = getStackInSlot(0);
                    if (existing.is(ModItems.LETHAL_DAMAGE_MODULE.get())) {
                        yield Config.reaperLethalUpgradeMax;
                    }
                    yield Config.reaperNormalUpgradeMax;
                }
                case 1 -> Config.reaperEnchantUpgradeMax;
                case 2 -> Config.reaperBeheadingUpgradeMax;
                case 3 -> Config.reaperLuckUpgradeMax;
                case 4 -> Config.reaperExperienceUpgradeMax;
                default -> super.getSlotLimit(slot);
            };
        }
    };

    private final ResourceHandler<ItemResource> itemTransferHandler = LegacyItemHandlerResourceHandler.wrap(moduleHandler);

    public MobReaperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOB_REAPER_BE.get(), pos, state);
    }

    private boolean isValidDamageModule(ItemStack stack) {
        if (stack.is(ModItems.NORMAL_DAMAGE_MODULE.get()) || stack.is(ModItems.LETHAL_DAMAGE_MODULE.get())) {
            ItemStack existing = moduleHandler.getStackInSlot(0);
            if (existing.isEmpty()) {
                return true;
            }
            if (existing.is(ModItems.LETHAL_DAMAGE_MODULE.get())) {
                return stack.is(ModItems.LETHAL_DAMAGE_MODULE.get());
            }
            if (existing.is(ModItems.NORMAL_DAMAGE_MODULE.get())) {
                return stack.is(ModItems.NORMAL_DAMAGE_MODULE.get());
            }
        }
        return false;
    }

    public ItemStackHandler getModuleHandler() {
        return moduleHandler;
    }

    public ResourceHandler<ItemResource> getItemTransferHandler() {
        return itemTransferHandler;
    }

    public void ensureOwner(UUID uuid) {
        if (ownerUuid == null && uuid != null) {
            ownerUuid = uuid;
            setChanged();
        }
    }

    public void setMountedOnPlate(boolean mountedOnPlate) {
        if (this.mountedOnPlate != mountedOnPlate) {
            this.mountedOnPlate = mountedOnPlate;
            setChanged();
        }
    }

    public boolean isMountedOnPlate() {
        return mountedOnPlate;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.iska_utils.mob_reaper");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        ensureOwner(player.getUUID());
        return new net.unfamily.iskautils.client.gui.MobReaperMenu(containerId, playerInventory, this);
    }

    public MachineTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(MachineTargetType value) {
        this.targetType = value != null ? value : MachineTargetType.MOBS_ONLY;
        setChanged();
    }

    public int getRedstoneMode() {
        return Math.max(0, Math.min(redstoneMode, 4));
    }

    public void setRedstoneMode(int value) {
        if (value == 3) {
            value = 4;
        }
        int newMode = Math.max(0, Math.min(value, 4));
        if (this.redstoneMode != newMode) {
            this.redstoneMode = newMode;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    public void cycleRedstoneMode() {
        int nextMode = (this.redstoneMode + 1) % 5;
        if (nextMode == 3) {
            nextMode = 4;
        }
        setRedstoneMode(nextMode);
    }

    public void cycleRedstoneModeBackward() {
        int m = getRedstoneMode();
        int prev = switch (m) {
            case 0 -> 4;
            case 1 -> 0;
            case 2 -> 1;
            case 4 -> 2;
            default -> m;
        };
        setRedstoneMode(prev);
    }

    public void cycleTargetType() {
        setTargetType(targetType.cycle(false));
    }

    public void cycleTargetTypeBackward() {
        setTargetType(targetType.cycle(true));
    }

    public MobAgeFilter getAgeFilter() {
        return ageFilter;
    }

    public void setAgeFilter(MobAgeFilter value) {
        this.ageFilter = value != null ? value : MobAgeFilter.BOTH;
        setChanged();
    }

    public void cycleAgeFilter() {
        setAgeFilter(ageFilter.cycle(false));
    }

    public void cycleAgeFilterBackward() {
        setAgeFilter(ageFilter.cycle(true));
    }

    public boolean isLethalActive() {
        return moduleHandler.getStackInSlot(0).is(ModItems.LETHAL_DAMAGE_MODULE.get());
    }

    public int getNormalModuleCount() {
        ItemStack stack = moduleHandler.getStackInSlot(0);
        if (stack.is(ModItems.NORMAL_DAMAGE_MODULE.get())) {
            return stack.getCount();
        }
        return 0;
    }

    public double getEffectiveDamage() {
        if (isLethalActive()) {
            return Config.reaperLethalDamage;
        }
        return Config.reaperDefaultDamage + getNormalModuleCount() * Config.reaperNormalBonusPerModule;
    }

    public float getBeheadingChance() {
        ItemStack stack = moduleHandler.getStackInSlot(2);
        if (stack.is(ModItems.BEHEADING_MODULE.get())) {
            return stack.getCount() * (float) Config.reaperBeheadingChancePerLevel;
        }
        return 0.0f;
    }

    public int getEffectiveLuckLevel() {
        ItemStack stack = moduleHandler.getStackInSlot(3);
        if (stack.is(ModItems.LUCK_MODULE.get())) {
            return stack.getCount();
        }
        return 0;
    }

    public float getExperienceMultiplier() {
        ItemStack stack = moduleHandler.getStackInSlot(4);
        if (stack.is(ModItems.EXPERIENCE_MODULE.get())) {
            return 1.0f + stack.getCount() * (float) Config.reaperExperienceBonusPerLevel;
        }
        return 1.0f;
    }

    public static Direction getExcludedContactDirection(BlockState state) {
        if (state.getValue(MobReaperBlock.VERTICAL)) {
            return state.getValue(MobReaperBlock.FACING).getOpposite();
        }
        return Direction.DOWN;
    }

    public static AABB calculateContactHitArea(BlockPos pos, Direction direction) {
        return calculateHitArea(pos, direction, 1);
    }

    public static AABB calculateHitArea(BlockPos pos, Direction direction, int step) {
        return new AABB(pos.relative(direction, step));
    }

    private static void collectTargets(ServerLevel level, BlockPos pos, BlockState state, MachineTargetType targetType,
                                       MobAgeFilter ageFilter, Set<LivingEntity> targets) {
        Direction facing = state.getValue(MobReaperBlock.FACING);
        Direction excluded = getExcludedContactDirection(state);
        int hitDepth = Math.max(1, Config.reaperHitDepth);

        for (Direction direction : Direction.values()) {
            if (direction == excluded) {
                continue;
            }
            int depth = direction == facing ? hitDepth : 1;
            for (int step = 1; step <= depth; step++) {
                AABB hitArea = calculateHitArea(pos, direction, step);
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, hitArea,
                        e -> e.isAlive() && !e.isSpectator() && e.getBoundingBox().intersects(hitArea)
                                && shouldTargetEntity(e, targetType, ageFilter))) {
                    targets.add(entity);
                }
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        targetType = MachineTargetType.fromId(input.getIntOr("TargetType", MachineTargetType.MOBS_ONLY.getId()));
        ageFilter = MobAgeFilter.fromId(input.getIntOr("AgeFilter", MobAgeFilter.BOTH.getId()));
        redstoneMode = input.getIntOr("RedstoneMode", 0);
        if (redstoneMode == 3) {
            redstoneMode = 4;
        }
        attackCooldown = input.getIntOr("AttackCooldown", 0);
        mountedOnPlate = input.getBooleanOr("MountedOnPlate", false);
        ownerUuid = input.read("OwnerUuid", net.minecraft.core.UUIDUtil.CODEC).orElse(null);

        for (ItemStackWithSlot item : input.listOrEmpty("Modules", ItemStackWithSlot.CODEC)) {
            int slot = item.slot();
            if (slot >= 0 && slot < moduleHandler.getSlots()) {
                moduleHandler.setStackInSlot(slot, item.stack());
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("TargetType", targetType.getId());
        output.putInt("AgeFilter", ageFilter.getId());
        output.putInt("RedstoneMode", redstoneMode);
        output.putInt("AttackCooldown", attackCooldown);
        output.putBoolean("MountedOnPlate", mountedOnPlate);
        output.storeNullable("OwnerUuid", net.minecraft.core.UUIDUtil.CODEC, ownerUuid);

        ValueOutput.TypedOutputList<ItemStackWithSlot> modules = output.list("Modules", ItemStackWithSlot.CODEC);
        for (int slot = 0; slot < moduleHandler.getSlots(); slot++) {
            ItemStack stack = moduleHandler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                modules.add(new ItemStackWithSlot(slot, stack));
            }
        }
        if (modules.isEmpty()) {
            output.discard("Modules");
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MobReaperBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        if (blockEntity.redstoneMode == 3) {
            blockEntity.redstoneMode = 4;
        }

        int currentRedstoneMode = Math.max(0, Math.min(blockEntity.redstoneMode, 4));
        int redstonePower = level.getBestNeighborSignal(pos);
        boolean hasRedstoneSignal = redstonePower > 0;
        boolean shouldAttack;
        switch (currentRedstoneMode) {
            case 1 -> shouldAttack = !hasRedstoneSignal;
            case 2 -> shouldAttack = hasRedstoneSignal;
            case 4 -> shouldAttack = false;
            default -> shouldAttack = true;
        }

        if (state.getValue(MobReaperBlock.POWERED) != shouldAttack) {
            level.setBlock(pos, state.setValue(MobReaperBlock.POWERED, shouldAttack), 3);
        }

        if (!shouldAttack) {
            return;
        }

        if (blockEntity.attackCooldown > 0) {
            blockEntity.attackCooldown--;
            return;
        }

        blockEntity.attackCooldown = Config.reaperAttackIntervalTicks;

        ServerLevel serverLevel = (ServerLevel) level;
        Set<LivingEntity> targets = new HashSet<>();
        collectTargets(serverLevel, pos, state, blockEntity.targetType, blockEntity.ageFilter, targets);

        for (LivingEntity entity : targets) {
            blockEntity.attackEntity(serverLevel, entity);
        }
    }

    private static boolean shouldTargetEntity(LivingEntity entity, MachineTargetType targetType, MobAgeFilter ageFilter) {
        boolean isPlayer = entity instanceof Player;
        if (targetType == MachineTargetType.MOBS_AND_PLAYERS) {
            // fall through to age filter
        } else if (targetType == MachineTargetType.PLAYERS_ONLY) {
            if (!isPlayer) {
                return false;
            }
        } else if (isPlayer) {
            return false;
        }

        if (ageFilter == MobAgeFilter.BOTH) {
            return true;
        }
        boolean isBaby = entity.isBaby();
        if (ageFilter == MobAgeFilter.ADULTS) {
            return !isBaby;
        }
        return isBaby;
    }

    private void attackEntity(ServerLevel level, LivingEntity target) {
        FakePlayer fakePlayer = getCombatFakePlayer(level);
        ItemStack weapon = getEnchantWeaponStack();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        applyLuckEffect(fakePlayer);

        float damage = (float) getEffectiveDamage();
        if (damage <= 0.0F) {
            return;
        }

        DamageSource damageSource = level.damageSources().source(ModDamageTypes.MOB_REAPER, fakePlayer);
        if (!weapon.isEmpty() && EnchantmentHelper.hasAnyEnchantments(weapon)) {
            damage = EnchantmentHelper.modifyDamage(level, weapon, target, damageSource, damage);
        }
        if (damage <= 0.0F) {
            return;
        }

        boolean wasAlive = target.isAlive();
        target.hurtServer(level, damageSource, damage);
        fakePlayer.setLastHurtMob(target);
        if (!weapon.isEmpty()) {
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, damageSource, weapon);
        }

        if (wasAlive && !target.isAlive()) {
            handleKill(level, target, fakePlayer);
        }
    }

    private ItemStack getEnchantWeaponStack() {
        ItemStack stack = moduleHandler.getStackInSlot(1);
        if (stack.is(ModItems.ENCHANT_MODULE.get())) {
            return stack.copy();
        }
        return ItemStack.EMPTY;
    }

    private FakePlayer getCombatFakePlayer(ServerLevel level) {
        UUID ownerId = ownerUuid != null ? ownerUuid : new UUID(0L, 0L);
        if (combatFakePlayer == null || combatFakePlayer.level() != level) {
            GameProfile profile = new GameProfile(
                    UUID.nameUUIDFromBytes(("iska_mob_reaper:" + ownerId).getBytes(StandardCharsets.UTF_8)),
                    "[MobReaper]"
            );
            combatFakePlayer = FakePlayerFactory.get(level, profile);
        }
        combatFakePlayer.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        return combatFakePlayer;
    }

    private void applyLuckEffect(FakePlayer fakePlayer) {
        int luckLevel = getEffectiveLuckLevel();
        if (luckLevel > 0) {
            fakePlayer.addEffect(new MobEffectInstance(MobEffects.LUCK, 200, luckLevel - 1, false, false, true));
        }
    }

    private void handleKill(ServerLevel level, LivingEntity target, FakePlayer fakePlayer) {
        float beheadingChance = getBeheadingChance();
        if (beheadingChance > 0.0f && level.getRandom().nextFloat() < beheadingChance) {
            ItemStack skull = getSkullDrop(target);
            if (!skull.isEmpty()) {
                Containers.dropItemStack(level, target.getX(), target.getY(), target.getZ(), skull);
            }
        }

        float xpMultiplier = getExperienceMultiplier();
        if (xpMultiplier > 1.0f) {
            int xp = target.getExperienceReward(level, fakePlayer);
            int bonus = Math.round(xp * (xpMultiplier - 1.0f));
            if (bonus > 0) {
                ExperienceOrb.award(level, target.position(), bonus);
            }
        }
    }

    private static ItemStack getSkullDrop(LivingEntity entity) {
        if (entity.getType() == EntityType.ZOMBIE
                || entity.getType() == EntityType.ZOMBIE_VILLAGER
                || entity.getType() == EntityType.HUSK
                || entity.getType() == EntityType.DROWNED) {
            return new ItemStack(Items.ZOMBIE_HEAD);
        }
        if (entity.getType() == EntityType.SKELETON) {
            return new ItemStack(Items.SKELETON_SKULL);
        }
        if (entity.getType() == EntityType.WITHER_SKELETON) {
            return new ItemStack(Items.WITHER_SKELETON_SKULL);
        }
        if (entity.getType() == EntityType.CREEPER) {
            return new ItemStack(Items.CREEPER_HEAD);
        }
        if (entity.getType() == EntityType.PIGLIN
                || entity.getType() == EntityType.PIGLIN_BRUTE
                || entity.getType() == EntityType.ZOMBIFIED_PIGLIN) {
            return new ItemStack(Items.PIGLIN_HEAD);
        }
        return ItemStack.EMPTY;
    }
}
