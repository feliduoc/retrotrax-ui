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
 * Popup estilo EA Trax.
 * Secuencia: círculo + aro → barra se despliega → canción / artista / álbum → se repliega
 * hasta quedar el lateral → el lateral se esconde → el aro vuelve a rodear el círculo → desaparece.
 */
public class TraxNotification {

    // ── Geometría (todo en píxeles sin escalar) ──
    private static final double PAD = 12;          // margen para la sombra
    private static final double LOGO = 66;         // diámetro del disco
    private static final double WRAP = 88;         // caja del disco + aro
    private static final double R_RING = 40;       // radio del aro exterior
    private static final double BAR_H = 54;
    private static final double OVERLAP = 14 + LOGO / 2; // tramo de la barra que queda detrás del disco
    private static final double INFO_X = OVERLAP + 12;
    private static final double INFO_MAX_W = 300;
    private static final double TAG_W = 30;
    private static final double HIDDEN_W = 28;     // ancho en el que la barra está totalmente tapada por el disco
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

        // ── Disco (texto o logo propio) ──
        StackPane disco = new StackPane(buildBadgeContent(cfg));
        disco.getStyleClass().add("trax-logo");
        fixSize(disco, LOGO, LOGO);

        // ── Aros que rodean el círculo ──
        Arc ringA = makeArc(R_RING, 2.8, "trax-ring-a");
        Arc ringB = makeArc(R_RING - 5, 1.2, "trax-ring-b");
        Pane ringLayer = new Pane(ringA, ringB);
        ringLayer.setMouseTransparent(true);
        ringLayer.setOpacity(0);

        Pane logoWrap = new Pane(disco, ringLayer);
        fixSize(logoWrap, WRAP, WRAP);
        disco.relocate((WRAP - LOGO) / 2, (WRAP - LOGO) / 2);

        // ── Texto: canción → artista → álbum ──
        VBox info = new VBox(1);
        Label title = new Label(song.getTitle());
        title.getStyleClass().add("trax-title");
        title.setStyle(fontStyle(cfg.italicBar));
        info.getChildren().add(title);
        if (notBlank(song.getArtist())) info.getChildren().add(sub(song.getArtist(), cfg.italicBar));
        if (notBlank(song.getAlbum()))  info.getChildren().add(sub(song.getAlbum(), cfg.italicBar));

        // ── Lateral (TRAX) girado 90° ──
        String tagText = notBlank(cfg.tagText) ? cfg.tagText.trim() : "";
        Label tag = new Label(tagText);
        tag.getStyleClass().add("trax-tag");
        tag.setStyle("-fx-font-size: " + Math.max(6, 11 - Math.max(0, tagText.length() - 4)) + "px; "
                + fontStyle(cfg.italicBar));
        tag.setRotate(90);
        StackPane side = new StackPane(new Group(tag));
        side.getStyleClass().add("trax-side");
        fixSize(side, TAG_W, BAR_H);
        side.setVisible(!tagText.isEmpty());

        // ── Barra (se maneja a mano: no es "managed", así controlamos su ancho cuadro a cuadro) ──
        Pane bar = new Pane(info, side);
        bar.getStyleClass().add("trax-bar");
        bar.setManaged(false);
        bar.setOpacity(0);

        // ── Raíz ──
        Pane root = new Pane(bar, logoWrap); // el disco va encima de la barra
        root.getStyleClass().add("trax-overlay");
        root.setStyle("-trax-accent: " + accent + "; -fx-font-family: '" + family.replace("'", "\\'") + "';");

