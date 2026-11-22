# SushiTV

App de Android TV para ver la TV abierta de Chile. Es un fork de
[OwnTV](https://github.com/ahXN00/OwnTV) de ahXN00 y sus colaboradores, distribuido bajo la
licencia **GPL-3.0** (ver `LICENSE` en la raíz). SushiTV mantiene esa misma licencia.

- Repo: <https://github.com/fvrby/SushiTV> (fork público de OwnTV).
- Base: la etiqueta **v5.2.2** de OwnTV. El `main` de OwnTV suele ir por delante del core publicado
  (`tv.own.owntv:core`) y no compila hasta que el autor publica la siguiente versión del core. Para
  traer mejoras, conviene hacer merge de la siguiente **etiqueta** de release (`git fetch upstream --tags`
  y luego `git merge v5.x.y`), no de `upstream/main`.

## Cambios respecto de OwnTV

Exigido por la GPL-3.0 (sección 5a): estos son los archivos que SushiTV modificó.

| Fecha | Cambio | Archivos |
|---|---|---|
| 2026-11-21 | ID de app `cl.sushitv`, para instalarse junto a OwnTV y no recibir sus actualizaciones | `app/build.gradle.kts` |
| 2026-11-21 | Nombre, ícono, banner y pantalla de inicio de SushiTV (foto de Sushi) | `AndroidManifest.xml`, `res/values*/themes.xml`, `res/values/sushitv_brand.xml`, `res/mipmap-anydpi-v26/sushitv_icon.xml`, `res/drawable-*/sushitv_*.png` |
| 2026-11-21 | Actualizador apuntando al repo de SushiTV (`sushitv.githubRepo`); sin repo no busca actualizaciones | `gradle.properties`, `app/build.gradle.kts`, `OwnTVApp.kt`, `OwnTVShell.kt`, `SettingsScreen.kt` |
| 2026-11-21 | Tarjeta "Canales de Chile" en el asistente inicial, con la lista incluida en el APK | `SetupWizard.kt`, `SushiChileSource.kt`, `app/src/main/assets/canales-chile.m3u` |
| 2026-11-21 | Literales técnicos nuevos registrados para la verificación i18n | `tools/i18n/safe_literals.txt` |
| 2026-11-21 | La foto original (`sushi-icon.png`) queda fuera del repo; solo se suben los recortes | `.gitignore` |

Las imágenes de marca se regeneran con `sushitv/tools/make-brand.ps1` a partir de `sushi-icon.png`
en la raíz (archivo local, no versionado).

## Compilar (Windows)

Herramientas ya instaladas en este PC: JDK 25 en `C:\Users\bloai\.jdks\jdk-25.0.5.1+2` y el
Android SDK en `C:\Users\bloai\AppData\Local\Android\Sdk` (referenciado desde `local.properties`).

```powershell
$env:JAVA_HOME = "C:\Users\bloai\.jdks\jdk-25.0.5.1+2"; .\gradlew.bat :app:assembleStandardDebug
```

El APK queda en `app\build\outputs\apk\standard\debug\`.

## Instalar en la tele

1. En la tele: **Configuración › Sistema › Acerca de › Compilación del SO**. Presiona OK 7 veces
   hasta que diga que eres desarrollador.
2. **Configuración › Sistema › Opciones de desarrollador**: activa **Depuración USB** y, si
   aparece, **Depuración inalámbrica / por red**.
3. Busca la IP de la tele en **Configuración › Red**.
4. Desde el PC (la tele pedirá autorizar la conexión la primera vez):

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" connect 192.168.1.50:5555
```

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\standard\debug\app-standard-debug.apk
```

Reemplaza `192.168.1.50` por la IP de tu tele. En Google TV recientes, la depuración inalámbrica
muestra su propio puerto y un código de emparejamiento (`adb pair IP:PUERTO`).

## Canales

La lista está en `app/src/main/assets/canales-chile.m3u` (formato M3U estándar). Cada canal tiene
el nombre, el logo, el grupo y la URL `.m3u8`. Fuentes: [Free-TV/IPTV](https://github.com/Free-TV/IPTV)
y [Alplox/json-teles](https://github.com/Alplox/json-teles). Solo señales oficiales y gratuitas.

- **Sin repo configurado:** la lista viaja dentro del APK. Para cambiarla hay que recompilar.
- **Con `sushitv.githubRepo` configurado:** la app la lee desde GitHub y la refresca cada vez que
  se abre. Para arreglar un enlace basta con editar el archivo en GitHub.

Pendientes: TVN (señal principal; su reproductor web exige un token que cambia), 13 Entretención
y 13 Prime (no entregaron video en la última prueba).

## Actualizaciones por Wi-Fi

El actualizador de OwnTV consulta `api.github.com/repos/<sushitv.githubRepo>/releases/latest`, compara
la etiqueta (`v1.2.3`) con la versión instalada y descarga el `.apk` adjunto. Requisitos:

1. El repo debe ser **público** (la consulta no usa login).
2. Cada release necesita una etiqueta `vX.Y.Z` mayor que la anterior, y el APK se compila con esa
   versión (`VERSION_NAME` / `VERSION_CODE`).
3. Todas las versiones se firman con **la misma llave** (keystore). Si cambia la llave, Android no
   instala la actualización encima.
