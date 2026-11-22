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
| 2026-11-21 | Logo dentro de la app = Sushi (barra lateral, inicio, Acerca de) y texto "SushiTV" en vez de "owntv" | `ui/components/BrandLockup.kt`, `res/drawable-nodpi/sushitv_mark.png`, `res/values/sushitv_brand.xml` |
| 2026-11-21 | Sin selector "Ícono de la app" ni "Logo en color de acento" (el ícono siempre es Sushi) | `SetupWizard.kt`, `SettingsScreen.kt` |
| 2026-11-21 | Textos visibles "OwnTV" → "SushiTV" (salvo los que nombran servicios de OwnTV: metadatos compartidos y traducción) | `res/values*/sushitv_rename.xml`, generados con `sushitv/tools/rename-strings.py` |

Las imágenes de marca se regeneran con `sushitv/tools/make-brand.ps1` a partir de `sushi-icon.png`
en la raíz (archivo local, no versionado).

## Compilar (Windows)

Herramientas ya instaladas en este PC: JDK 25 en `C:\Users\bloai\.jdks\jdk-25.0.5.1+2` y el
Android SDK en `C:\Users\bloai\Android\Sdk` (referenciado desde `local.properties`).

Versión de lanzamiento, firmada con la llave de SushiTV. Las versiones van **por fecha**:
`AAAA.MM.N` (N = número de release dentro del mes) y `VERSION_CODE = AAAAMM*100 + N`. Así nunca
chocan con las etiquetas de OwnTV (`v1.x`–`v5.x`), que vienen en el clon. Ejemplo para la primera:

```powershell
$env:JAVA_HOME = "C:\Users\bloai\.jdks\jdk-25.0.5.1+2"; $env:VERSION_NAME = "2026.11.1"; $env:VERSION_CODE = "20261101"; .\gradlew.bat :app:assembleStandardRelease
```

Cada release en GitHub lleva la etiqueta `v` + `VERSION_NAME` (ej. `v2026.11.1`) y el APK adjunto
como `sushitv.apk`. No uses `git push --tags`: subiría también las etiquetas de OwnTV.

El APK queda en `app\build\outputs\apk\standard\release\app-standard-release.apk`.

La llave está en `C:\Users\bloai\.sushitv-signing\sushitv-release.jks` y su contraseña en
`C:\Users\bloai\.gradle\gradle.properties` (líneas `owntv.*`). Ninguna de las dos va al repo.
**Respáldalas:** sin esa llave no se puede actualizar la app instalada.

## Instalar en la tele

1. En la tele: **Configuración › Sistema › Acerca de › Compilación del SO**. Presiona OK 7 veces
   hasta que diga que eres desarrollador.
2. **Configuración › Sistema › Opciones de desarrollador**: activa **Depuración USB** y, si
   aparece, **Depuración inalámbrica / por red**.
3. Busca la IP de la tele en **Configuración › Red**.
4. Desde el PC (la tele pedirá autorizar la conexión la primera vez):

```powershell
& "C:\Users\bloai\Android\Sdk\platform-tools\adb.exe" connect 192.168.1.50:5555
```

```powershell
& "C:\Users\bloai\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\standard\release\app-standard-release.apk
```

Reemplaza `192.168.1.50` por la IP de tu tele. En Google TV recientes, la depuración inalámbrica
muestra su propio puerto y un código de emparejamiento (`adb pair IP:PUERTO`).

## Canales

