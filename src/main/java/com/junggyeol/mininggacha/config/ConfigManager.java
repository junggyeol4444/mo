package com.junggyeol.mininggacha.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 단일 JSON 파일 기반 설정 관리자 (config/mininggacha_config.json)
 */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;

    public double quitChance = 0.001; // 기본 0.1%
    public int gachaCost = 50;
    public Map<String, Integer> orePoints = new HashMap<>();

    public ConfigManager(Path path) {
        this.path = path;
        // 기본 오레 값 (아이템 레지스트리 이름)
        orePoints.put("minecraft:iron_ore", 5);
        orePoints.put("minecraft:gold_ore", 10);
        orePoints.put("minecraft:diamond_ore", 25);
        load();
    }

    public synchronized void load() {
        try {
            if (Files.exists(path)) {
                String json = Files.readString(path);
                ConfigManager loaded = GSON.fromJson(json, ConfigManager.class);
                if (loaded != null) {
                    this.quitChance = loaded.quitChance;
                    this.gachaCost = loaded.gachaCost;
                    if (loaded.orePoints != null && !loaded.orePoints.isEmpty()) this.orePoints = loaded.orePoints;
                }
            } else {
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void save() {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            String json = GSON.toJson(this);
            Files.writeString(path, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}