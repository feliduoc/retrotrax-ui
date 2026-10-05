package cl.feliminish.ui;

import cl.feliminish.TraxApp;
import cl.feliminish.model.Song;
import cl.feliminish.settings.SettingsStore;
import cl.feliminish.settings.TraxSettings;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;

/**
 * parametros general del pop up de retrotax viene enlazado a trax css
 */
public class TraxNotification {

    // ── Geometría (px sin escalar) ──
    private static final double PAD = 14;          // margen para sombras
    private static final double D = 72;            // disco completo (aro incluido)
    private static final double WRAP = 92;         // caja del disco y los arcos
    private static final double R_ARC = 37;        // radio del arco fino: apenas por fuera del aro del disco
    private static final double BAR_H = 72;        // alto de la barra = diámetro del disco: los bordes dorados quedan tangentes al círculo
    private static final double BORDER = 4;        // borde dorado de la barra
    private static final double TAG_W = 30;        // etiqueta lateral
    private static final double INFO_X = 47;       // inicio del texto, medido desde el centro del disco
    private static final double INFO_MAX_W = 300;
    private static final double MIN_BAR_W = 260;   // del centro del disco al borde derecho
    private static final double HIDDEN_W = 30;     // ancho con la barra totalmente tapada por el disco
    private static final double MARGIN = 10;

    private static Stage activo;
    private static Animation animActual;

