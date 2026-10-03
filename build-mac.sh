#!/bin/sh
# Genera dist/RetroTrax.app. Hay que ejecutarlo en un Mac (el jar lleva las librerías de JavaFX de macOS).
set -e
mvn -q clean package
rm -rf dist build-input && mkdir build-input
cp target/ea-retrotrax-ui-1.1.jar build-input/
jpackage --type app-image --name RetroTrax --input build-input \
  --main-jar ea-retrotrax-ui-1.1.jar --main-class cl.feliminish.Main \
  --dest dist --java-options "-Dfile.encoding=UTF-8"
rm -rf build-input
echo "Listo: dist/RetroTrax.app"
