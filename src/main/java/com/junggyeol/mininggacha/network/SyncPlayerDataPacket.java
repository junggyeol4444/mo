package com.junggyeol.mininggacha.network;

import com.junggyeol.mininggacha.player.PlayerPointsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

/**
 * 서버 -> 클라이언트: 플레이어 포인트와 퇴근 허용 상태 동기화
 */
public class SyncPlayerDataPacket {
    private final int points;
    private final boolean allowed;

    public SyncPlayerDataPacket(int points, boolean allowed) {
        this.points = points;
        this.allowed = allowed;
    }

    public static void encode(SyncPlayerDataPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.points);
        buf.writeBoolean(msg.allowed);
    }

    public static SyncPlayerDataPacket decode(FriendlyByteBuf buf) {
        return new SyncPlayerDataPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(SyncPlayerDataPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // 클라이언트 스레드에서 capability에 적용
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerPointsProvider.PLAYER_POINTS_CAP).ifPresent(pp -> {
                    pp.setPoints(msg.points);
                    pp.setAllowedToQuit(msg.allowed);
                });
            }
        });
        ctx.setPacketHandled(true);
    }
}