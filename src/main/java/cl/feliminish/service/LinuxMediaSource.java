package cl.feliminish.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Linux: lee lo que suena por MPRIS (el estándar de Spotify, VLC, navegadores, etc.) usando la
 * herramienta "playerctl", que debe estar instalada:  sudo apt install playerctl
 * (Fedora: sudo dnf install playerctl; Arch: sudo pacman -S playerctl)
 */
public class LinuxMediaSource implements MediaSource {

    private static final String FORMAT = "{{status}}|||{{title}}|||{{artist}}|||{{album}}|||{{playerName}}";

    private volatile boolean running;
    private volatile Process process;

    @Override
    public void start(Consumer<TrackInfo> onTrack) {
        running = true;
        Thread t = new Thread(() -> loop(onTrack), "now-playing-linux");
        t.setDaemon(true);
        t.start();
    }

    private void loop(Consumer<TrackInfo> onTrack) {
        while (running) {
            try {
                // --follow: playerctl se queda corriendo e imprime una línea cada vez que algo cambia
                Process p = new ProcessBuilder("playerctl", "--follow", "metadata", "--format", FORMAT)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                process = p;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        TrackInfo info = parse(line);
                        if (info != null) onTrack.accept(info);
                    }
                }
                p.waitFor();
            } catch (IOException e) {
                // playerctl no está instalado: no tiene sentido reintentar cada pocos segundos
                System.err.println("No se pudo ejecutar 'playerctl' (" + e.getMessage() + "). "
                        + "Instálalo, por ejemplo con: sudo apt install playerctl");
                return;
            } catch (InterruptedException e) {
                return;
            }
            // playerctl termina si no hay reproductores abiertos; se vuelve a intentar un rato después
            sleep(3000);
        }
    }

    /** Línea → canción. Solo cuenta si el reproductor está en "Playing". Devuelve null si no hay nada que mostrar. */
    static TrackInfo parse(String line) {
        String[] p = line.split("\\|\\|\\|", -1);
        if (p.length < 4) return null;
        if (!"Playing".equalsIgnoreCase(p[0].trim())) return null;

        TrackInfo info = new TrackInfo();
        info.title = p[1].trim();
        info.artist = p[2].trim();
        info.album = p[3].trim();
        info.app = p.length > 4 ? p[4].trim() : "";
        return info.title.isEmpty() ? null : info;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        running = false;
        Process p = process;
        if (p != null) p.destroy();
    }
}
