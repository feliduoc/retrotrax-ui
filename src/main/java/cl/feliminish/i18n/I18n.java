package cl.feliminish.i18n;

import java.util.HashMap;
import java.util.Map;

/**
 * Textos de la interfaz en español (por defecto), inglés y portugués.
 * Para agregar un idioma: sumar un valor a {@link Lang} y una traducción más en cada línea de la tabla.
 */
public final class I18n {

    public enum Lang {
        ES("es", "Español"), EN("en", "English"), PT("pt", "Português");

        public final String code;
        public final String label;

        Lang(String code, String label) { this.code = code; this.label = label; }

        public static Lang fromCode(String code) {
            if (code != null) {
                for (Lang l : values()) if (l.code.equalsIgnoreCase(code.trim())) return l;
            }
            return ES;
        }
    }

    private static Lang current = Lang.ES;
    private static final Map<String, String[]> TABLE = new HashMap<>();

    private I18n() {}

    /** key → {español, inglés, portugués} */
    private static void p(String key, String es, String en, String pt) {
        TABLE.put(key, new String[]{es, en, pt});
    }

    static {
        // Ventana
        p("title", "retro/ajustes", "retro/settings", "retro/ajustes");
        p("window.title", "Retro Trax: ajustes", "Retro Trax: settings", "Retro Trax: ajustes");

        // Etiquetas de fila
        p("lbl.language", "Idioma", "Language", "Idioma");
        p("lbl.badge", "Texto del círculo", "Circle text", "Texto do círculo");
        p("lbl.tag", "Texto lateral", "Side text", "Texto lateral");
        p("lbl.logo", "Logo propio", "Custom logo", "Logo próprio");
        p("lbl.font", "Fuente", "Font", "Fonte");
        p("lbl.fontfile", "Fuente propia", "Custom font", "Fonte própria");
        p("lbl.color", "Color", "Color", "Cor");
        p("lbl.barstyle", "Fondo de la barra", "Bar background", "Fundo da barra");
        p("lbl.barcolor", "Color de la barra", "Bar color", "Cor da barra");
        p("lbl.baropacity", "Opacidad de la barra", "Bar opacity", "Opacidade da barra");
        p("lbl.discstyle", "Fondo del círculo", "Circle background", "Fundo do círculo");
        p("lbl.disccolor", "Color del círculo", "Circle color", "Cor do círculo");
        p("lbl.discopacity", "Opacidad del círculo", "Circle opacity", "Opacidade do círculo");
        p("lbl.hold", "Segundos abierta", "Seconds visible", "Segundos visível");
        p("lbl.scale", "Tamaño", "Size", "Tamanho");
        p("lbl.position", "Posición", "Position", "Posição");
        p("lbl.style", "Estilo", "Style", "Estilo");
        p("lbl.sound", "Sonido", "Sound", "Som");

        // Botones
        p("btn.pickimage", "Elegir imagen…", "Choose image…", "Escolher imagem…");
        p("btn.loadfont", "Cargar .ttf…", "Load .ttf…", "Carregar .ttf…");
        p("btn.remove", "Quitar", "Remove", "Remover");
        p("btn.opensounds", "Abrir carpeta de sonidos", "Open sounds folder", "Abrir pasta de sons");
        p("btn.test", "PROBAR", "TEST", "TESTAR");
        p("btn.save", "GUARDAR", "SAVE", "SALVAR");
        p("btn.quit", "SALIR", "EXIT", "SAIR");

        // Casillas
        p("chk.italicbadge", "Cursiva en el círculo", "Italic in the circle", "Itálico no círculo");
        p("chk.italicbar", "Cursiva en la barra", "Italic in the bar", "Itálico na barra");
        p("chk.enabled", "Mostrar notificaciones", "Show notifications", "Mostrar notificações");
        p("chk.startup", "Iniciar con Windows", "Start with Windows", "Iniciar com o Windows");
        p("chk.sound", "Sonidos de la interfaz", "Interface sounds", "Sons da interface");

        // Textos de ayuda
        p("hint.startup", "(solo funciona desde el .exe)", "(only works from the .exe)", "(só funciona a partir do .exe)");
        p("hint.sounds",
                "Coloca tus WAV (click, hover, slider, select, open, close) en esta carpeta",
                "Put your WAV files (click, hover, slider, select, open, close) in this folder",
                "Coloque seus WAV (click, hover, slider, select, open, close) nesta pasta");
        p("hint.none", "(ninguna)", "(none)", "(nenhuma)");
        p("hint.usetext", "(usa el texto del círculo)", "(uses the circle text)", "(usa o texto do círculo)");

        // Esquinas
        p("corner.TOP_LEFT", "Arriba izquierda", "Top left", "Superior esquerdo");
        p("corner.TOP_RIGHT", "Arriba derecha", "Top right", "Superior direito");
        p("corner.BOTTOM_LEFT", "Abajo izquierda", "Bottom left", "Inferior esquerdo");
        p("corner.BOTTOM_RIGHT", "Abajo derecha", "Bottom right", "Inferior direito");

        // Estilos de fondo
        p("bar.classic", "Clásico", "Classic", "Clássico");
        p("bar.aero", "Aero", "Aero", "Aero");

        // Mensajes
        p("status.saved", "Guardado", "Saved", "Salvo");
        p("status.savedNoStartup",
                "Guardado, pero no se pudo cambiar el inicio con Windows",
                "Saved, but the Windows startup setting could not be changed",
                "Salvo, mas não foi possível alterar a inicialização com o Windows");

        // Diálogos de archivo
        p("dlg.chooselogo", "Elegir logo", "Choose logo", "Escolher logo");
        p("dlg.choosefont", "Elegir fuente", "Choose font", "Escolher fonte");
        p("dlg.images", "Imágenes", "Images", "Imagens");
        p("dlg.fonts", "Fuentes", "Fonts", "Fontes");

        // Canción de prueba (botón PROBAR)
        p("test.title", "Nombre canción", "Song title", "Nome da música");
        p("test.artist", "Artista", "Artist", "Artista");
        p("test.album", "Álbum", "Album", "Álbum");

        // Bandeja del sistema
        p("tray.settings", "Ajustes", "Settings", "Configurações");
        p("tray.exit", "Salir", "Exit", "Sair");
    }

    public static Lang lang() { return current; }

    public static void setLang(Lang lang) { current = lang == null ? Lang.ES : lang; }

    /** Texto de la clave en el idioma actual; si falta la traducción usa el español, y si falta la clave, la clave. */
    public static String t(String key) {
        String[] v = TABLE.get(key);
        if (v == null) return key;
        String s = v[current.ordinal()];
        return (s == null || s.isEmpty()) ? v[0] : s;
    }

    /** Solo para pruebas: claves registradas. */
    static Map<String, String[]> table() { return TABLE; }
}
