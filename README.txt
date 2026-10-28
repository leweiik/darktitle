Leweiik Title (Fabric, Minecraft 1.21.11, client-only)

Pixel-art fire title screen: layered flames (yellow core, orange body, red tips), fire walls
on the left and right edges, rising embers, a "Press to Enter" start, and your own stickers.

IN-GAME SETTINGS
  Main menu -> "Fire Settings" (top-left). Changes show live behind the settings screen.
  Fire speed, fire height, side fire, glow, embers, ember speed, pixel detail, sticker size,
  and a switch for the Press to Enter screen. Saved in config/leweiiktitle.properties.

STICKERS (your own images)
  Put PNG files in:  <game folder>/config/leweiiktitle/
    left.png   -> shown on the left
    right.png  -> shown on the right (if missing, left.png is mirrored)
  The folder is created the first time you launch the game. Changes are picked up
  within a few seconds, no restart needed. Transparent PNGs work best.

BUILD
  Push to GitHub and download the "darktitle-jar" artifact from the Actions tab
  (the jar inside is named leweiiktitle-1.1.0.jar), or run  ./gradlew build  locally (JDK 21).
