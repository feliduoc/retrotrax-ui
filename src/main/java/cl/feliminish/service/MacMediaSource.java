package cl.feliminish.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * macOS: consulta Spotify y Music con AppleScript cada 2 segundos.
 * La primera vez macOS pedirá permiso de "Automatización" para controlar esas apps.
 */
public class MacMediaSource implements MediaSource {

    private static final String[] APPS = {"Spotify", "Music"};
    private ScheduledExecutorService executor;

    @Override
    public void start(Consumer<TrackInfo> onTrack) {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "now-playing-mac");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(() -> poll(onTrack), 0, 2, TimeUnit.SECONDS);
    }

    private void poll(Consumer<TrackInfo> onTrack) {
        for (String app : APPS) {
            String out = runScript(script(app));
            if (out.isBlank()) continue;

            String[] p = out.split("\\|\\|\\|", -1);
            TrackInfo info = new TrackInfo();
            info.title = p.length > 0 ? p[0].trim() : "";
            info.artist = p.length > 1 ? p[1].trim() : "";
            info.album = p.length > 2 ? p[2].trim() : "";
            info.app = app;
            onTrack.accept(info);
            return; // la primera app que esté sonando gana
        }
    }

    // Un script por app: si una no está instalada, solo falla ese y seguimos con la otra
    private static String script(String app) {
        return "if application \"" + app + "\" is running then\n"
             + "  tell application \"" + app + "\"\n"
             + "    if player state is playing then\n"
             + "      return (name of current track) & \"|||\" & (artist of current track) & \"|||\" & (album of current track)\n"
             + "    end if\n"
             + "  end tell\n"
             + "end if\n"
             + "return \"\"";
    }

    private static String runScript(String script) {
        try {
            Process p = new ProcessBuilder("osascript", "-e", script).redirectErrorStream(false).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (!p.waitFor(5, TimeUnit.SECONDS)) p.destroyForcibly();
            return out;
        } catch (IOException | InterruptedException e) {
            return "";
        }
    }

    @Override
    public void stop() {
        if (executor != null) executor.shutdownNow();
    }
}
