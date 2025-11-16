package com.junggyeol.mininggacha.player;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

/**
 * Capability token and provider for PlayerPoints
 */
public class PlayerPointsProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerPoints> PLAYER_POINTS_CAP = CapabilityManager.get(new CapabilityToken<>(){});
    private final PlayerPoints instance = new PlayerPoints();
    private final LazyOptional<PlayerPoints> optional = LazyOptional.of(() -> instance);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == PLAYER_POINTS_CAP) return optional.cast();
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return instance.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.deserializeNBT(nbt);
    }
}