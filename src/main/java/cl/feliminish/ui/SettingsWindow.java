package cl.feliminish.ui;

import cl.feliminish.model.Song;
import cl.feliminish.service.StartupManager;
import cl.feliminish.settings.SettingsStore;
import cl.feliminish.settings.TraxSettings;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;

/** Ventana de ajustes con la estética de los menús Most Wanted. */
public class SettingsWindow {

    private static final String GOLD = "#e0a030";
    private static final String[] PRESETS = {"#c4943c", "#ff8a00", "#e23a2e", "#2ec4ff", "#4cd964", "#ffffff"};

    private final Stage stage = new Stage(StageStyle.UNDECORATED);
    private boolean hideOnClose = false;

    private final TextField badge = new TextField();
    private final TextField tag = new TextField();
    private final ComboBox<String> font = new ComboBox<>();
    private final Label fontFileLbl = new Label();
    private final Label logoLbl = new Label();
    private final TextField hex = new TextField();
    private final Region preview = new Region();
    private final HBox swatches = new HBox(6);
    private final Slider hold = new Slider(1, 15, 4);
    private final Label holdLbl = new Label();
    private final Slider scale = new Slider(0.6, 2.0, 1.0);
    private final Label scaleLbl = new Label();
    private final ToggleGroup cornerGroup = new ToggleGroup();
    private final Map<TraxSettings.Corner, ToggleButton> cornerButtons = new EnumMap<>(TraxSettings.Corner.class);
    private final CheckBox italicBadge = new CheckBox("Cursiva en el círculo");
    private final CheckBox italicBar = new CheckBox("Cursiva en la barra");
    private final CheckBox enabled = new CheckBox("Mostrar notificaciones");
    private final CheckBox startup = new CheckBox("Iniciar con Windows");
    private final Label startupHint = new Label("(solo funciona desde el .exe)");
    private final CheckBox soundOn = new CheckBox("Sonidos de la interfaz");
    private final Slider volume = new Slider(0, 1, 0.6);
    private final Label volumeLbl = new Label();
    private final Label status = new Label();

    private String fontFile = "";
    private String logoImage = "";