        Scene scene = new Scene(root, 1, 1);
        scene.setFill(Color.TRANSPARENT);
        var css = TraxNotification.class.getResource("/cl/feliminish/ui/trax.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        root.applyCss(); // necesario para medir el texto con la fuente ya aplicada

        // ── Medidas ──
        double infoW = Math.min(INFO_MAX_W, info.prefWidth(-1));
        info.setMinWidth(infoW); info.setPrefWidth(infoW); info.setMaxWidth(infoW);
        double infoH = info.prefHeight(infoW);
        info.relocate(INFO_X, (BAR_H - infoH) / 2);

        double fullW = INFO_X + infoW + 16 + TAG_W;
        double endW = OVERLAP + TAG_W + 2; // ancho cuando solo queda el lateral junto al disco

        double cx = PAD + WRAP / 2;
        double barX = cx - 14;
        double barY = PAD + (WRAP - BAR_H) / 2;
        logoWrap.relocate(PAD, PAD);
        bar.relocate(barX, barY);

        double rootW = Math.max(PAD + WRAP, barX + fullW) + PAD;
        double rootH = WRAP + 2 * PAD;
        root.getTransforms().add(new Scale(scale, scale, 0, 0));

        // Ancho animable de la barra: redimensiona la barra, el recorte y la posición del lateral
        DoubleProperty barW = new SimpleDoubleProperty(HIDDEN_W);
        Rectangle clip = new Rectangle(HIDDEN_W, BAR_H);
        clip.setArcWidth(BAR_H);
        clip.setArcHeight(BAR_H);
        clip.widthProperty().bind(barW);
        bar.setClip(clip);
        side.layoutXProperty().bind(barW.subtract(TAG_W));
        barW.addListener((o, a, n) -> bar.resize(n.doubleValue(), BAR_H));
        bar.resize(HIDDEN_W, BAR_H);

        // ── Ventana ──
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        if (TraxApp.owner() != null) stage.initOwner(TraxApp.owner()); // dueño oculto: sin icono en la barra de tareas
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);
        stage.setWidth(rootW * scale);
        stage.setHeight(rootH * scale);
        place(stage, cfg.corner, rootW * scale, rootH * scale);
        activo = stage;

        // ── Estados iniciales ──
        disco.setScaleX(0);
        disco.setScaleY(0);
        for (Node n : info.getChildren()) n.setOpacity(0);

        // ── Secuencia ──
        // 1. nace el disco y el aro lo rodea
        ScaleTransition discoIn = new ScaleTransition(Duration.millis(350), disco);
        discoIn.setToX(1); discoIn.setToY(1);
        discoIn.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition intro = new ParallelTransition(discoIn, ringSweep(ringA, ringB, ringLayer));

        // 2. la barra se despliega y el texto entra en cascada
        FadeTransition barIn = new FadeTransition(Duration.millis(120), bar);
        barIn.setToValue(1);
        Timeline expand = new Timeline(new KeyFrame(Duration.millis(550),
                new KeyValue(barW, fullW, Interpolator.EASE_BOTH)));
        SequentialTransition textDelayed = new SequentialTransition(
                new PauseTransition(Duration.millis(320)), textCascade(info));
        ParallelTransition open = new ParallelTransition(barIn, expand, textDelayed);

        // 3. pausa con la info visible
        PauseTransition hold = new PauseTransition(Duration.seconds(Math.max(1, cfg.holdSeconds)));

        // 4. el texto se apaga y la barra se repliega hasta dejar solo el lateral
        FadeTransition infoOut = new FadeTransition(Duration.millis(180), info);
        infoOut.setToValue(0);
        Timeline toTag = new Timeline(new KeyFrame(Duration.millis(380),
                new KeyValue(barW, endW, Interpolator.EASE_BOTH)));

        // 5. el lateral se mete detrás del disco
        FadeTransition barOut = new FadeTransition(Duration.millis(260), bar);
        barOut.setToValue(0);
        Timeline hide = new Timeline(new KeyFrame(Duration.millis(260),
                new KeyValue(barW, HIDDEN_W, Interpolator.EASE_BOTH)));

