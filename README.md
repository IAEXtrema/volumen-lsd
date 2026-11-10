# PsychoVolume

Controlador de volumen psicodélico para Android. Al subir o bajar el volumen multimedia (botones físicos, panel del sistema o el slider de la app), un pequeño universo dentro de una cápsula de cristal se transforma en tiempo real: criaturas que despiertan, honguitos que bailan, un ajolote que nada, gatitos DJ, extraterrestres, arcoíris líquidos y un túnel caleidoscópico.

> Estado: proyecto completo, sin compilar localmente (el entorno donde se generó no tenía Android SDK). La compilación se hace con el workflow de GitHub Actions incluido. Si el primer build falla, copia el error del log de Actions y corrígelo a partir de ahí.

---

## 1. Arquitectura técnica

| Capa | Tecnología | Por qué |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Declarativa, sin XML de layouts, buena integración con Canvas |
| Fondo | **AGSL `RuntimeShader`** (GPU) | Plasma, espirales y túnel caleidoscópico por píxel a costo casi cero en CPU. Disponible desde Android 13, por eso `minSdk = 33` |
| Personajes | Compose `Canvas` / `DrawScope` con `Path` vectoriales | Personajes originales dibujados por código (sin archivos externos), escalables a cualquier resolución |
| Partículas | Arrays planos + pool fijo (360) | Cero asignaciones por fotograma → sin pausas del GC |
| Estado | MVVM: `MainViewModel` + `StateFlow` | Sobrevive a rotación y pantalla completa |
| Volumen | `AudioManager` + `ContentObserver` + broadcast `VOLUME_CHANGED_ACTION` | Detecta botones físicos, panel del sistema y otras apps |
| Ajustes | DataStore Preferences | Persistencia asíncrona y segura |
| Flotante | `LifecycleService` en primer plano + `TYPE_APPLICATION_OVERLAY` + `ComposeView` | La misma escena Compose dentro de una ventana sobre otras apps |

### Cómo se anima

Todo el universo es una **función continua** de dos valores:

- `level`: el volumen normalizado (0.0–1.0) pasado por un resorte exponencial (`level += (objetivo − level)·(1 − e^(−dt·k))`). La constante `k` es la *Sensibilidad visual* de ajustes.
- `animT`: tiempo de animación que avanza más rápido con más volumen.

Cada elemento calcula su estado con `smoothstep` sobre `level`, así que no hay escenas discretas: **bajar el volumen recorre exactamente el mismo camino de regreso**, sin reinicios.

| Fase | Rango | Qué ocurre |
|---|---|---|
| 1 · Sueño estelar | 0–20 % | Cielo nocturno desaturado, nubes y hongos dormidos con "Z", lengua en reposo, pocas partículas |
| 2 · Despertar | 20–40 % | La lengua sale y se mueve, aparece el papelito ilustrado sobre ella, espirales en el fondo, los ojos se abren |
| 3 · Baile de hongos | 40–60 % | Los hongos bailan (squash & stretch), aparece el ajolote, arcoíris líquido, gotitas saltarinas, geometría girando, el papel salta |
| 4 · Universo fantástico | 60–80 % | Gatitos DJ con tornamesa, primer OVNI, la geometría se multiplica, lunares en las colinas, empieza el caleidoscopio |
| 5 · Explosión cósmica | 80–100 % | Túnel caleidoscópico, tercer anillo de geometría sagrada, ojomedusa, segundo OVNI con rayo, el papel orbita la boca, estallido de partículas, destellos |

### Rendimiento

- Bucle con `withFrameNanos`; cada fotograma solo invalida la fase de **dibujo** (no hay recomposición).
- El bucle se pausa fuera de `Lifecycle.State.STARTED` (pantalla apagada / app en segundo plano).
- `Path` reutilizados con un pool por fotograma.
- **Calidad adaptativa**: si el promedio de fotograma supera el presupuesto, baja la calidad (menos partículas, se omiten capas secundarias) y la recupera cuando hay margen.
- **Ahorro de batería**: ~30 FPS y menos partículas.

---

## 2. Estructura

```
PsychoVolume/
├── .github/workflows/build-apk.yml     ← compila el APK en GitHub
├── build.gradle.kts / settings.gradle.kts / gradle.properties
├── gradle/libs.versions.toml           ← versiones centralizadas
├── gradle/wrapper/                     ← Gradle 8.11.1
├── gradlew / gradlew.bat
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/                         ← icono adaptativo vectorial, tema, textos
        └── java/com/psychovolume/app/
            ├── PsychoVolumeApp.kt
            ├── MainActivity.kt
            ├── data/
            │   ├── VolumeRepository.kt  ← lectura/observación/escritura del volumen
            │   └── SettingsRepository.kt
            ├── engine/
            │   ├── PsychoEngine.kt      ← orquesta capas, resorte, calidad adaptativa
            │   ├── PsychoScene.kt       ← Composable con el bucle de fotogramas
            │   ├── BackgroundShader.kt  ← shader AGSL
            │   ├── Characters.kt        ← boca, lengua, papel, ajolote, hongos, gatos DJ, OVNIs, ojomedusa, gotitas, nubes
            │   ├── Scenery.kt           ← estrellas, geometría sagrada, arcoíris, colinas
            │   ├── Particles.kt
            │   ├── Palettes.kt / RenderParams.kt / MathUtil.kt
            ├── ui/
            │   ├── MainScreen.kt  ← cápsula, slider, fases, demo, pantalla completa
            │   ├── SettingsScreen.kt
            │   ├── GlassCapsule.kt
            │   ├── MainViewModel.kt
            │   └── theme/Theme.kt
            └── overlay/
                ├── FloatingCapsuleService.kt
                ├── FloatingCapsuleContent.kt
                └── FloatingController.kt
```

