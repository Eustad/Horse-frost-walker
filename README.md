# Horse Frost Walker

Si vas montado a caballo (también burro, mula, llama, camello, caballo esqueleto/zombi)
y llevas botas con **Paso helado**, el caballo congela el agua bajo sus patas igual que
lo harías tú a pie. Radio según el nivel del encantamiento, como en vanilla.

Extra: mientras lo montas con esas botas, el caballo no recibe daño de los bloques de magma.

## Proyectos y compatibilidad

| Carpeta | Minecraft | Loaders compatibles | Java |
|---|---|---|---|
| `neoforge-1.21.x` | 1.21 y 1.21.1 | NeoForge 21.0.143+ y cualquier 21.1.x | 21 |
| `forge-1.20.1` | 1.20.1        | Forge 47.x y NeoForge 1.20.1 (47.1.x) | 17 |

Es un mod solo de servidor: los jugadores que entren sin el mod instalado pueden conectarse
igualmente. En un solo jugador, basta con ponerlo en tu carpeta `mods`.

## Compilar

Entra en la carpeta del loader que quieras y ejecuta:

```
./gradlew build        (Linux/macOS)
gradlew.bat build      (Windows)
```

El `.jar` final queda en `build/libs/`. Cópialo a la carpeta `mods` de tu instancia.
Para probar en desarrollo: `./gradlew runClient`.

## Descargar los .jar ya compilados

Cada vez que se sube un cambio, GitHub Actions compila ambas versiones.
Ve a la pestaña **Actions** del repositorio, abre la última ejecución de **Build**
y descarga los `.jar` en la sección **Artifacts**.
