package com.junggyeol.mininggacha.client;

import com.junggyeol.mininggacha.MiningGachaMod;
import com.junggyeol.mininggacha.network.ConvertItemsPacket;
import com.junggyeol.mininggacha.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 간단한 전환 GUI.
 * - 서버에 등록된 orePoints에 따라 인벤토리에서 변환 가능한 아이템들을 표시
 * - 각 항목은 클릭할 때마다 선택 수량을 순환 (0 -> full -> half -> 1 -> 0)
 * - Convert 버튼은 선택된 항목들을 서버로 전송
 */
public class ConvertScreen extends Screen {
    private final Minecraft mc = Minecraft.getInstance();
    private final Map<Integer, Integer> selection = new HashMap<>(); // slot -> selectedCount
    private final List<Integer> convertibleSlots = new ArrayList<>();
    private final Map<Integer, Button> slotButtons = new HashMap<>();

    protected ConvertScreen() {
        super(Component.literal("Convert Items to Points"));
    }

    @Override
    protected void init() {
        selection.clear();
        convertibleSlots.clear();
        slotButtons.clear();

        if (mc.player == null) return;
        var inv = mc.player.getInventory();
        Font font = mc.font;

        // 버튼 배치
        int startX = (this.width / 2) - 120;
        int startY = (this.height / 2) - 100;
        int y = startY;
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            String itemId = stack.getItem().getRegistryName() != null ? stack.getItem().getRegistryName().toString() : stack.getItem().toString();
            Integer perPoint = MiningGachaMod.CONFIG.orePoints.get(itemId);
            if (perPoint == null || perPoint <= 0) continue;

            convertibleSlots.add(slot);
            int displaySlot = slot;
            String label = makeLabel(displaySlot, stack.getHoverName().getString(), stack.getCount(), 0);
            Button b = Button.builder(Component.literal(label), btn -> {
                cycleSelection(displaySlot, btn);
            }).bounds(startX, y, 240, 20).build();
            this.addRenderableWidget(b);
            slotButtons.put(slot, b);
            y += 22;
            // 화면 높이 여유 확인(간단)
            if (y > this.height - 60) break;
        }

        // Convert 버튼
        this.addRenderableWidget(Button.builder(Component.literal("Convert Selected"), btn -> {
            sendConvertRequest();
            this.onClose();
        }).bounds(this.width / 2 - 100, this.height - 45, 100, 20).build());

        // Cancel 버튼
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            this.onClose();
        }).bounds(this.width / 2 + 5, this.height - 45, 100, 20).build());
    }

    private String makeLabel(int slot, String name, int total, int selected) {
        return String.format("Slot %d: %s (%d) -> %d", slot, name, total, selected);
    }

    private void cycleSelection(int slot, Button btn) {
        ItemStack stack = mc.player.getInventory().getItem(slot);
        if (stack.isEmpty()) return;
        int total = stack.getCount();
        int cur = selection.getOrDefault(slot, 0);
        // cycle: 0 -> full -> half -> 1 -> 0
        int next;
        if (cur == 0) next = total;
        else if (cur == total) next = Math.max(1, total / 2);
        else if (cur == Math.max(1, total / 2)) next = 1;
        else next = 0;
        if (next <= 0) selection.remove(slot);
        else selection.put(slot, next);

        String label = makeLabel(slot, stack.getHoverName().getString(), total, next);
        btn.setMessage(Component.literal(label));
    }

    private void sendConvertRequest() {
        if (selection.isEmpty()) return;
        List<Integer> slots = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (var e : selection.entrySet()) {
            slots.add(e.getKey());
            counts.add(e.getValue());
        }
        NetworkHandler.CHANNEL.sendToServer(new ConvertItemsPacket(slots, counts));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics graphics) {
        super.renderBackground(graphics);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(mc.font, this.title.getString(), this.width / 2, 20, 0xFFFFFF);
    }
}