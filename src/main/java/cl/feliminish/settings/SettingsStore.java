package cl.feliminish.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SettingsStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".retrotrax", "settings.json");
    private static TraxSettings current = new TraxSettings();

    private SettingsStore() {}

    public static TraxSettings get() { return current; }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                TraxSettings s = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), TraxSettings.class);
                if (s != null) current = s;
            }
        } catch (Exception e) {
            System.err.println("No se pudo leer la configuración, uso valores por defecto: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(current), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo guardar la configuración: " + e.getMessage());
        }
    }
}
