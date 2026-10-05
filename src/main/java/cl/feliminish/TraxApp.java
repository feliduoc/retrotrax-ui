package cl.feliminish;

import cl.feliminish.service.NowPlayingService;
import cl.feliminish.service.SingleInstance;
import cl.feliminish.service.StartupManager;
import cl.feliminish.settings.SettingsStore;
import cl.feliminish.ui.SettingsWindow;
import cl.feliminish.ui.TrayIconManager;
import cl.feliminish.ui.TraxNotification;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class TraxApp extends Application {

    private static Stage owner;
    private NowPlayingService service;

    /** Ventana invisible que "es dueña" de los popups para que no aparezcan en la barra de tareas. */
    public static Stage owner() { return owner; }

    @Override
    public void start(Stage primary) {
        Platform.setImplicitExit(false); // la app sigue viva aunque no haya ventanas visibles

        owner = new Stage(StageStyle.UTILITY);
        owner.setScene(new Scene(new StackPane(), 1, 1));
        owner.setOpacity(0);
        owner.setX(-10000);
        owner.setY(-10000);
        owner.show();

        SettingsStore.load();

        SettingsWindow settings = new SettingsWindow(this::quit);
        // Si el usuario abre el programa otra vez, esta instancia muestra sus ajustes en vez de abrirse una segunda
        SingleInstance.setShowHandler(() -> Platform.runLater(settings::show));
        boolean tray = TrayIconManager.install(settings::show, this::quit);
        settings.setHideOnClose(tray);
        // Desde el inicio de Windows arranca en silencio (solo si hay bandeja para volver a abrir los ajustes)
        boolean background = getParameters().getRaw().contains(StartupManager.BACKGROUND_ARG);
        if (!(background && tray)) settings.show();

        service = new NowPlayingService(TraxNotification::show);
        service.start();
    }

    private void quit() {
        if (service != null) service.stop();
        TrayIconManager.remove();
        SingleInstance.release();
        Platform.exit();
        System.exit(0);
    }
}
