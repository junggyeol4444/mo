package com.junggyeol.mininggacha.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.client.Minecraft;
import com.junggyeol.mininggacha.client.ConvertScreen;

import java.util.function.Supplier;

/**
 * 서버가 클라이언트에게 "전환 GUI 열어라" 라고 지시하기 위한 빈 패킷
 */
public class OpenConvertGuiPacket {
    public OpenConvertGuiPacket() {}

    public static void encode(OpenConvertGuiPacket msg, FriendlyByteBuf buf) {
        // 빈
    }

    public static OpenConvertGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenConvertGuiPacket();
    }

    public static void handle(OpenConvertGuiPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // 클라이언트 스레드에서 화면 열기
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.setScreen(new ConvertScreen());
            }
        });
        ctx.setPacketHandled(true);
    }
}