package cl.feliminish.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * "Iniciar con Windows": una entrada en HKCU\Software\Microsoft\Windows\CurrentVersion\Run
 * (no necesita permisos de administrador). Solo funciona desde el .exe empaquetado.
 */
public final class StartupManager {

    /** Argumento con el que arranca desde el inicio de Windows: no abre la ventana de ajustes. */
    public static final String BACKGROUND_ARG = "--background";

    private static final String KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String NAME = "RetroTrax";

    private StartupManager() {}

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /** Ruta del .exe que está corriendo, o null si estamos en el IDE (ahí el proceso es java.exe). */
    private static String exePath() {
        String cmd = ProcessHandle.current().info().command().orElse(null);
        if (cmd == null) return null;
        String lower = cmd.toLowerCase();
        if (!lower.endsWith(".exe") || lower.endsWith("\\java.exe") || lower.endsWith("\\javaw.exe")) return null;
        return cmd;
    }

    /** true si estamos en Windows y corriendo desde el ejecutable (no desde el IDE ). */
    public static boolean isSupported() {
        return isWindows() && exePath() != null;
    }

    public static boolean isEnabled() {
        if (!isWindows()) return false;
        return run("reg", "query", KEY, "/v", NAME) == 0;
    }

    /** @return true si el cambio se aplicó */
    public static boolean setEnabled(boolean on) {
        String exe = exePath();
        if (!isWindows() || exe == null) return false;
        if (on) {
            String value = "\"" + exe + "\" " + BACKGROUND_ARG;
            return run("reg", "add", KEY, "/v", NAME, "/t", "REG_SZ", "/d", value, "/f") == 0;
        }
        if (!isEnabled()) return true; // ya estaba apagado
        return run("reg", "delete", KEY, "/v", NAME, "/f") == 0;
    }

    private static int run(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes(); // vaciar la salida para que no se bloquee
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return -1;
            }
            return p.exitValue();
        } catch (IOException | InterruptedException e) {
            return -1;
        }
    }
}
