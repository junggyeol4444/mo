package com.junggyeol.mininggacha.network;

import com.junggyeol.mininggacha.MiningGachaMod;
import com.junggyeol.mininggacha.player.PlayerPointsProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 클라이언트 -> 서버: 슬롯 인덱스와 전환 수량 목록 전송
 */
public class ConvertItemsPacket {
    public final List<Integer> slots;
    public final List<Integer> counts;

    public ConvertItemsPacket(List<Integer> slots, List<Integer> counts) {
        this.slots = slots;
        this.counts = counts;
    }

    public static void encode(ConvertItemsPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slots.size());
        for (int i = 0; i < msg.slots.size(); i++) {
            buf.writeInt(msg.slots.get(i));
            buf.writeInt(msg.counts.get(i));
        }
    }

    public static ConvertItemsPacket decode(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        List<Integer> slots = new ArrayList<>(n);
        List<Integer> counts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            slots.add(buf.readInt());
            counts.add(buf.readInt());
        }
        return new ConvertItemsPacket(slots, counts);
    }

    public static void handle(ConvertItemsPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        // 서버 스레드에서 처리
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                ctx.setPacketHandled(true);
                return;
            }

            int totalGained = 0;
            var inv = player.getInventory();

            for (int i = 0; i < msg.slots.size(); i++) {
                int slot = msg.slots.get(i);
                int want = msg.counts.get(i);
                if (want <= 0) continue;
                if (slot < 0 || slot >= inv.getContainerSize()) continue;

                ItemStack stack = inv.getItem(slot);
                if (stack.isEmpty()) continue;

                String itemId = stack.getItem().getRegistryName() != null ? stack.getItem().getRegistryName().toString() : stack.getItem().toString();
                Integer perPoint = MiningGachaMod.CONFIG.orePoints.get(itemId);
                if (perPoint == null || perPoint <= 0) continue;

                int available = stack.getCount();
                int take = Math.min(want, available);
                if (take <= 0) continue;

                // 서버 인벤토리에서 제거 (검증)
                inv.removeItem(slot, take);

                // 포인트 계산 및 추가
                int gained = take * perPoint;
                totalGained += gained;

                player.getCapability(PlayerPointsProvider.PLAYER_POINTS_CAP).ifPresent(pp -> {
                    pp.addPoints(gained);
                });
            }

            // 동기화: capability -> 클라이언트
            player.getCapability(PlayerPointsProvider.PLAYER_POINTS_CAP).ifPresent(pp -> {
                NetworkHandler.CHANNEL.sendToPlayer(new SyncPlayerDataPacket(pp.getPoints(), pp.isAllowedToQuit()), player);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Converted items into " + totalGained + " points. Total points: " + pp.getPoints()));
            });
        });
        ctx.setPacketHandled(true);
    }
}