    public static void show(Song song) {
        TraxSettings cfg = SettingsStore.get();
        if (!cfg.enabled) return;

        if (animActual != null) animActual.stop();
        if (activo != null) { activo.close(); activo = null; }

        String family = UiUtil.resolveFontFamily(cfg);
        String accent = UiUtil.toHex(UiUtil.color(cfg.accentColor));
        double scale = Math.max(0.5, Math.min(3.0, cfg.scale));

        // ── Disco: aro degradado, borde interior oscuro y rayas (todo en el CSS) ──
        Node logo = buildBadgeContent(cfg);
        Region hatch = new Region();                 // rayas brillantes del destello de entrada
        hatch.getStyleClass().add("trax-hatch");
        fixSize(hatch, D - 10, D - 10);
        hatch.setMouseTransparent(true);
        hatch.setOpacity(0);

        StackPane disco = new StackPane(hatch, logo);
        disco.getStyleClass().add("trax-logo");
        fixSize(disco, D, D);
        disco.setOpacity(0);

        // ── Arco fino que rodea el círculo (el mismo se usa al entrar y al salir) ──
        Arc arc = new Arc(WRAP / 2, WRAP / 2, R_ARC, R_ARC, 55, -110);
        arc.setType(ArcType.OPEN);
        arc.setFill(null);
        arc.setStrokeWidth(2.2);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.getStyleClass().add("trax-arc");
        arc.setOpacity(0);
        Pane arcLayer = new Pane(arc);
        arcLayer.setMouseTransparent(true);

        Pane logoWrap = new Pane(disco, arcLayer);
        fixSize(logoWrap, WRAP, WRAP);
        disco.relocate((WRAP - D) / 2, (WRAP - D) / 2);

        // ── Texto: canción / artista / álbum ──
        VBox info = new VBox(1);
        Label title = new Label(song.getTitle());
        title.getStyleClass().add("trax-title");
        title.setStyle(fontStyle(cfg.italicBar));
        info.getChildren().add(title);
        if (notBlank(song.getArtist())) info.getChildren().add(sub(song.getArtist(), cfg.italicBar));
        if (notBlank(song.getAlbum()))  info.getChildren().add(sub(song.getAlbum(), cfg.italicBar));
        info.setOpacity(0);

        // ── Etiqueta lateral con el texto girado 90° ──
        String tagText = notBlank(cfg.tagText) ? cfg.tagText.trim() : "";
        Label tag = new Label(tagText);
        tag.getStyleClass().add("trax-tag");
        tag.setStyle("-fx-font-size: " + Math.max(6, 12 - Math.max(0, tagText.length() - 4)) + "px; "
                + fontStyle(cfg.italicBar));
        tag.setRotate(90);
        StackPane side = new StackPane(new Group(tag));
        side.getStyleClass().add("trax-side");
        fixSize(side, TAG_W, BAR_H);
        side.setVisible(!tagText.isEmpty());

        // ── Barra: marco dorado + relleno negro→café + etiqueta ──
        Region fill = new Region();
        fill.getStyleClass().add("trax-fill");
        fill.setManaged(false);                       // su tamaño lo manejamos nosotros
        fill.relocate(0, BORDER);

        Pane bar = new Pane(fill, info, side);
        bar.getStyleClass().add("trax-bar");
        bar.setManaged(false);
        bar.setOpacity(0);

        Pane root = new Pane(bar, logoWrap);          // el disco va encima de la barra
        root.getStyleClass().add("trax-overlay");
        root.setStyle("-trax-accent: " + accent + "; -fx-font-family: '" + family.replace("'", "\\'") + "';");

        Scene scene = new Scene(root, 1, 1);
        scene.setFill(Color.TRANSPARENT);
        var css = TraxNotification.class.getResource("/cl/feliminish/ui/trax.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        root.applyCss(); // para medir el texto ya con su fuente

        // ── Medidas ──
        double infoW = Math.min(INFO_MAX_W, info.prefWidth(-1));
        info.setMinWidth(infoW); info.setPrefWidth(infoW); info.setMaxWidth(infoW);
        double infoH = info.prefHeight(infoW);
        info.relocate(INFO_X, (BAR_H - infoH) / 2);

        double fullW = Math.max(MIN_BAR_W, INFO_X + infoW + 14 + TAG_W);
        double cx = PAD + WRAP / 2;
        double barX = cx;                              // la barra nace en el centro del disco
        double barY = PAD + (WRAP - D) / 2;            // borde superior de la barra = borde superior del disco
        logoWrap.relocate(PAD, PAD);
        bar.relocate(barX, barY);

        double rootW = Math.max(PAD + WRAP, barX + fullW) + PAD;
        double rootH = WRAP + 2 * PAD;
        root.getTransforms().add(new Scale(scale, scale, 0, 0));

        // Ancho animable: redimensiona barra, relleno, recorte y posición de la etiqueta
        DoubleProperty barW = new SimpleDoubleProperty(HIDDEN_W);
        // Recorte redondeado SOLO a la derecha: se extiende BAR_H/2 hacia la izquierda para que su extremo
        // redondo quede fuera de la barra y el borde izquierdo (oculto bajo el disco) sea recto.
        Rectangle clip = new Rectangle(-BAR_H / 2, 0, HIDDEN_W + BAR_H / 2, BAR_H);
        clip.setArcWidth(BAR_H);
        clip.setArcHeight(BAR_H);
        clip.widthProperty().bind(barW.add(BAR_H / 2));
        bar.setClip(clip);
        side.layoutXProperty().bind(barW.subtract(TAG_W));
        barW.addListener((o, a, n) -> {
            bar.resize(n.doubleValue(), BAR_H);
            fill.resize(Math.max(0, n.doubleValue() - TAG_W), BAR_H - 2 * BORDER);
        });
        bar.resize(HIDDEN_W, BAR_H);
        fill.resize(0, BAR_H - 2 * BORDER);

        // ── Ventana ──
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        if (TraxApp.owner() != null) stage.initOwner(TraxApp.owner()); // dueño oculto: sin icono en la barra de tareas
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);
        stage.setWidth(rootW * scale);
        stage.setHeight(rootH * scale);
        place(stage, cfg.corner, rootW * scale, rootH * scale);
        activo = stage;

        // Estado inicial del logo: grande y transparente (llega "volando" al disco)
        logo.setOpacity(0);
        logo.setScaleX(1.8);
        logo.setScaleY(1.8);

        // ═══════════ ENTRADA (900 ms) ═══════════
        Timeline intro = new Timeline(
                // disco (negro con aro) aparece
                kf(0,   v(disco.opacityProperty(), 0)),
                kf(400, v(disco.opacityProperty(), 0)),
                kf(600, v(disco.opacityProperty(), 1)),
                // logo: llega grande y se asienta
                kf(590, v(logo.opacityProperty(), 0), v(logo.scaleXProperty(), 1.8), v(logo.scaleYProperty(), 1.8)),
                kf(600, v(logo.opacityProperty(), 0.5)),
                kf(770, v(logo.opacityProperty(), 1, Interpolator.EASE_OUT),
                        v(logo.scaleXProperty(), 1, Interpolator.EASE_OUT), v(logo.scaleYProperty(), 1, Interpolator.EASE_OUT)),
                // destello de rayas brillantes
                kf(760, v(hatch.opacityProperty(), 0)),
                kf(775, v(hatch.opacityProperty(), 1)),
                kf(950, v(hatch.opacityProperty(), 0)),
                // la barra aparece de golpe (50 ms)
                kf(830, v(bar.opacityProperty(), 1, Interpolator.DISCRETE), v(barW, HIDDEN_W)),
                kf(880, v(barW, fullW)),
                // el texto aparece casi de golpe
                kf(860, v(info.opacityProperty(), 0)),
                kf(900, v(info.opacityProperty(), 1)));

        // ═══════════ PAUSA ═══════════
        // El texto se apaga ARC_LEAD ms después de que el arco empieza a ensancharse para despedirse
        final int ARC_LEAD = 950;
        final int T0 = 900;                                            // fin de la entrada
        double holdMs = Math.max(1000, cfg.holdSeconds * 1000);
        int waitMs = (int) Math.max(50, holdMs - ARC_LEAD);
        PauseTransition wait = new PauseTransition(Duration.millis(waitMs));
        final int tE = T0 + waitMs;                                    // aquí el arco empieza a despedirse

        // ═══════════ SALIDA (texto, barra y disco) ═══════════
        Timeline outro = new Timeline(
                // el texto se apaga
                kf(ARC_LEAD,        v(info.opacityProperty(), 1)),
                kf(ARC_LEAD + 150,  v(info.opacityProperty(), 0)),
                // la barra se recoge casi al instante
                kf(ARC_LEAD + 150,  v(barW, fullW)),
                kf(ARC_LEAD + 300,  v(barW, HIDDEN_W, Interpolator.EASE_IN), v(bar.opacityProperty(), 0, Interpolator.DISCRETE)),
                // el disco se desvanece
                kf(ARC_LEAD + 250,  v(disco.opacityProperty(), 1)),
                kf(ARC_LEAD + 450,  v(disco.opacityProperty(), 0)));

        // ═══════════ EL ARCO: una sola animación durante todo el popup ═══════════
        // 1) Entrada: nace a la derecha y crece hacia arriba/izquierda hasta formar el aro, y se apaga.
        // 2) Mientras el popup está completo: un tramo brillante recorre el aro sin parar, antihorario
        //    (~145°/s, ~45° de largo), como en el original.
        // 3) Despedida: sigue girando, se alarga y acelera; continúa solo cuando el disco ya se fue y se apaga.
        final double SPEED = 0.145;                                    // grados por ms
        final double LEN = 45;                                         // largo del tramo en órbita
        final double startAtE = 270;                                   // ángulo de inicio al empezar la despedida
        double startAtT0 = startAtE - SPEED * (tE - T0);

        Timeline arcTl = new Timeline(
                // 1) entrada
                kf(0,   v(arc.opacityProperty(), 0), v(arc.startAngleProperty(), 55), v(arc.lengthProperty(), -110)),
                kf(120, v(arc.opacityProperty(), 1)),
                kf(450, v(arc.startAngleProperty(), 125, Interpolator.EASE_OUT), v(arc.lengthProperty(), -205, Interpolator.EASE_OUT)),
                kf(650, v(arc.opacityProperty(), 0, Interpolator.EASE_IN)),
                // 2) órbita constante durante la pausa (interpolación lineal = velocidad constante)
                kf(T0,        v(arc.opacityProperty(), 0), v(arc.startAngleProperty(), startAtT0), v(arc.lengthProperty(), LEN)),
                kf(T0 + 150,  v(arc.opacityProperty(), 1)),
                kf(tE,        v(arc.opacityProperty(), 1, Interpolator.LINEAR),
                              v(arc.startAngleProperty(), startAtE), v(arc.lengthProperty(), LEN)),
                // 3) despedida (ángulos medidos del clip; el inicio siempre avanza hacia el mismo lado)
                kf(tE + 300,  v(arc.startAngleProperty(), 300), v(arc.lengthProperty(), 70)),
                kf(tE + 600,  v(arc.startAngleProperty(), 375), v(arc.lengthProperty(), 65)),
                kf(tE + 900,  v(arc.startAngleProperty(), 415), v(arc.lengthProperty(), 110)),
                kf(tE + 1400, v(arc.startAngleProperty(), 440), v(arc.lengthProperty(), 115)),
                kf(tE + 1800, v(arc.startAngleProperty(), 450), v(arc.lengthProperty(), 175)),
                kf(tE + 2000, v(arc.startAngleProperty(), 472), v(arc.lengthProperty(), 168), v(arc.opacityProperty(), 0.8)),
                kf(tE + 2200, v(arc.startAngleProperty(), 530), v(arc.lengthProperty(), 120), v(arc.opacityProperty(), 0.25)),
                kf(tE + 2300, v(arc.opacityProperty(), 0)));

        SequentialTransition seq = new SequentialTransition(intro, wait, outro);
        ParallelTransition all = new ParallelTransition(arcTl, seq);   // el popup se cierra cuando termina el arco
        all.setOnFinished(e -> {
            stage.close();
            if (activo == stage) activo = null;
        });
        animActual = all;

        stage.show();
        SoundManager.play(SoundManager.Sfx.TRAX); // opcional: solo suena si existe trax.wav
        all.play();
    }

