#!/bin/sh
# Genera dist/RetroTrax (carpeta ejecutable) y RetroTrax-linux.tar.gz.
#   ./build-linux.sh        → carpeta + tar.gz
#   ./build-linux.sh deb    → además crea un paquete .deb (necesita: sudo apt install fakeroot dpkg-dev)
# Requisitos: JDK 21 completo (con jpackage) y Maven. Hay que ejecutarlo en Linux.
set -e

if command -v mvn >/dev/null 2>&1; then
  mvn -q clean package
else
  echo "No encontré Maven: uso el jar que ya está en target/"
fi

JAR=$(ls target/ea-retrotrax-ui-*.jar 2>/dev/null | grep -v original | head -n 1)
if [ -z "$JAR" ]; then
  echo "Falta el jar en target/. Compila primero con: mvn clean package"
  exit 1
fi
NAME=$(basename "$JAR")
VERSION=${NAME#ea-retrotrax-ui-}
VERSION=${VERSION%.jar}

rm -rf dist build-input
mkdir build-input
cp "$JAR" build-input/

ICON=""
[ -f icon.png ] && ICON="--icon icon.png"

# 1) Carpeta ejecutable (no necesita Java instalado): dist/RetroTrax/bin/RetroTrax
jpackage --type app-image --name RetroTrax --input build-input \
  --main-jar "$NAME" --main-class cl.feliminish.Main \
  --dest dist --java-options "-Dfile.encoding=UTF-8" $ICON

tar -czf RetroTrax-linux.tar.gz -C dist RetroTrax
echo "Listo: dist/RetroTrax/bin/RetroTrax  y  RetroTrax-linux.tar.gz"

# 2) Opcional: paquete .deb con acceso directo en el menú de aplicaciones
if [ "$1" = "deb" ]; then
  jpackage --type deb --name RetroTrax --linux-package-name retro-trax \
    --app-version "$VERSION" --linux-shortcut --linux-menu-group "AudioVideo" \
    --input build-input --main-jar "$NAME" --main-class cl.feliminish.Main \
    --dest dist --java-options "-Dfile.encoding=UTF-8" $ICON
  echo "Listo: $(ls dist/*.deb)"
fi

rm -rf build-input
