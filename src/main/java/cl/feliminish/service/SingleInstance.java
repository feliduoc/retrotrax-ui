package cl.feliminish.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Deja una sola instancia de la app. La primera abre un puerto local (solo accesible desde este PC);
 * si se abre una segunda, se conecta a ese puerto, le pide a la primera que muestre sus ajustes y se cierra.
 */
public final class SingleInstance {

    private static final int PORT = 52317;
    private static final String GREETING = "RETROTRAX";

    private static ServerSocket server;
    private static Runnable showHandler;
    private static boolean pendingShow = false;

    private SingleInstance() {}

    /**
     * @param command "SHOW" para que la instancia que ya corre muestre sus ajustes, o "PING" para no hacer nada
     * @return true si esta es la primera instancia y debe seguir arrancando; false si ya había otra
     *         (ya se le avisó) y esta debe cerrarse.
     */
    public static boolean acquireOrSignal(String command) {
        try {
            ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(false); // en Windows, true permitiría que dos procesos usen el mismo puerto
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), PORT), 2);
            server = ss;
            startListening(ss);
            return true;
        } catch (IOException busy) {
            // El puerto está ocupado: ¿es otra copia de la app?
            if (signalRunningInstance(command)) return false;
            // Lo ocupa otro programa: no podemos vigilar instancias, pero tampoco bloqueamos el arranque
            System.err.println("No se pudo comprobar si ya hay otra instancia abierta (puerto ocupado).");
            return true;
        }
    }

    private static boolean signalRunningInstance(String command) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), PORT), 800);
            s.setSoTimeout(1500);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            if (!GREETING.equals(in.readLine())) return false;
            PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8);
            out.println(command);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void startListening(ServerSocket ss) {
        Thread t = new Thread(() -> {
            while (!ss.isClosed()) {
                try (Socket c = ss.accept()) {
                    c.setSoTimeout(2000);
                    PrintWriter out = new PrintWriter(c.getOutputStream(), true, StandardCharsets.UTF_8);
                    out.println(GREETING);
                    BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                    if ("SHOW".equals(in.readLine())) requestShow();
                } catch (IOException ignored) {
                    // conexión rota o cerrada: se sigue escuchando
                }
            }
        }, "single-instance");
        t.setDaemon(true);
        t.start();
    }

    private static synchronized void requestShow() {
        if (showHandler != null) showHandler.run();
        else pendingShow = true; // llegó antes de que la ventana de ajustes existiera
    }

    /** La app llama a esto cuando ya puede mostrar sus ajustes. */
    public static synchronized void setShowHandler(Runnable handler) {
        showHandler = handler;
        if (pendingShow) {
            pendingShow = false;
            handler.run();
        }
    }

    public static void release() {
        try {
            if (server != null) server.close();
        } catch (IOException ignored) { }
    }
}
