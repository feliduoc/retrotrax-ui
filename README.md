# Retro Trax
<img width="866" height="236" alt="retrotrax" src="https://github.com/user-attachments/assets/94d34904-caae-4a0f-ad87-8eee3718439b" />

# ¿que es retrotrax?
es un proyecto universitario personal :D
Un popup animado que muestra la canción que estás escuchando (título, artista y álbum),
inspirado en las notificaciones de música de los juegos de carreras de principios de los 2000.
Hecho con JavaFX.

> Proyecto de aficionados, sin relación con Electronic Arts. Ver [THIRD_PARTY.md](THIRD_PARTY.md).
> se uso la ayuda de la IA para la creacion de esta app.

## Qué hace

- Muestra un popup con círculo, aro animado y barra que se despliega con la canción, el artista y el álbum.
- Lee lo que suena en **Spotify, Apple Music y otros reproductores** (Windows y macOS).
- Todo se personaliza desde una ventana de ajustes:
  - texto del círculo (o una imagen tuya) y texto lateral,
  - fuente instalada o un archivo `.ttf`/`.otf` propio, con opción de cursiva,
  - color, tamaño, tiempo en pantalla y esquina donde aparece,
  - sonidos de la interfaz (los pones tú, ver más abajo),
  - iniciar con Windows.
- En Windows queda en la bandeja del sistema, junto al reloj.


# demostracion visual de retrotrax

- menu de ajustes
  <img width="767" height="890" alt="image" src="https://github.com/user-attachments/assets/20ce4618-d718-4005-acbe-f75f461a8928" />


- en apple music
<img width="300" height="83" alt="image" src="https://github.com/user-attachments/assets/82492bbf-5f40-4c5f-b631-3079e39809a8" />


## Descargar y usar (Windows)

1. Entra a la sección **Releases** y descarga `RetroTrax-X.X-windows.zip`.
2. Descomprime **toda** la carpeta (el `.exe` no funciona suelto).
3. Abre `RetroTrax.exe`.

No necesitas tener Java instalado.

**Aviso de Windows:** como el ejecutable no está firmado, SmartScreen puede mostrar
"Windows protegió su PC". Pulsa *Más información → Ejecutar de todas formas*.
Algunos antivirus también desconfían de programas nuevos sin firmar.

### macOS

No hay descarga lista: hay que compilarlo en un Mac (ver abajo). La primera vez, macOS pedirá
permiso de *Automatización* para consultar Spotify y Music.

## Sonidos

La app **no incluye sonidos**. Para activarlos, copia tus propios archivos WAV a:

```
Windows:  C:\Users\<tu usuario>\.retrotrax\sounds\
macOS:    ~/.retrotrax/sounds/
```

Nombres reconocidos (todos opcionales): `click.wav`, `hover.wav`, `slider.wav`, `select.wav`,
`open.wav`, `close.wav` y `trax.wav`. Si falta uno, simplemente no suena (`select` usa `click` de respaldo).
Formato recomendado: WAV PCM de 16 bits, 44.1 o 48 kHz. En los ajustes hay un botón que abre esa carpeta.

## Ajustes

Se guardan en `~/.retrotrax/settings.json`. Puedes editarlos desde la ventana de ajustes
(en Windows también desde el menú del icono de la bandeja).

## Compilar desde el código

Requisitos: **JDK 21 completo** (incluye `jpackage`) y Maven, o el panel Maven de IntelliJ.

```bash
mvn clean package        # genera target/ea-retrotrax-ui-1.1.jar con todo incluido
mvn javafx:run           # ejecutar sin empaquetar
```

Crear el ejecutable (se compila en el mismo sistema operativo donde se va a usar):

- Windows: `build-windows.bat` → `dist\RetroTrax\RetroTrax.exe`
  (si existe un `icon.ico` junto al script, se usa como icono).
- macOS: `./build-mac.sh` → `dist/RetroTrax.app`

Si `jpackage` no se reconoce, agrega la carpeta `bin` de tu JDK al PATH.

## Cómo lee la canción

- **Windows:** un script de PowerShell lee el control de medios del sistema (el mismo que aparece
  al subir el volumen), así que funciona con cualquier reproductor que se registre ahí.
  Algunos reproductores no entregan el álbum.
- **macOS:** consulta Spotify y Music con AppleScript cada 2 segundos.
- **Linux:** todavía no está soportado.

## terceros

Componentes de terceros en [THIRD_PARTY.md](THIRD_PARTY.md).
