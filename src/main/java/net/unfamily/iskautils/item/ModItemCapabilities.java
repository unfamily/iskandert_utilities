package net.unfamily.iskautils.item;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.iskautils.IskaUtils;
import net.unfamily.iskautils.item.custom.VectorCharmItem;
import net.unfamily.iskautils.item.custom.PortableDislocatorItem;
import net.unfamily.iskautils.item.custom.ElectricTreeTapItem;

@EventBusSubscriber(modid = IskaUtils.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModItemCapabilities {
    
    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // Register energy capability for Vector Charm
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
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
        
        // Register energy capability for Portable Dislocator
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
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
                Capabilities.EnergyStorage.ITEM,
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
    }
    
    /**
     * Energy storage implementation for Vector Charm
     */
    public static class VectorCharmEnergyStorage implements IEnergyStorage {
        private final VectorCharmItem vectorCharm;
        private final net.minecraft.world.item.ItemStack stack;
        
        public VectorCharmEnergyStorage(VectorCharmItem vectorCharm, net.minecraft.world.item.ItemStack stack) {
            this.vectorCharm = vectorCharm;
            this.stack = stack;
        }
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int currentEnergy = vectorCharm.getEnergyStored(stack);
            int maxEnergy = vectorCharm.getMaxEnergyStored(stack);
            int energyToReceive = Math.min(maxReceive, maxEnergy - currentEnergy);
            
            if (!simulate && energyToReceive > 0) {
                vectorCharm.setEnergyStored(stack, currentEnergy + energyToReceive);
            }
            
            return energyToReceive;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int currentEnergy = vectorCharm.getEnergyStored(stack);
            int energyToExtract = Math.min(maxExtract, currentEnergy);
            
            if (!simulate && energyToExtract > 0) {
                vectorCharm.setEnergyStored(stack, currentEnergy - energyToExtract);
            }
            
            return energyToExtract;
        }
        
        @Override
        public int getEnergyStored() {
            return vectorCharm.getEnergyStored(stack);
        }
        
        @Override
        public int getMaxEnergyStored() {
            return vectorCharm.getMaxEnergyStored(stack);
        }
        
        @Override
        public boolean canExtract() {
            return false; // Vector Charm doesn't allow energy extraction
        }
        
        @Override
        public boolean canReceive() {
            return vectorCharm.canStoreEnergy();
        }
    }
    
    /**
     * Energy storage implementation for Portable Dislocator
     */
    public static class PortableDislocatorEnergyStorage implements IEnergyStorage {
        private final PortableDislocatorItem dislocator;
        private final net.minecraft.world.item.ItemStack stack;
        
        public PortableDislocatorEnergyStorage(PortableDislocatorItem dislocator, net.minecraft.world.item.ItemStack stack) {
            this.dislocator = dislocator;
            this.stack = stack;
        }
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int currentEnergy = dislocator.getEnergyStored(stack);
            int maxEnergy = dislocator.getMaxEnergyStored(stack);
            int energyToReceive = Math.min(maxReceive, maxEnergy - currentEnergy);
            
            if (!simulate && energyToReceive > 0) {
                dislocator.setEnergyStored(stack, currentEnergy + energyToReceive);
            }
            
            return energyToReceive;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int currentEnergy = dislocator.getEnergyStored(stack);
            int energyToExtract = Math.min(maxExtract, currentEnergy);
            
            if (!simulate && energyToExtract > 0) {
                dislocator.setEnergyStored(stack, currentEnergy - energyToExtract);
            }
            
            return energyToExtract;
        }
        
        @Override
        public int getEnergyStored() {
            return dislocator.getEnergyStored(stack);
        }
        
        @Override
        public int getMaxEnergyStored() {
            return dislocator.getMaxEnergyStored(stack);
        }
        
        @Override
        public boolean canExtract() {
            return false; // Portable Dislocator doesn't allow energy extraction
        }
        
        @Override
        public boolean canReceive() {
            return dislocator.canStoreEnergy();
        }
    }
    
    /**
     * Energy storage implementation for Electric TreeTap
     */
    public static class ElectricTreeTapEnergyStorage implements IEnergyStorage {
        private final ElectricTreeTapItem treeTap;
        private final net.minecraft.world.item.ItemStack stack;
        
        public ElectricTreeTapEnergyStorage(ElectricTreeTapItem treeTap, net.minecraft.world.item.ItemStack stack) {
            this.treeTap = treeTap;
            this.stack = stack;
        }
        
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int currentEnergy = treeTap.getEnergyStored(stack);
            int maxEnergy = treeTap.getMaxEnergyStored(stack);
            int energyToReceive = Math.min(maxReceive, maxEnergy - currentEnergy);
            
            if (!simulate && energyToReceive > 0) {
                treeTap.setEnergyStored(stack, currentEnergy + energyToReceive);
            }
            
            return energyToReceive;
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int currentEnergy = treeTap.getEnergyStored(stack);
            int energyToExtract = Math.min(maxExtract, currentEnergy);
            
            if (!simulate && energyToExtract > 0) {
                treeTap.setEnergyStored(stack, currentEnergy - energyToExtract);
            }
            
            return energyToExtract;
        }
        
        @Override
        public int getEnergyStored() {
            return treeTap.getEnergyStored(stack);
        }
        
        @Override
        public int getMaxEnergyStored() {
            return treeTap.getMaxEnergyStored(stack);
        }
        
        @Override
        public boolean canExtract() {
            return false; // Electric TreeTap doesn't allow energy extraction
        }
        
        @Override
        public boolean canReceive() {
            return treeTap.canStoreEnergy();
        }
    }
} 