        // 6. el disco desaparece y el aro vuelve a girar
        FadeTransition discoFade = new FadeTransition(Duration.millis(260), disco);
        discoFade.setToValue(0);
        ScaleTransition discoOut = new ScaleTransition(Duration.millis(260), disco);
        discoOut.setToX(0.6); discoOut.setToY(0.6);
        ParallelTransition outro = new ParallelTransition(discoFade, discoOut, ringSweep(ringA, ringB, ringLayer));

        SequentialTransition seq = new SequentialTransition(
                intro, open, hold, infoOut, toTag,
                new PauseTransition(Duration.millis(150)),
                new ParallelTransition(barOut, hide),
                outro);
        seq.setOnFinished(e -> {
            stage.close();
            if (activo == stage) activo = null;
        });
        animActual = seq;

        stage.show();
        seq.play();
    }

    // ───────────────────────── helpers ─────────────────────────

    private static Node buildBadgeContent(TraxSettings cfg) {
        if (notBlank(cfg.logoImage) && new File(cfg.logoImage).isFile()) {
            double d = LOGO - 10;
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
        l.setStyle("-fx-font-size: " + (n <= 2 ? 24 : n == 3 ? 19 : n == 4 ? 15 : 12) + "px; "
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

    private static Arc makeArc(double r, double width, String styleClass) {
        Arc a = new Arc(WRAP / 2, WRAP / 2, r, r, 90, 0);
        a.setType(ArcType.OPEN);
        a.setFill(null);
        a.setStrokeWidth(width);
        a.setStrokeLineCap(StrokeLineCap.ROUND);
        a.getStyleClass().add(styleClass);
        return a;
    }

    /** Dos arcos que giran en sentidos opuestos, crecen alrededor del círculo y se desvanecen. */
    private static Timeline ringSweep(Arc a, Arc b, Node layer) {
        return new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(layer.opacityProperty(), 1),
                        new KeyValue(a.lengthProperty(), 0), new KeyValue(a.startAngleProperty(), 90),
                        new KeyValue(b.lengthProperty(), 0), new KeyValue(b.startAngleProperty(), 90)),
                new KeyFrame(Duration.millis(650),
                        new KeyValue(a.lengthProperty(), 300, Interpolator.EASE_OUT),
                        new KeyValue(a.startAngleProperty(), 450, Interpolator.EASE_BOTH),
                        new KeyValue(b.lengthProperty(), 220, Interpolator.EASE_OUT),
                        new KeyValue(b.startAngleProperty(), -270, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(900), new KeyValue(layer.opacityProperty(), 0)));
    }

    private static Timeline textCascade(VBox info) {
        Timeline t = new Timeline();
        int i = 0;
        for (Node n : info.getChildren()) {
            n.setTranslateX(-14);
            Duration start = Duration.millis(140 * i++);
            t.getKeyFrames().addAll(
                    new KeyFrame(start,
                            new KeyValue(n.opacityProperty(), 0), new KeyValue(n.translateXProperty(), -14)),
                    new KeyFrame(start.add(Duration.millis(260)),
                            new KeyValue(n.opacityProperty(), 1, Interpolator.EASE_OUT),
                            new KeyValue(n.translateXProperty(), 0, Interpolator.EASE_OUT)));
        }
        return t;
    }

    private static void place(Stage stage, TraxSettings.Corner corner, double w, double h) {
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        boolean right = corner == TraxSettings.Corner.TOP_RIGHT || corner == TraxSettings.Corner.BOTTOM_RIGHT;
        boolean bottom = corner == TraxSettings.Corner.BOTTOM_LEFT || corner == TraxSettings.Corner.BOTTOM_RIGHT;
        stage.setX(right ? b.getMaxX() - w - MARGIN : b.getMinX() + MARGIN);
        stage.setY(bottom ? b.getMaxY() - h - MARGIN : b.getMinY() + MARGIN);
    }

    private static void fixSize(javafx.scene.layout.Region r, double w, double h) {
        r.setMinSize(w, h);
        r.setPrefSize(w, h);
        r.setMaxSize(w, h);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
