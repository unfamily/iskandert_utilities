package net.unfamily.iskautils.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.unfamily.iskautils.item.custom.ElectricTreeTapItem;
import net.unfamily.iskautils.item.custom.FanpackItem;
import net.unfamily.iskautils.item.custom.PortableDislocatorItem;
import net.unfamily.iskautils.item.custom.ScannerItem;
import net.unfamily.iskautils.item.custom.VectorCharmItem;

public class ModItemCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        registerItemEnergy(event, ModItems.VECTOR_CHARM.get(), (stack, access) -> {
            if (stack.getItem() instanceof VectorCharmItem item && item.canStoreEnergy()) {
                return new ItemAccessEnergyHandler(item, access);
            }
            return null;
        });

        registerItemEnergy(event, ModItems.FANPACK.get(), (stack, access) -> {
            if (stack.getItem() instanceof FanpackItem item && item.canStoreEnergy()) {
                return new ItemAccessEnergyHandler(item, access);
            }
            return null;
        });

        registerItemEnergy(event, ModItems.PORTABLE_DISLOCATOR.get(), (stack, access) -> {
            if (stack.getItem() instanceof PortableDislocatorItem item && item.canStoreEnergy()) {
                return new ItemAccessEnergyHandler(item, access);
            }
            return null;
        });

        registerItemEnergy(event, ModItems.ELECTRIC_TREE_TAP.get(), (stack, access) -> {
            if (stack.getItem() instanceof ElectricTreeTapItem item && item.canStoreEnergy()) {
                return new ItemAccessEnergyHandler(item, access);
            }
            return null;
        });

        registerItemEnergy(event, ModItems.SCANNER.get(), (stack, access) -> {
            if (stack.getItem() instanceof ScannerItem item && item.canStoreEnergy()) {
                return new ItemAccessEnergyHandler(item, access);
            }
            return null;
        });
    }

    private static void registerItemEnergy(
            RegisterCapabilitiesEvent event,
            Item item,
            net.neoforged.neoforge.capabilities.ICapabilityProvider<net.minecraft.world.item.ItemStack, ItemAccess, EnergyHandler> provider) {
        event.registerItem(Capabilities.Energy.ITEM, provider, item);
    }

    /**
     * Writes RF through {@link ItemAccess} so charging machines update the real inventory stack,
     * not a stale copy from capability lookup.
     */
    public static final class ItemAccessEnergyHandler implements EnergyHandler {
        private final RfStoringItem storage;
        private final ItemAccess itemAccess;
        private final Item validItem;

        public ItemAccessEnergyHandler(RfStoringItem storage, ItemAccess itemAccess) {
            this.storage = storage;
            this.itemAccess = itemAccess;
            this.validItem = itemAccess.getResource().getItem();
        }

        private boolean isActive() {
            return storage.canStoreEnergy() && itemAccess.getAmount() > 0 && itemAccess.getResource().is(validItem);
        }

        private ItemStack liveStack() {
            return itemAccess.getResource().toStack(itemAccess.getAmount());
        }

        private boolean commitStack(ItemStack stack, TransactionContext transaction) {
            int accessAmount = itemAccess.getAmount();
            if (accessAmount <= 0) {
                return false;
            }
            return itemAccess.exchange(ItemResource.of(stack), accessAmount, transaction) == accessAmount;
        }

        @Override
        public long getAmountAsLong() {
            return isActive() ? storage.getEnergyStored(liveStack()) : 0L;
        }

        @Override
        public long getCapacityAsLong() {
            return isActive() ? storage.getMaxEnergyStored(liveStack()) : 0L;
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            if (!isActive() || amount == 0) {
                return 0;
            }

            ItemStack stack = liveStack();
            int current = storage.getEnergyStored(stack);
            int max = storage.getMaxEnergyStored(stack);
            int inserted = Math.min(amount, Math.max(0, max - current));
            if (inserted <= 0) {
                return 0;
            }

            storage.setEnergyStored(stack, current + inserted);
            return commitStack(stack, transaction) ? inserted : 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    }
}