    public SettingsWindow(Runnable onQuit) {
        UiUtil.registerBundledFonts();
        font.setItems(FXCollections.observableArrayList(Font.getFamilies()));
        font.setMaxWidth(Double.MAX_VALUE);
        status.getStyleClass().add("mw-status");

        hold.valueProperty().addListener((o, a, n) -> holdLbl.setText(String.format("%.1f s", n.doubleValue())));
        scale.valueProperty().addListener((o, a, n) -> scaleLbl.setText(String.format("%.0f%%", n.doubleValue() * 100)));
        volume.valueProperty().addListener((o, a, n) -> volumeLbl.setText(String.format("%.0f%%", n.doubleValue() * 100)));
        volumeLbl.getStyleClass().add("mw-value");
        holdLbl.getStyleClass().add("mw-value");
        scaleLbl.getStyleClass().add("mw-value");
        fontFileLbl.getStyleClass().add("mw-hint");
        logoLbl.getStyleClass().add("mw-hint");
        hex.setPrefColumnCount(7);
        hex.textProperty().addListener((o, a, n) -> refreshColor());

        // ── Archivos ──
        Button pickLogo = mini("Elegir imagen…", () -> {
            File f = choose("Elegir logo", "Imágenes", "*.png", "*.jpg", "*.jpeg");
            if (f != null) { logoImage = f.getAbsolutePath(); refreshFiles(); }
        });
        Button clearLogo = mini("Quitar", () -> { logoImage = ""; refreshFiles(); });
        Button pickFont = mini("Cargar .ttf…", () -> {
            File f = choose("Elegir fuente", "Fuentes", "*.ttf", "*.otf");
            if (f != null) { fontFile = f.getAbsolutePath(); refreshFiles(); }
        });
        Button clearFont = mini("Quitar", () -> { fontFile = ""; refreshFiles(); });

        // ── Color: muestras rápidas + hex ──
        preview.setMinSize(26, 26);
        preview.setPrefSize(26, 26);
        for (String c : PRESETS) {
            Region sw = new Region();
            sw.setMinSize(20, 20);
            sw.setPrefSize(20, 20);
            sw.getStyleClass().add("mw-swatch");
            sw.setStyle("-fx-background-color: " + c + ";");
            sw.setOnMouseClicked(e -> hex.setText(c));
            swatches.getChildren().add(sw);
        }
        HBox colorRow = new HBox(10, preview, hex, swatches);
        colorRow.setAlignment(Pos.CENTER_LEFT);

        // ── Posición: 2x2 ──
        GridPane corners = new GridPane();
        corners.setHgap(6);
        corners.setVgap(6);
        int i = 0;
        for (TraxSettings.Corner c : TraxSettings.Corner.values()) {
            ToggleButton b = new ToggleButton(c.toString());
            b.getStyleClass().add("mw-corner");
            b.setToggleGroup(cornerGroup);
            b.setUserData(c);
            b.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(b, Priority.ALWAYS);
            cornerButtons.put(c, b);
            corners.add(b, i % 2, i / 2);
            i++;
        }
        // Que siempre haya una esquina elegida
        cornerGroup.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null && old != null) old.setSelected(true);
        });

        // ── Formulario ──
        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(14);
        ColumnConstraints c0 = new ColumnConstraints(150);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(c0, c1);

        int r = 0;
        r = row(form, r, "Texto del círculo", badge);
        r = row(form, r, "Texto lateral", tag);
        r = row(form, r, "Logo propio", new VBox(4, new HBox(6, pickLogo, clearLogo), logoLbl));
        r = row(form, r, "Fuente", font);
        r = row(form, r, "Fuente propia", new VBox(4, new HBox(6, pickFont, clearFont), fontFileLbl));
        r = row(form, r, "Color", colorRow);
        r = row(form, r, "Segundos abierta", sliderRow(hold, holdLbl));
        r = row(form, r, "Tamaño", sliderRow(scale, scaleLbl));
        r = row(form, r, "Posición", corners);
        r = row(form, r, "Estilo", new VBox(8, italicBadge, italicBar));
        r = row(form, r, "", enabled);
        if (StartupManager.isWindows()) {
            startupHint.getStyleClass().add("mw-hint");
            boolean ok = StartupManager.isSupported();
            startup.setDisable(!ok);
            startupHint.setVisible(!ok);
            startupHint.setManaged(!ok);
            r = row(form, r, "", new VBox(4, startup, startupHint));
        }
        Label soundHint = new Label("Coloca tus WAV (click, hover, slider, select, open, close) en esta carpeta");
        soundHint.getStyleClass().add("mw-hint");
        soundHint.setWrapText(true);
        Button openSounds = mini("Abrir carpeta de sonidos", SoundManager::openFolder);
        r = row(form, r, "Sonido", new VBox(8, soundOn, sliderRow(volume, volumeLbl), openSounds, soundHint));

        // ── Sonidos ──
        soundOn.selectedProperty().addListener((o, a, n) -> SettingsStore.get().soundEnabled = n);
        volume.valueProperty().addListener((o, a, n) -> SettingsStore.get().soundVolume = n.doubleValue());
        SoundManager.attach(italicBadge);
        SoundManager.attach(italicBar);
        SoundManager.attach(enabled);
        SoundManager.attach(startup);
        SoundManager.attach(soundOn);
        SoundManager.attach(hold);
        SoundManager.attach(scale);
        SoundManager.attach(volume);
        font.setOnShowing(e -> SoundManager.play(SoundManager.Sfx.CLICK));
        font.valueProperty().addListener((o, a, v) -> { if (stage.isShowing()) SoundManager.play(SoundManager.Sfx.SELECT); });
        for (ToggleButton tb : cornerButtons.values()) SoundManager.attach(tb);
        for (Node sw : swatches.getChildren()) {
            SoundManager.attach(sw);
            sw.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> SoundManager.play(SoundManager.Sfx.SELECT));
        }

        ScrollPane scroll = new ScrollPane(form);
        scroll.getStyleClass().add("mw-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        form.setPadding(new Insets(20, 26, 20, 26));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ── Pie con los botones principales ──
        Node test = bracketButton("PROBAR", () -> {
            apply();
            TraxNotification.show(new Song("Nombre canción", "Artista", "Álbum"));
        });
        Node save = bracketButton("GUARDAR", () -> {
            apply();
            SettingsStore.save();
            if (StartupManager.isSupported() && !StartupManager.setEnabled(startup.isSelected())) {
                status.setText("Guardado, pero no se pudo cambiar el inicio con Windows");
                return;
            }
            status.setText("Guardado");
        });
        Node quit = bracketButton("SALIR", () -> {
            SoundManager.play(SoundManager.Sfx.CLOSE);
            PauseTransition wait = new PauseTransition(Duration.millis(450)); // deja sonar antes de cerrar
            wait.setOnFinished(e -> onQuit.run());
            wait.play();
        });
        HBox buttons = new HBox(18, test, save, quit);
        buttons.setAlignment(Pos.CENTER);
        VBox footer = new VBox(6, buttons, status);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(12, 0, 12, 0));

        // ── Panel principal ──
        VBox panel = new VBox(header(), band(), scroll, band(), footer);
        panel.getStyleClass().add("mw-panel");

        // ── Esquinas blancas de la ventana ──
        StackPane root = new StackPane(panel);
        root.getStyleClass().add("mw-root");
        root.setPadding(new Insets(9));
        root.getChildren().addAll(
                corner(Pos.TOP_LEFT, 0, 16, 0, 0, 16, 0),
                corner(Pos.TOP_RIGHT, 0, 0, 16, 0, 16, 16),
                corner(Pos.BOTTOM_LEFT, 0, 0, 0, 16, 16, 16),
                corner(Pos.BOTTOM_RIGHT, 16, 0, 16, 16, 0, 16));

        Scene scene = new Scene(root);
        var css = SettingsWindow.class.getResource("/cl/feliminish/ui/settings.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Retro Trax: ajustes");
        stage.setResizable(false);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setWidth(620);
        stage.setHeight(Math.min(720, screen.getHeight() - 40));

        load();
    }

    /** Con bandeja del sistema: cerrar la ventana la oculta. Sin bandeja: la minimiza. */
    public void setHideOnClose(boolean hide) { this.hideOnClose = hide; }

    public void show() {
        load();
        status.setText("");
        SoundManager.play(SoundManager.Sfx.OPEN);
        stage.show();
        stage.setIconified(false);
        stage.toFront();
    }

    // ───────────────────────── construcción de piezas ─────────────────────────

    /** Cabecera: triángulo con "!", título, y a la derecha el icono redondo dorado y los botones de ventana. */
    private Node header() {
        Polygon tri = new Polygon(0, 0, 28, 0, 14, 24);
        tri.setFill(Color.TRANSPARENT);
        tri.setStroke(Color.WHITE);
        tri.setStrokeWidth(2.6);
        Label bang = new Label("!");
        bang.getStyleClass().add("mw-bang");
        StackPane warn = new StackPane(tri, bang);
        warn.setPadding(new Insets(0, 0, 4, 0));

        Label title = new Label("retro/ajustes");
        title.getStyleClass().add("mw-title");

        Polygon arrow = new Polygon(0, 0, 8, 5, 0, 10);
        arrow.setFill(Color.web(GOLD));

        Circle ring = new Circle(19);
        ring.setFill(Color.web("#1a1208"));
        ring.setStroke(Color.web(GOLD));
        ring.setStrokeWidth(2.6);
        Label note = new Label("\u266A");
        note.getStyleClass().add("mw-note");
        StackPane icon = new StackPane(ring, note);

        Button min = new Button("_");
        min.getStyleClass().add("mw-win");
        min.setOnAction(e -> stage.setIconified(true));
        SoundManager.attach(min);
        Button close = new Button("X");
        close.getStyleClass().add("mw-win");
        close.setOnAction(e -> closeWindow());
        SoundManager.attach(close);
        HBox win = new HBox(2, min, close);
        win.setAlignment(Pos.TOP_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox h = new HBox(12, warn, title, spacer, arrow, icon, win);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("mw-header");

        // La ventana no tiene marco: se arrastra desde la cabecera
        double[] off = new double[2];
        h.setOnMousePressed(e -> { off[0] = e.getSceneX(); off[1] = e.getSceneY(); });
        h.setOnMouseDragged(e -> { stage.setX(e.getScreenX() - off[0]); stage.setY(e.getScreenY() - off[1]); });
        return h;
    }

    /** Franja con rayas diagonales entre secciones. */
    private static Region band() {
        Region r = new Region();
        r.getStyleClass().add("mw-band");
        r.setMinHeight(16);
        r.setPrefHeight(16);
        return r;
    }

    /** Botón grande gris con texto dorado y corchetes en las esquinas, como el "OK" del juego. */
    private static Node bracketButton(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("mw-ok");
        b.setOnAction(e -> action.run());
        SoundManager.attach(b);
        StackPane sp = new StackPane(b,
                bracket(Pos.TOP_LEFT, 0, 9, 0, 0, 9, 0),
                bracket(Pos.TOP_RIGHT, 0, 0, 9, 0, 9, 9),
                bracket(Pos.BOTTOM_LEFT, 0, 0, 0, 9, 9, 9),
                bracket(Pos.BOTTOM_RIGHT, 9, 0, 9, 9, 0, 9));
        sp.setPadding(new Insets(4));
        return sp;
    }

    private static Node bracket(Pos pos, double x1, double y1, double x2, double y2, double x3, double y3) {
        Path p = new Path(new MoveTo(x1, y1), new LineTo(x2, y2), new LineTo(x3, y3));
        p.setStroke(Color.web(GOLD));
        p.setStrokeWidth(2);
        p.setMouseTransparent(true);
        Group g = new Group(p);
        StackPane.setAlignment(g, pos);
        return g;
    }

    /** Esquinas blancas de la ventana. */
    private static Node corner(Pos pos, double x1, double y1, double x2, double y2, double x3, double y3) {
        Path p = new Path(new MoveTo(x1, y1), new LineTo(x2, y2), new LineTo(x3, y3));
        p.setStroke(Color.WHITE);
        p.setStrokeWidth(3);
        Group g = new Group(p);
        g.setMouseTransparent(true);
        StackPane.setAlignment(g, pos);
        return g;
    }

    private static Button mini(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("mw-mini");
        b.setOnAction(e -> action.run());
        SoundManager.attach(b);
        return b;
    }

    private static HBox sliderRow(Slider s, Label value) {
        value.setMinWidth(54);
        value.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(s, Priority.ALWAYS);
        HBox h = new HBox(10, s, value);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static int row(GridPane g, int r, String label, Node control) {
        Label l = new Label(label);
        l.getStyleClass().add("mw-label");
        l.setWrapText(true);
        GridPane.setValignment(l, javafx.geometry.VPos.TOP);
        g.add(l, 0, r);
        g.add(control, 1, r);
        return r + 1;
    }

    // ───────────────────────── datos ─────────────────────────

    private void load() {
        TraxSettings s = SettingsStore.get();
        badge.setText(s.badgeText);
        tag.setText(s.tagText);
        font.setValue(s.fontFamily);
        hex.setText(UiUtil.toHex(UiUtil.color(s.accentColor)));
        hold.setValue(s.holdSeconds);
        holdLbl.setText(String.format("%.1f s", hold.getValue()));
        scale.setValue(s.scale);
        scaleLbl.setText(String.format("%.0f%%", scale.getValue() * 100));
        ToggleButton cb = cornerButtons.get(s.corner);
        if (cb != null) cb.setSelected(true);
        italicBadge.setSelected(s.italicBadge);
        italicBar.setSelected(s.italicBar);
        enabled.setSelected(s.enabled);
        startup.setSelected(StartupManager.isEnabled()); // el estado real vive en el registro de Windows
        soundOn.setSelected(s.soundEnabled);
        volume.setValue(s.soundVolume);
        volumeLbl.setText(String.format("%.0f%%", volume.getValue() * 100));
        fontFile = s.fontFile == null ? "" : s.fontFile;
        logoImage = s.logoImage == null ? "" : s.logoImage;
        refreshFiles();
        refreshColor();
    }

    /** Pasa lo que hay en pantalla al objeto de configuración (todavía sin guardar en disco). */
    private void apply() {
        TraxSettings s = SettingsStore.get();
        s.badgeText = badge.getText().trim();
        s.tagText = tag.getText().trim();
        if (font.getValue() != null) s.fontFamily = font.getValue();
        s.fontFile = fontFile;
        s.logoImage = logoImage;
        s.accentColor = UiUtil.toHex(UiUtil.color(hex.getText().trim()));
        s.holdSeconds = Math.round(hold.getValue() * 10) / 10.0;
        s.scale = Math.round(scale.getValue() * 20) / 20.0;
        if (cornerGroup.getSelectedToggle() != null) {
            s.corner = (TraxSettings.Corner) cornerGroup.getSelectedToggle().getUserData();
        }
        s.italicBadge = italicBadge.isSelected();
        s.italicBar = italicBar.isSelected();
        s.enabled = enabled.isSelected();
        s.soundEnabled = soundOn.isSelected();
        s.soundVolume = Math.round(volume.getValue() * 20) / 20.0;
    }

    private void refreshFiles() {
        fontFileLbl.setText(fontFile.isEmpty() ? "(ninguna)" : new File(fontFile).getName());
        logoLbl.setText(logoImage.isEmpty() ? "(usa el texto del círculo)" : new File(logoImage).getName());
    }

    private void refreshColor() {
        String text = hex.getText() == null ? "" : hex.getText().trim();
        String shown = UiUtil.toHex(UiUtil.color(text.isEmpty() ? "#c4943c" : text));
        preview.setStyle("-fx-background-color: " + shown + "; -fx-border-color: white; -fx-border-width: 1.5;");
    }

    private void closeWindow() {
        SoundManager.play(SoundManager.Sfx.CLOSE);
        if (hideOnClose) stage.hide(); else stage.setIconified(true);
    }

    private File choose(String title, String desc, String... exts) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, exts));
        return fc.showOpenDialog(stage);
    }
}
