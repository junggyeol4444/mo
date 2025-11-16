package com.junggyeol.mininggacha;

import com.junggyeol.mininggacha.network.NetworkHandler;
import com.junggyeol.mininggacha.network.SyncPlayerDataPacket;
import com.junggyeol.mininggacha.player.PlayerPoints;
import com.junggyeol.mininggacha.player.PlayerPointsProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 플레이어 Capability 등록/부착/로그인/클론 처리 및 동기화
 */
@Mod.EventBusSubscriber(modid = MiningGachaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerDataEvents {
    @SubscribeEvent
    public static void onRegisterCaps(RegisterCapabilitiesEvent event) {
        event.register(PlayerPoints.class);
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.player.Player> event) {
        // provider key는 모드 id 사용
        event.addCapability(new net.minecraft.resources.ResourceLocation(MiningGachaMod.MODID, "player_points"), new PlayerPointsProvider());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 복제 시 capability 데이터 복사
        var original = event.getOriginal();
        var clone = event.getEntity();
        original.getCapability(PlayerPointsProvider.PLAYER_POINTS_CAP).ifPresent(orig -> {
            clone.getCapability(PlayerPointsProvider.PLAYER_POINTS_CAP).ifPresent(dest -> {
                dest.deserializeNBT(orig.serializeNBT());
            });
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        // 서버 -> 클라이언트 동기화
        if (event.getPlayer() instanceof ServerPlayer sp) {
            sp.getCapability(PlayerPointsProvider.PLAYER_POINTS_CAP).ifPresent(pp -> {
                NetworkHandler.CHANNEL.sendToPlayer(new SyncPlayerDataPacket(pp.getPoints(), pp.isAllowedToQuit()), sp);
            });
        }
    }
}