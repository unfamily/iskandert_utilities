package net.unfamily.iskautils.item;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.custom.VectorCharmItem;
import net.unfamily.iskautils.item.custom.FanpackItem;
import net.unfamily.iskautils.item.custom.PortableDislocatorItem;
import net.unfamily.iskautils.item.custom.ElectricTreeTapItem;
import net.unfamily.iskautils.item.custom.ScannerItem;

public class ModItemCapabilities {
    
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Register energy capability for Vector Charm
        event.registerItem(
                Capabilities.Energy.ITEM,
                (stack, context) -> {
                    if (stack.getItem() instanceof VectorCharmItem vectorCharm) {
                        if (vectorCharm.canStoreEnergy()) {
                            return new VectorCharmEnergyStorage(vectorCharm, stack);
                        }
                    }
                    return null;
                },
                ModItems.VECTOR_CHARM.get()
        );
        
        // Register energy capability for Fanpack
        event.registerItem(
                Capabilities.Energy.ITEM,
                (stack, context) -> {
                    if (stack.getItem() instanceof FanpackItem fanpack) {
                        if (fanpack.canStoreEnergy()) {
                            return new FanpackEnergyStorage(fanpack, stack);
                        }
                    }
                    return null;
                },
                ModItems.FANPACK.get()
        );
        
        // Register energy capability for Portable Dislocator
        event.registerItem(
                Capabilities.Energy.ITEM,
                (stack, context) -> {
                    if (stack.getItem() instanceof PortableDislocatorItem dislocator) {
                        if (dislocator.canStoreEnergy()) {
                            return new PortableDislocatorEnergyStorage(dislocator, stack);
                        }
                    }
                    return null;
                },
                ModItems.PORTABLE_DISLOCATOR.get()
        );
        
        // Register energy capability for Electric TreeTap
        event.registerItem(
                Capabilities.Energy.ITEM,
                (stack, context) -> {
                    if (stack.getItem() instanceof ElectricTreeTapItem treeTap) {
                        if (treeTap.canStoreEnergy()) {
                            return new ElectricTreeTapEnergyStorage(treeTap, stack);
                        }
                    }
                    return null;
                },
                ModItems.ELECTRIC_TREE_TAP.get()
        );
        
