package com.junggyeol.mininggacha.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import com.junggyeol.mininggacha.MiningGachaMod;

import java.util.Optional;

/**
 * SimpleChannel 등록 및 패킷 id 관리
 */
public class NetworkHandler {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MiningGachaMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int id = 0;

    public static void register() {
        // 서버 -> 클라이언트: 플레이어 데이터 동기화
        CHANNEL.registerMessage(id++, SyncPlayerDataPacket.class,
                SyncPlayerDataPacket::encode,
                SyncPlayerDataPacket::decode,
                SyncPlayerDataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // 서버 -> 클라이언트: GUI 열기 요청
        CHANNEL.registerMessage(id++, OpenConvertGuiPacket.class,
                OpenConvertGuiPacket::encode,
                OpenConvertGuiPacket::decode,
                OpenConvertGuiPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // 클라이언트 -> 서버: 변환 요청 (슬롯, 수량)
        CHANNEL.registerMessage(id++, ConvertItemsPacket.class,
                ConvertItemsPacket::encode,
                ConvertItemsPacket::decode,
                ConvertItemsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}