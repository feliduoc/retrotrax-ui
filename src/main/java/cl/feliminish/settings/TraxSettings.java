package cl.feliminish.settings;

/** Todo lo que el usuario puede personalizar. Se guardara como JSON en ~/.retrotrax/settings.json */
public class TraxSettings {

    public enum Corner {
        TOP_LEFT("Arriba izquierda"), TOP_RIGHT("Arriba derecha"),
        BOTTOM_LEFT("Abajo izquierda"), BOTTOM_RIGHT("Abajo derecha");

        private final String label;
        Corner(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public String badgeText = "RT";          // texto dentro del círculo
    public String tagText = "NOW";           // texto vertical del lateral
    public String fontFamily = "Michroma";   // fuente instalada o la incluida
    public String fontFile = "";             // ruta a un .ttf/.otf propio (tiene prioridad)
    public String accentColor = "#c4943c";   // dorado de bordes, aro y lateral
    public String logoImage = "";            // imagen propia; si existe reemplaza al texto del círculo
    public boolean italicBadge = true;       // cursiva en el texto del círculo
    public boolean italicBar = false;        // cursiva en el texto de la barra (canción, artista, álbum y lateral)
    public double holdSeconds = 4.2;         // cuánto se queda abierta la barra
    public double scale = 1.0;
    public Corner corner = Corner.TOP_LEFT;
    public boolean enabled = true;
    public boolean soundEnabled = true;      // sonidos de la ventana de ajustes
    public double soundVolume = 0.7;         // 0.0 a 1.0
}
