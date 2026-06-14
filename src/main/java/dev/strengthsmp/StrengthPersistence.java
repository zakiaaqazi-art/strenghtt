package dev.strengthsmp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StrengthPersistence {

    private static final Gson GSON = new Gson();
    private static Path saveFile;

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            saveFile = server.getRunDirectory().resolve("config/strengthsmp_data.json");
            load();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            save();
        });
    }

    public static void save() {
        if (saveFile == null) return;
        try {
            saveFile.getParent().toFile().mkdirs();
            // Convert UUID keys to strings for JSON
            Map<String, Integer> data = new HashMap<>();
            StrengthSMP.getAllStrengths().forEach((uuid, lvl) ->
                data.put(uuid.toString(), lvl));
            try (Writer w = new FileWriter(saveFile.toFile())) {
                GSON.toJson(data, w);
            }
        } catch (IOException e) {
            StrengthSMP.LOGGER.error("Failed to save strength data", e);
        }
    }

    public static void load() {
        if (saveFile == null || !saveFile.toFile().exists()) return;
        try (Reader r = new FileReader(saveFile.toFile())) {
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> data = GSON.fromJson(r, type);
            if (data != null) {
                data.forEach((uuidStr, lvl) ->
                    StrengthSMP.loadStrength(UUID.fromString(uuidStr), lvl));
            }
        } catch (IOException e) {
            StrengthSMP.LOGGER.error("Failed to load strength data", e);
        }
    }
}