La lista está en `app/src/main/assets/canales-chile.m3u` (formato M3U estándar): **190 canales** en
12 grupos (Nacionales, Noticias, Deportes, Cultura e infantil, Entretención, Música, Radios con video,
Universitarios, Regionales, Institucionales, Religiosos y Paisajes en vivo). Fuentes:
[Alplox/json-teles](https://github.com/Alplox/json-teles), [iptv-org](https://github.com/iptv-org/iptv)
y [Free-TV/IPTV](https://github.com/Free-TV/IPTV). Solo señales gratuitas; se descartan las
retransmisiones desde IPs sueltas y los canales de pago (TCM, Nick Jr.) o extranjeros.

### Refrescar la lista

Los scripts de `sushitv/tools/canales/` rehacen todo el proceso: bajan las tres fuentes, prueban cada
señal hasta descargar un trozo de video real, unen duplicados y agrupan.

```powershell
$d = "$env:TEMP\sushitv-canales"; New-Item -ItemType Directory -Force $d | Out-Null
curl.exe -sSL https://raw.githubusercontent.com/Alplox/json-teles/main/countries/cl.json -o "$d\teles.json"
curl.exe -sSL https://iptv-org.github.io/iptv/countries/cl.m3u -o "$d\iptvorg.m3u"
curl.exe -sSL https://raw.githubusercontent.com/Free-TV/IPTV/master/lists/chile.md -o "$d\freetv.md"
Copy-Item app\src\main\assets\canales-chile.m3u "$d\current.m3u"
python -I sushitv\tools\canales\candidates.py $d; python -I sushitv\tools\canales\deepcheck.py $d
python -I sushitv\tools\canales\curate.py $d app\src\main\assets\canales-chile.m3u --report
```

Los nombres, grupos y canales excluidos se ajustan en `RULES` y `EXCLUDE` de `curate.py`. La lista
actual entra como fuente prioritaria, así que sus nombres y logos se conservan.

### Guía de programación (EPG)

La cabecera de la lista (`#EXTM3U url-tvg=...`) apunta a las guías XMLTV de open-epg.com, que traen
programación real (~1,5 días hacia adelante) y se renuevan a diario:

- `https://www.open-epg.com/files/chile2.xml`: Mega, Chilevisión, Canal 13, TV+, 24 Horas,
  T13 en vivo, CHV Noticias, NTV, 13 Cultura y TV Senado.
- `https://www.open-epg.com/files/chile1.xml`: La Red, UCV, TVU y Radio Agricultura.

`EPG_IDS` en `curate.py` fija el `tvg-id` de esos 14 canales exactamente como los escribe open-epg.
**No usar iptv-epg.org**: para Chile solo entrega programas de relleno que dicen "No Data".

OwnTV no descarga la guía solo: en la tele hay que agregarla una vez en **Configuración › Fuentes de
EPG › Agregar fuente de EPG** y dejar **Actualización automática** en "al iniciar". TV Chile tiene guía
en `https://epgshare01.online/epgshare01/epg_ripper_CL1.xml.gz` (id `TV.Chile.cl`). Los canales
regionales y las radios no tienen guía en ninguna fuente pública.

- **Sin repo configurado:** la lista viaja dentro del APK. Para cambiarla hay que recompilar.
- **Con `sushitv.githubRepo` configurado:** la app la lee desde GitHub y la refresca cada vez que
  se abre. Para arreglar un enlace basta con editar el archivo en GitHub.

Pendientes:
- **TVN** (señal principal): su reproductor web exige un token que cambia.
- **13 Entretención, 13 Prime, CNN Chile, Deportes13:** sus URLs públicas no entregaron video.
- **Solo por YouTube** (la app no reproduce YouTube en vivo): Cámara de Diputados, MegaDeportes,
  MegaTiempo, Teleseries TVN, UES TV, 24 Horas Valparaíso, entre otros.
- **Sin señal oficial en internet** (solo aparecen como retransmisiones desde IPs sueltas, que no se
  incluyen): Mega 2, Mega Ficción, Meganoticias Ahora.
- **Exclusivos de Zapping** (servicio de pago): los canales "Zapping" y "music" de Zapping.

## Actualizaciones por Wi-Fi

El actualizador de OwnTV consulta `api.github.com/repos/<sushitv.githubRepo>/releases/latest`, compara
la etiqueta (`v1.2.3`) con la versión instalada y descarga el `.apk` adjunto. Requisitos:

1. El repo debe ser **público** (la consulta no usa login).
2. Cada release necesita una etiqueta `vX.Y.Z` mayor que la anterior, y el APK se compila con esa
   versión (`VERSION_NAME` / `VERSION_CODE`).
3. Todas las versiones se firman con **la misma llave** (keystore). Si cambia la llave, Android no
   instala la actualización encima.
