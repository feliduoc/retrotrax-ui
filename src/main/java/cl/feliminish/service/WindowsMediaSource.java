package cl.feliminish.service;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

/** Windows: lee el SMTC (lo mismo que muestra el control de volumen) con un script de PowerShell. */
public class WindowsMediaSource implements MediaSource {

    private final Gson gson = new Gson();
    private volatile Process process;

    @Override
    public void start(Consumer<TrackInfo> onTrack) {
        Thread t = new Thread(() -> run(onTrack), "now-playing-windows");
        t.setDaemon(true);
        t.start();
    }

    private void run(Consumer<TrackInfo> onTrack) {
        try {
            // El script vive dentro del jar; lo sacamos a un temporal para que PowerShell pueda ejecutarlo
            Path script = Files.createTempFile("retrotrax-nowplaying", ".ps1");
            script.toFile().deleteOnExit();
            try (InputStream in = getClass().getResourceAsStream("/scripts/nowplaying.ps1")) {
                if (in == null) throw new IllegalStateException("Falta /scripts/nowplaying.ps1 en resources");
                Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
            }

            process = new ProcessBuilder("powershell.exe", "-ExecutionPolicy", "Bypass",
                    "-NoProfile", "-WindowStyle", "Hidden", "-File", script.toString()).start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        TrackInfo info = gson.fromJson(line, TrackInfo.class);
                        if (info != null) onTrack.accept(info);
                    } catch (Exception ignored) { /* línea que no es JSON: se ignora */ }
                }
            }
        } catch (Exception e) {
            System.err.println("Error leyendo la canción (Windows): " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        Process p = process;
        if (p != null) p.destroy();
    }
}
