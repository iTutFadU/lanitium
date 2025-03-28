package me.itut.lanitium.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.itut.lanitium.Lanitium;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerLinks;

import java.io.*;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(Component.class, new ComponentTypeAdapter())
        .registerTypeAdapter(ServerLinks.Entry.class, new ServerLinksEntryTypeAdapter())
        .create();
    protected Config config;
    protected final File configFile;

    public ConfigManager(File configFile) {
        this.configFile = configFile;
    }

    public Config config() {
        return config;
    }

    public Config load() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile)));
            config = GSON.fromJson(reader, Config.class);
            config.fillDefaults();
        } catch (IOException e) {
            config = new Config();
            config.fillDefaults();
            save();
        }
        return config;
    }

    public void save() {
        String jsonConfig = GSON.toJson(config);
        try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(jsonConfig);
            writer.flush();
        } catch (IOException e) {
            Lanitium.LOGGER.error("Could not save config", e);
        }
    }
}