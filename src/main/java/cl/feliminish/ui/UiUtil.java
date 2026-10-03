package cl.feliminish.ui;

import cl.feliminish.settings.TraxSettings;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Utilidades de fuentes y colores compartidas por el popup y la ventana de ajustes. */
final class UiUtil {

    private static boolean bundledLoaded = false;
    private static String customFile = "";
    private static String customFamily = "";

    private UiUtil() {}

    /** Registra la fuente que viene dentro del jar (una sola vez). */
    static void registerBundledFonts() {
        if (bundledLoaded) return;
        bundledLoaded = true;
        try (InputStream in = UiUtil.class.getResourceAsStream("/fonts/Michroma.ttf")) {
            if (in != null) Font.loadFont(in, 12);
        } catch (IOException ignored) { }
    }

    /** Devuelve la familia a usar: el .ttf propio del usuario si existe, si no la familia elegida. */
    static String resolveFontFamily(TraxSettings cfg) {
        registerBundledFonts();
        String file = cfg.fontFile == null ? "" : cfg.fontFile.trim();
        if (!file.isEmpty()) {
            if (file.equals(customFile)) return customFamily;
            Path p = Path.of(file);
            if (Files.isRegularFile(p)) {
                try (InputStream in = Files.newInputStream(p)) {
                    Font f = Font.loadFont(in, 12);
                    if (f != null) {
                        customFile = file;
                        customFamily = f.getFamily();
                        return customFamily;
                    }
                } catch (IOException ignored) { }
            }
        }
        return (cfg.fontFamily == null || cfg.fontFamily.isBlank()) ? "Michroma" : cfg.fontFamily;
    }

    static Color color(String hex) {
        try { return Color.web(hex); } catch (Exception e) { return Color.web("#c9a227"); }
    }

    static String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255));
    }
}
