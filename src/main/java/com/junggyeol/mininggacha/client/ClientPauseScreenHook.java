package com.junggyeol.mininggacha.client;

import com.junggyeol.mininggacha.player.PlayerPointsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * PauseScreen에서 Save & Quit 버튼을 비활성화합니다. (클라이언트 레벨)
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "mininggacha", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientPauseScreenHook {
    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof PauseScreen)) return;
        var mc = Minecraft.getInstance();
        boolean allowed = false;
        if (mc.player != null) {
            var cap = mc.player.getCapability(PlayerPointsProvider.PLAYER_POINTS_CAP);
            if (cap.isPresent()) {
                allowed = cap.orElseThrow(IllegalStateException::new).isAllowedToQuit();
            }
        }
        for (var widget : screen.children()) {
            if (widget instanceof Button b) {
                String text = b.getMessage() != null ? b.getMessage().getString().toLowerCase() : "";
                if (text.contains("save") && text.contains("quit")) {
                    b.active = allowed;
                    if (!allowed) b.setTooltip((p) -> Component.literal("You must get '퇴근' from the gacha to quit."));
                }
            }
        }
    }
}