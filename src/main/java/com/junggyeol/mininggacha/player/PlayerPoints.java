package com.junggyeol.mininggacha.player;

import net.minecraft.nbt.CompoundTag;

/**
 * 플레이어 포인트 및 퇴근 허용 상태
 */
public class PlayerPoints {
    private int points = 0;
    private boolean allowedToQuit = false;

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = Math.max(0, points); }
    public void addPoints(int v) { this.points = Math.max(0, this.points + v); }
    public boolean consumePoints(int cost) {
        if (points >= cost) { points -= cost; return true; }
        return false;
    }

    public boolean isAllowedToQuit() { return allowedToQuit; }
    public void setAllowedToQuit(boolean allowed) { this.allowedToQuit = allowed; }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("points", points);
        tag.putBoolean("allowedToQuit", allowedToQuit);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag == null) return;
        if (tag.contains("points")) points = tag.getInt("points");
        if (tag.contains("allowedToQuit")) allowedToQuit = tag.getBoolean("allowedToQuit");
    }
}