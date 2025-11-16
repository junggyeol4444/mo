package com.junggyeol.mininggacha;

import com.junggyeol.mininggacha.config.ConfigManager;
import com.junggyeol.mininggacha.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod main class
 */
@Mod(MiningGachaMod.MODID)
public class MiningGachaMod {
    public static final String MODID = "mininggacha";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    // 전역 config 접근용 인스턴스
    public static final ConfigManager CONFIG;

    static {
        // config 파일은 FMLPaths.CONFIGDIR에 저장 (config/mininggacha_config.json)
        CONFIG = new ConfigManager(FMLPaths.CONFIGDIR.get().resolve("mininggacha_config.json"));
    }

    public MiningGachaMod() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new com.junggyeol.mininggacha.PlayerDataEvents());
        MinecraftForge.EVENT_BUS.register(new com.junggyeol.mininggacha.OreBreakHandler());
        NetworkHandler.register();
        LOGGER.info("MiningGacha mod initialized. Config at: {}", FMLPaths.CONFIGDIR.get().resolve("mininggacha_config.json").toAbsolutePath());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        com.junggyeol.mininggacha.GachaCommands.register(event.getDispatcher());
    }
}