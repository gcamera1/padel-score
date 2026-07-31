#!/usr/bin/env bash
#
# Genera las capturas de la ficha de Google Play para el form factor Wear OS.
#
#   ./scripts/wear-store-screenshots.sh
#
# Por qué existe: los PNG de Paparazzi salen con las esquinas transparentes
# (recorte de pantalla redonda) y Play rechaza capturas con canal alfa
# (WO-G5: "no transparent backgrounds"). Este script graba los snapshots y los
# aplana sobre negro — que además es el fondo que pide WO-V13.
#
# Salida: release-artifacts/store-assets/wear/ — PNG 900x900 sin alfa, junto a
# los assets del teléfono que ya usa la ficha.

set -euo pipefail

cd "$(dirname "$0")/.."

SNAPSHOTS="wear/src/test/snapshots/images"
OUT="release-artifacts/store-assets/wear"
PKG="com.gonzalocamera.padelcounter.presentation"

# Solo las capturas del Pixel Watch (900x900) van a la ficha; las del
# Galaxy Watch 40mm son regresión visual del reloj chico.
#   <nombre del test> : <nombre del archivo para Play>
SHOTS=(
  "CounterScreenshot_PixelWatch_inGame:reloj-1.png"
  "CounterScreenshot_PixelWatch_starPointDecider:reloj-2.png"
  "CounterScreenshot_PixelWatch_tieBreak:reloj-3.png"
  "CounterScreenshot_PixelWatch_matchPoint:reloj-4.png"
)

echo "==> Grabando snapshots de Paparazzi"
./gradlew :wear:recordPaparazziDebug

FLATTEN=$(mktemp -d)/Flatten.java
cat > "$FLATTEN" <<'JAVA'
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Aplana un PNG con alfa sobre fondo negro y lo escribe sin canal alfa. */
public class Flatten {
    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        BufferedImage out = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        ImageIO.write(out, "png", new File(args[1]));
        System.out.printf("    %s  %dx%d%n", new File(args[1]).getName(),
                out.getWidth(), out.getHeight());
    }
}
JAVA

echo "==> Aplanando sobre negro -> $OUT"
mkdir -p "$OUT"
for shot in "${SHOTS[@]}"; do
  test_name="${shot%%:*}"
  out_name="${shot##*:}"
  src="$SNAPSHOTS/${PKG}_${test_name}.png"

  if [[ ! -f "$src" ]]; then
    echo "ERROR: no se generó $src" >&2
    echo "       ¿Cambió el nombre del test en CounterScreenshotTest.kt?" >&2
    exit 1
  fi

  java "$FLATTEN" "$src" "$OUT/$out_name"
done

echo
echo "Listo. $(ls -1 "$OUT" | wc -l | tr -d ' ') capturas en $OUT"
echo "Requisitos de Play para Wear OS: PNG/JPEG sin alfa, 1:1, 384-3840px, hasta 8 capturas."
