package cl.feliminish.service;

import java.util.function.Consumer;

/** Una forma de averiguar qué suena, según el sistema operativo. Llama a onTrack desde su propio hilo. */
public interface MediaSource {
    void start(Consumer<TrackInfo> onTrack);
    void stop();
}