    // ───────────────────────── helpers ─────────────────────────

    private static KeyFrame kf(double ms, KeyValue... values) {
        return new KeyFrame(Duration.millis(ms), values);
    }

    private static KeyValue v(javafx.beans.value.WritableValue<Number> p, double value) {
        return new KeyValue(p, value);
    }

    private static KeyValue v(javafx.beans.value.WritableValue<Number> p, double value, Interpolator i) {
        return new KeyValue(p, value, i);
    }

    private static Node buildBadgeContent(TraxSettings cfg) {
        if (notBlank(cfg.logoImage) && new File(cfg.logoImage).isFile()) {
            double d = D - 12;
            ImageView iv = new ImageView(new Image(new File(cfg.logoImage).toURI().toString(), d, d, false, true));
            iv.setFitWidth(d);
            iv.setFitHeight(d);
            iv.setClip(new Circle(d / 2, d / 2, d / 2));
            return iv;
        }
        String text = notBlank(cfg.badgeText) ? cfg.badgeText.trim() : "";
        Label l = new Label(text);
        l.getStyleClass().add("trax-logo-text");
        int n = text.length();
        l.setStyle("-fx-font-size: " + (n <= 2 ? 28 : n == 3 ? 22 : n == 4 ? 17 : 13) + "px; "
                + fontStyle(cfg.italicBadge));
        return l;
    }

    private static Label sub(String text, boolean italic) {
        Label l = new Label(text);
        l.getStyleClass().add("trax-sub");
        l.setStyle(fontStyle(italic));
        return l;
    }

    private static String fontStyle(boolean italic) {
        return "-fx-font-style: " + (italic ? "italic" : "normal") + ";";
    }

    private static void place(Stage stage, TraxSettings.Corner corner, double w, double h) {
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        boolean right = corner == TraxSettings.Corner.TOP_RIGHT || corner == TraxSettings.Corner.BOTTOM_RIGHT;
        boolean bottom = corner == TraxSettings.Corner.BOTTOM_LEFT || corner == TraxSettings.Corner.BOTTOM_RIGHT;
        stage.setX(right ? b.getMaxX() - w - MARGIN : b.getMinX() + MARGIN);
        stage.setY(bottom ? b.getMaxY() - h - MARGIN : b.getMinY() + MARGIN);
    }

    private static void fixSize(Region r, double w, double h) {
        r.setMinSize(w, h);
        r.setPrefSize(w, h);
        r.setMaxSize(w, h);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
