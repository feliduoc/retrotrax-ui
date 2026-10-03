package cl.feliminish.ui;

import javafx.application.Platform;

import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

/** Icono en la bandeja de Windows (junto al reloj). En macOS no se usa: AWT y JavaFX se llevan mal ahí. */
public final class TrayIconManager {

    private static TrayIcon icon;

    private TrayIconManager() {}

    /** @return true si la bandeja quedó instalada */
    public static boolean install(Runnable openSettings, Runnable quit) {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (!windows || !SystemTray.isSupported()) return false;
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new java.awt.Color(0xc9a227));
            g.fillOval(0, 0, 15, 15);
            g.setColor(java.awt.Color.BLACK);
            g.fillOval(4, 4, 7, 7);
            g.dispose();

            PopupMenu menu = new PopupMenu();
            MenuItem settings = new MenuItem("Ajustes");
            settings.addActionListener(e -> Platform.runLater(openSettings));
            MenuItem exit = new MenuItem("Salir");
            exit.addActionListener(e -> Platform.runLater(quit));
            menu.add(settings);
            menu.addSeparator();
            menu.add(exit);

            icon = new TrayIcon(img, "Retro Trax", menu);
            icon.addActionListener(e -> Platform.runLater(openSettings)); // doble clic
            SystemTray.getSystemTray().add(icon);
            return true;
        } catch (AWTException | RuntimeException e) {
            System.err.println("No se pudo crear el icono de bandeja: " + e.getMessage());
            return false;
        }
    }

    public static void remove() {
        if (icon != null) {
            SystemTray.getSystemTray().remove(icon);
            icon = null;
        }
    }
}
