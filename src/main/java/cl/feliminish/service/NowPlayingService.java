package cl.feliminish.service;

import cl.feliminish.model.Song;
import javafx.application.Platform;

import java.util.function.Consumer;

/** Elige la fuente según el sistema operativo y avisa (en el hilo de JavaFX) cuando cambia la canción. */
public class NowPlayingService {

    private final Consumer<Song> listener;
    private final MediaSource source;
    private String lastKey = "";

    public NowPlayingService(Consumer<Song> listener) {
        this.listener = listener;
        this.source = pickSource();
    }

    private static MediaSource pickSource() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return new WindowsMediaSource();
        if (os.contains("mac")) return new MacMediaSource();
        return null;
    }

    public void start() {
        if (source == null) {
            System.err.println("Sistema operativo no soportado todavía (solo Windows y macOS).");
            return;
        }
        source.start(this::process);
    }

    public void stop() {
        if (source != null) source.stop();
    }

    private void process(TrackInfo info) {
        if (info == null || info.title == null || info.title.isBlank()) return;

        String key = info.title + "|" + info.artist;
        if (key.equals(lastKey)) return; // misma canción de antes: no avisar
        lastKey = key;

        String artist = info.artist == null ? "" : info.artist.trim();
        String album = info.album == null ? "" : info.album.trim();

        // Apple Music (Windows) entrega "Artista — Álbum" en un solo campo y deja el álbum vacío
        if (album.isEmpty()) {
            String[] parts = artist.split("\\s+[\u2014\u2013]\\s+", 2); // raya larga o media con espacios
            if (parts.length == 2) {
                artist = parts[0].trim();
                album = parts[1].trim();
            }
        }

        Song song = new Song(info.title, artist, album);
        // Llegamos desde un hilo ajeno: la UI solo se toca desde el hilo de JavaFX
        Platform.runLater(() -> listener.accept(song));
    }
}
