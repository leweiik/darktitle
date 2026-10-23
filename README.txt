Dark Title (Fabric, Minecraft 1.21.11, client-only)

1. Install JDK 21 and Gradle (or copy gradlew, gradlew.bat and the gradle/ folder
   from a template made at https://fabricmc.net/develop/template).
2. If you have no wrapper yet:   gradle wrapper --gradle-version 9.2.1
3. Test in-game:                 ./gradlew runClient
4. Build the jar:                ./gradlew build   ->  build/libs/darktitle-1.0.0.jar
   (use the jar WITHOUT "-sources" in its name)

Tweak the look in src/main/java/com/example/darktitle/DarkTitleRenderer.java
(DARKNESS, PARTICLES, DRIFT).