        // Register energy capability for Scanner
        event.registerItem(
                Capabilities.Energy.ITEM,
                (stack, context) -> {
                    if (stack.getItem() instanceof ScannerItem scanner) {
                        if (scanner.canStoreEnergy()) {
                            return new ScannerEnergyStorage(scanner, stack);
                        }
                    }
                    return null;
                },
                ModItems.SCANNER.get()
        );
    }
    
    /**
     * Energy storage implementation for Vector Charm
     */
    public static class VectorCharmEnergyStorage extends SnapshotJournal<Integer> implements EnergyHandler {
        private final VectorCharmItem vectorCharm;
        private final net.minecraft.world.item.ItemStack stack;
        
        public VectorCharmEnergyStorage(VectorCharmItem vectorCharm, net.minecraft.world.item.ItemStack stack) {
            this.vectorCharm = vectorCharm;
            this.stack = stack;
        }
        
        @Override
        protected Integer createSnapshot() {
            return vectorCharm.getEnergyStored(stack);
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            vectorCharm.setEnergyStored(stack, snapshot);
        }

        @Override
        public long getAmountAsLong() {
            return vectorCharm.getEnergyStored(stack);
        }

        @Override
        public long getCapacityAsLong() {
            return vectorCharm.getMaxEnergyStored(stack);
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            updateSnapshots(transaction);

            int current = vectorCharm.getEnergyStored(stack);
            int max = vectorCharm.getMaxEnergyStored(stack);
            int inserted = Math.min(amount, Math.max(0, max - current));
            if (inserted > 0) {
                vectorCharm.setEnergyStored(stack, current + inserted);
            }
            return inserted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            // Keep legacy behavior: these items don't expose extraction.
            return 0;
        }
    }
    
    /**
     * Energy storage implementation for Fanpack
     */
    public static class FanpackEnergyStorage extends SnapshotJournal<Integer> implements EnergyHandler {
        private final FanpackItem fanpack;
        private final net.minecraft.world.item.ItemStack stack;
        
        public FanpackEnergyStorage(FanpackItem fanpack, net.minecraft.world.item.ItemStack stack) {
            this.fanpack = fanpack;
            this.stack = stack;
        }
        
        @Override
        protected Integer createSnapshot() {
            return fanpack.getEnergyStored(stack);
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            fanpack.setEnergyStored(stack, snapshot);
        }

        @Override
        public long getAmountAsLong() {
            return fanpack.getEnergyStored(stack);
        }

        @Override
        public long getCapacityAsLong() {
            return fanpack.getMaxEnergyStored(stack);
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            updateSnapshots(transaction);

            int current = fanpack.getEnergyStored(stack);
            int max = fanpack.getMaxEnergyStored(stack);
            int inserted = Math.min(amount, Math.max(0, max - current));
            if (inserted > 0) {
                fanpack.setEnergyStored(stack, current + inserted);
            }
            return inserted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    }
    
    /**
     * Energy storage implementation for Portable Dislocator
     */
    public static class PortableDislocatorEnergyStorage extends SnapshotJournal<Integer> implements EnergyHandler {
        private final PortableDislocatorItem dislocator;
        private final net.minecraft.world.item.ItemStack stack;
        
        public PortableDislocatorEnergyStorage(PortableDislocatorItem dislocator, net.minecraft.world.item.ItemStack stack) {
            this.dislocator = dislocator;
            this.stack = stack;
        }
        
        @Override
        protected Integer createSnapshot() {
            return dislocator.getEnergyStored(stack);
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            dislocator.setEnergyStored(stack, snapshot);
        }

        @Override
        public long getAmountAsLong() {
            return dislocator.getEnergyStored(stack);
        }

        @Override
        public long getCapacityAsLong() {
            return dislocator.getMaxEnergyStored(stack);
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            updateSnapshots(transaction);

            int current = dislocator.getEnergyStored(stack);
            int max = dislocator.getMaxEnergyStored(stack);
            int inserted = Math.min(amount, Math.max(0, max - current));
            if (inserted > 0) {
                dislocator.setEnergyStored(stack, current + inserted);
            }
            return inserted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    }
    
    /**
     * Energy storage implementation for Electric TreeTap
     */
    public static class ElectricTreeTapEnergyStorage extends SnapshotJournal<Integer> implements EnergyHandler {
        private final ElectricTreeTapItem treeTap;
        private final net.minecraft.world.item.ItemStack stack;
        
        public ElectricTreeTapEnergyStorage(ElectricTreeTapItem treeTap, net.minecraft.world.item.ItemStack stack) {
            this.treeTap = treeTap;
            this.stack = stack;
        }
        
        @Override
        protected Integer createSnapshot() {
            return treeTap.getEnergyStored(stack);
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            treeTap.setEnergyStored(stack, snapshot);
        }

        @Override
        public long getAmountAsLong() {
            return treeTap.getEnergyStored(stack);
        }

        @Override
        public long getCapacityAsLong() {
            return treeTap.getMaxEnergyStored(stack);
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            updateSnapshots(transaction);

            int current = treeTap.getEnergyStored(stack);
            int max = treeTap.getMaxEnergyStored(stack);
            int inserted = Math.min(amount, Math.max(0, max - current));
            if (inserted > 0) {
                treeTap.setEnergyStored(stack, current + inserted);
            }
            return inserted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    }
    
    /**
     * Energy storage implementation for Scanner
     */
    public static class ScannerEnergyStorage extends SnapshotJournal<Integer> implements EnergyHandler {
        private final ScannerItem scanner;
        private final net.minecraft.world.item.ItemStack stack;
        
        public ScannerEnergyStorage(ScannerItem scanner, net.minecraft.world.item.ItemStack stack) {
            this.scanner = scanner;
            this.stack = stack;
        }
        
        @Override
        protected Integer createSnapshot() {
            return scanner.getEnergyStored(stack);
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            scanner.setEnergyStored(stack, snapshot);
        }

        @Override
        public long getAmountAsLong() {
            return scanner.getEnergyStored(stack);
        }

        @Override
        public long getCapacityAsLong() {
            return scanner.getMaxEnergyStored(stack);
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(amount);
            updateSnapshots(transaction);

            int current = scanner.getEnergyStored(stack);
            int max = scanner.getMaxEnergyStored(stack);
            int inserted = Math.min(amount, Math.max(0, max - current));
            if (inserted > 0) {
                scanner.setEnergyStored(stack, current + inserted);
            }
            return inserted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            return 0;
        }
    }
} 