package com.junggyeol.mininggacha;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 블록이 부서질 때(채굴)를 감지하되 자동 포인트 전환은 수행하지 않습니다.
 * 플레이어는 /mg convertPoints 또는 GUI를 통해 인벤토리 아이템을 전환해야 합니다.
 */
public class OreBreakHandler {

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() == null) return;
        Block block = event.getState().getBlock();
        String blockId = block.getRegistryName() != null ? block.getRegistryName().toString() : block.toString();

        // 자동 포인트 전환 제거 - 로그만 남김
        if (block == Blocks.IRON_ORE || block == Blocks.GOLD_ORE || block == Blocks.DIAMOND_ORE) {
            MiningGachaMod.LOGGER.debug("Player {} broke block {} (no auto-convert).", event.getPlayer().getName().getString(), blockId);
        }
    }
}