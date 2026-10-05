package cl.feliminish;

import cl.feliminish.service.SingleInstance;
import cl.feliminish.service.StartupManager;
import javafx.application.Application;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        boolean background = Arrays.asList(args).contains(StartupManager.BACKGROUND_ARG);

        // Una sola instancia: si ya hay otra corriendo, le avisa (que muestre sus ajustes) y esta se cierra.
        // Si se abrió desde el inicio de Windows (--background) solo avisa, sin abrir la ventana.
        if (!SingleInstance.acquireOrSignal(background ? "PING" : "SHOW")) {
            return;
        }
        Application.launch(TraxApp.class, args);
    }
}