### Versiones (compatibles entre sí)

| Componente | Versión |
|---|---|
| Gradle | 8.11.1 |
| Android Gradle Plugin | 8.7.3 (requiere Gradle ≥ 8.9 y JDK 17) |
| Kotlin + plugin Compose Compiler | 2.0.21 |
| Compose BOM | 2024.12.01 |
| compileSdk / targetSdk | 35 |
| minSdk | 33 (Android 13) — funciona en 13, 14, 15 y 16 |

---

## 3. Compilar con GitHub Actions (sin Android Studio)

1. Crea un repositorio vacío en GitHub (por ejemplo `PsychoVolume`).
2. Descomprime `PsychoVolume.zip` en tu computadora.
3. Sube el **contenido** de la carpeta `PsychoVolume` a la raíz del repositorio:
   - **Opción web:** en el repositorio, *Add file → Upload files* y arrastra todos los archivos y carpetas (Chrome/Edge conservan la estructura de carpetas). En macOS la carpeta `.github` está oculta en Finder: pulsa `Cmd + Shift + .` para verla y asegúrate de subirla.
   - **Opción git:**
     ```bash
     cd PsychoVolume
     git init
     git add .
     git commit -m "PsychoVolume inicial"
     git branch -M main
     git remote add origin https://github.com/TU_USUARIO/PsychoVolume.git
     git push -u origin main
     ```
4. Ve a la pestaña **Actions**. El workflow *Build debug APK* se ejecuta solo con cada push a `main` (o manualmente con *Run workflow*).
5. Al terminar (≈ 5–8 min la primera vez), entra a la ejecución y descarga el artefacto **PsychoVolume-debug-apk**. Dentro está `app-debug.apk`.
6. En el teléfono, abre el APK y permite *Instalar apps desconocidas* para tu navegador o gestor de archivos.

Para abrirlo en Android Studio: *File → Open* y selecciona la carpeta `PsychoVolume`.

---

## 4. Uso

- **Botones físicos**: mientras la app está visible, ajustan el volumen multimedia y la escena reacciona al instante.
- **Slider** y botones **– / +**: cambian el volumen real (sincronización bidireccional).
- **Tocar la cápsula**: oculta o muestra los controles.
- **Pantalla completa** (icono de esquinas): modo inmersivo horizontal; toca para mostrar los controles; atrás para salir.
- **Demo manual / Demo auto**: recorren las fases sin tocar el volumen real.

### Ajustes

Intensidad de efectos · Velocidad · Partículas · Sensibilidad visual · Paleta (4) · Reducir movimiento intenso · Destellos · Ahorro de batería · Tamaño · Transparencia · Controlador flotante.

**Accesibilidad:** *Reducir movimiento intenso* desactiva destellos, suaviza el caleidoscopio y baja la velocidad. Se activa automáticamente si en el sistema las animaciones están desactivadas (Opciones de desarrollador → escala de animación = 0). Los destellos de la fase 5 siempre pulsan a 1.2 Hz, por debajo del umbral de 3 por segundo de WCAG.

---

## 5. Controlador flotante: qué se puede y qué no

**Sí:** una cápsula animada sobre cualquier app, que se arrastra, se expande al tocarla (slider, abrir app, cerrar) y reacciona a cualquier cambio de volumen. Usa el permiso *Mostrar sobre otras apps* (`SYSTEM_ALERT_WINDOW`) y un servicio en primer plano de tipo `specialUse` con su notificación obligatoria (incluye botón *Detener*).

**No:** Android no permite que una app normal **reemplace ni oculte el panel de volumen del sistema**. Al pulsar los botones físicos seguirá apareciendo el panel nativo junto a la cápsula. Esto requeriría ser app de sistema o un servicio de accesibilidad que intercepte teclas, lo cual Google restringe para este uso.

Otras notas:
- El servicio se inicia desde la app en primer plano, como exige Android 12+.
- Con *Mostrar sobre otras apps* activo, los botones físicos controlan el flujo que el sistema decida según la app en primer plano (normalmente multimedia).
- Algunos fabricantes (Xiaomi, Huawei, ciertos Samsung) cierran servicios agresivamente; si la cápsula desaparece, excluye la app de la optimización de batería.
- `specialUse` no tiene problema al instalar el APK directamente; en Google Play requeriría justificación.

---

## 6. Limitaciones conocidas y siguientes pasos

- Dispositivos con **volumen fijo** (algunos TV/auto): la app lo detecta y sugiere el modo demo.
- Escena optimizada para formato horizontal (cápsula ≈ 2.35:1); por eso la pantalla completa fuerza horizontal.
- Ideas para ampliar: reactividad al audio que suena (requiere `Visualizer` y permiso de micrófono), más paletas, widget de pantalla de inicio, exportar fondos de pantalla.
