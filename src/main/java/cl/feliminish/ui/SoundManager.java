package cl.feliminish.ui;

import cl.feliminish.settings.SettingsStore;
import cl.feliminish.settings.TraxSettings;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.media.AudioClip;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Sonidos de la interfaz. La app no trae sonidos incluidos: el usuario coloca sus propios WAV en
 * ~/.retrotrax/sounds (click.wav, hover.wav, slider.wav, select.wav, open.wav, close.wav, trax.wav).
 * Si falta alguno simplemente no suena (select usa click como respaldo).
 */
public final class SoundManager {

    public enum Sfx {
        CLICK("click"), HOVER("hover"), SLIDER("slider"), SELECT("select"), OPEN("open"), CLOSE("close"), TRAX("trax");

        final String file;
        Sfx(String file) { this.file = file; }
    }

    private static final Path DIR = Path.of(System.getProperty("user.home"), ".retrotrax", "sounds");
    private static final Map<Sfx, AudioClip> CLIPS = new EnumMap<>(Sfx.class);
    private static boolean loaded = false;
    private static long lastSlider = 0;

    private SoundManager() {}

    private static void loadAll() {
        if (loaded) return;
        loaded = true;
        try {
            Files.createDirectories(DIR); // así el usuario encuentra la carpeta lista para llenar
        } catch (IOException ignored) { }
        for (Sfx s : Sfx.values()) {
            Path file = DIR.resolve(s.file + ".wav");
            if (!Files.isRegularFile(file)) continue;
            try {
                CLIPS.put(s, new AudioClip(file.toUri().toString()));
            } catch (Exception e) {
                System.err.println("No se pudo cargar el sonido " + s.file + ": " + e.getMessage());
            }
        }
    }

    /** Abre la carpeta de sonidos en el explorador de archivos del sistema. */
    public static void openFolder() {
        try {
            Files.createDirectories(DIR);
            String os = System.getProperty("os.name", "").toLowerCase();
            String opener = os.contains("win") ? "explorer" : os.contains("mac") ? "open" : "xdg-open";
            new ProcessBuilder(opener, DIR.toString()).start();
        } catch (IOException e) {
            System.err.println("No se pudo abrir la carpeta de sonidos: " + e.getMessage());
        }
    }

    public static void play(Sfx sfx) {
        TraxSettings cfg = SettingsStore.get();
        if (!cfg.soundEnabled || cfg.soundVolume <= 0) return;
        loadAll();

        AudioClip clip = CLIPS.get(sfx);
        if (clip == null && sfx == Sfx.SELECT) clip = CLIPS.get(Sfx.CLICK); // sin select.wav usamos click
        if (clip == null) return;

        // El "tick" del slider se limita para que no suene en cada píxel que se arrastra
        if (sfx == Sfx.SLIDER) {
            long now = System.currentTimeMillis();
            if (now - lastSlider < 90) return;
            lastSlider = now;
        }
        clip.play(Math.max(0, Math.min(1, cfg.soundVolume)));
    }

    // ── Conexión con controles ──

    /** Botón: hover al pasar el mouse y click al pulsar. */
    public static void attach(ButtonBase b) {
        b.setOnMouseEntered(e -> play(Sfx.HOVER));
        b.addEventHandler(javafx.event.ActionEvent.ACTION, e -> play(Sfx.CLICK));
    }

    /** Casilla: hover y "select" al marcar o desmarcar. */
    public static void attach(CheckBox c) {
        c.setOnMouseEntered(e -> play(Sfx.HOVER));
        c.addEventHandler(javafx.event.ActionEvent.ACTION, e -> play(Sfx.SELECT));
    }

    /** Slider: tick solo cuando lo mueve el usuario (no cuando el programa cambia el valor). */
    public static void attach(Slider s) {
        s.valueChangingProperty().addListener((o, a, changing) -> { if (changing) play(Sfx.CLICK); });
        s.valueProperty().addListener((o, a, n) -> { if (s.isValueChanging() || s.isFocused() || s.isPressed()) play(Sfx.SLIDER); });
    }

    /** Cualquier nodo (muestras de color, etc.): hover y click por mouse. */
    public static void attach(Node n) {
        n.setOnMouseEntered(e -> play(Sfx.HOVER));
    }
}
