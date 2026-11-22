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
| 2026-11-22 | Menú: TV chilena, Pluto TV, Música y **Mundo** (canales gratis de otros países: anime, Japón y Corea, cine y series, y música por género que también sale en Música); fuera Películas y Series (sus canales siguen en las secciones de Pluto TV); ícono oficial de Pluto TV; EnerGeek (Fan, Retro y Radio) en TV chilena | `features/setup/SushiPluto.kt`, `SushiRail.kt`, `SushiChileSource.kt`, `assets/mundo.m3u`, `assets/canales-chile.m3u`, `res/values/sushitv_brand.xml`, `res/drawable-nodpi/sushitv_rail_*.png`, `sushitv/tools/make-rail-icons.ps1`, `sushitv/tools/marcas/pluto-tv.png`, `sushitv/tools/gratis/mundo*.py`, `envivo.py`, `sushitv/tools/canales/curate.py` |
| 2026-11-22 | Íconos propios en el menú (bandera, Plutón, nota, claqueta, tele) y título de TV en vivo = nombre de la lista; arranque más liviano (guías cada 24 h, TV chilena cada 12 h, trabajo de SushiTV 60 s después de abrir, reintentos solo de lo que falta) | `sushitv/tools/make-rail-icons.ps1`, `res/drawable-nodpi/sushitv_rail_*.png`, `StageRail.kt`, `SushiRail.kt`, `live/LiveViewModel.kt`, `LiveStage.kt`, `LiveScreen.kt`, `SushiGuides.kt`, `SushiDefaults.kt`, `SushiChileSource.kt`, `SushiPluto.kt`, `OwnTVApp.kt` |
| 2026-11-22 | Menú: TV chilena, Pluto TV, Música, Películas y Series (listas armadas en la tele); fuera Deportes, Guía y Películas/Series a pedido; bienvenida de ~4 s | `features/setup/SushiPluto.kt`, `SushiRail.kt`, `components/StageRail.kt`, `SushiWelcome.kt`, `res/values/sushitv_brand.xml` |
| 2026-11-22 | Sushi duerme sobre el logo del menú (respira, mueve la cola y las orejas, despierta al seleccionar el logo); las listas de GitHub que cambian llegan a la tele al abrir la app (ETag), sin esperar 12 h | `features/setup/SushiNap.kt`, `SushiListWatch.kt`, `SushiPluto.kt`, `SushiCatArt.kt`, `components/StageRail.kt`, `OwnTVApp.kt`, `sushitv/tools/bienvenida/siesta.html` |
| 2026-11-22 | Animaciones secretas: mantener OK sobre el logo de Sushi del menú lanza una de 4 al azar (la caja, puntero láser, la siesta, lluvia de sushi), flotando sobre la app y con sonido; la bienvenida espera a que la tele esté fluida (luz de "en espera") y se puede volver a ver en Acerca de; EnerGeek con su nombre oficial | `features/setup/SushiSecrets.kt`, `SushiCatArt.kt`, `SushiWelcome.kt`, `components/StageRail.kt` (RailBrand), `more/MorePages.kt` (AboutPage), `MainActivity.kt`, `res/raw/sushitv_secreto_*.wav`, `sushitv/tools/bienvenida/secretos.*`, `synth.js` |
| 2026-11-22 | Bienvenida de 10 s dibujada con código (sin imágenes): tele retro que se enciende, Sushi con sus manchas reales, cola, corazón, "SushiTV" letra por letra y parpadeo lento; sonido de 10 s con música hecha en código y ruiditos reales de Sushi; cualquier botón la salta | `features/setup/SushiWelcome.kt`, `MainActivity.kt` (dispatchKeyEvent), `res/raw/sushitv_bienvenida.wav`, `res/values/sushitv_brand.xml`, `sushitv/tools/bienvenida/` |
| 2026-11-22 | Avatares = fotos de los gatos (sin dibujos predefinidos ni silueta "sin avatar"); se mantiene "Tu propia foto" | `ui/components/OwnTVAvatar.kt`, `AvatarPickerDialog.kt`, `ProfileComponents.kt`, `SetupWizard.kt`, `StageRail.kt`, `res/drawable-nodpi/sushitv_avatar_*.png` (de `sushitv/tools/make-avatars.ps1`; fotos originales en `avatares/`, fuera de git) |
| 2026-11-22 | Íconos "TV chilena" y "Películas y series" en el menú lateral en lugar de "TV en vivo"; TV en vivo en diseño Separado | `components/StageRail.kt`, `shell/OwnTVShell.kt`, `features/setup/SushiRail.kt`, `features/setup/SushiDefaults.kt` |
| 2026-11-22 | Lista "Películas y series" (Pluto TV) armada en la tele con sesión propia y renovada cada 12 h | `features/setup/SushiPluto.kt`, `SushiChileSource.kt`, `OwnTVApp.kt`, `assets/peliculas-series-gratis.m3u`, `sushitv/tools/gratis/pluto.py` |
| 2026-11-22 | Guías de programación de Chile (open-epg) configuradas solas al abrir la app | `features/setup/SushiGuides.kt`, `OwnTVApp.kt`, `res/values/sushitv_brand.xml` |
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

La lista está en `app/src/main/assets/canales-chile.m3u` (formato M3U estándar): **193 canales** en
13 grupos (Nacionales, Noticias, Deportes, Cultura e infantil, Geek y anime, Entretención, Música, Radios
con video, Universitarios, Regionales, Institucionales, Religiosos y Paisajes en vivo). Fuentes:
[Alplox/json-teles](https://github.com/Alplox/json-teles), [iptv-org](https://github.com/iptv-org/iptv)
y [Free-TV/IPTV](https://github.com/Free-TV/IPTV); los de EnerGeek (canal geek de Santiago) con los enlaces
que EnerGeek publica en la lista de [TDTChannels](https://www.tdtchannels.com) (con los nombres de su propio
catálogo, `neotv.energeek.cl/assets/iptv.json`: EnerGeek, EnerGeek Fan y EnerGeek Radio, juntos en "Geek y
anime"; la radio sale también en Música). Solo señales gratuitas; se descartan las
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

OwnTV no descarga guías por su cuenta, así que SushiTV las configura sola
(`features/setup/SushiGuides.kt`): unos segundos después de abrir la app borra las fuentes de
iptv-epg.org, agrega las dos de open-epg con **Actualización automática: al iniciar** y las descarga.
Corre una vez por `VERSION`; súbela si cambian las guías. TV Chile tiene guía
en `https://epgshare01.online/epgshare01/epg_ripper_CL1.xml.gz` (id `TV.Chile.cl`). Los canales
regionales y las radios no tienen guía en ninguna fuente pública.

- **Sin repo configurado:** la lista viaja dentro del APK. Para cambiarla hay que recompilar.
- **Con `sushitv.githubRepo` configurado:** la app la lee desde GitHub y la refresca cada 12 h; además, un
  minuto después de abrirla y luego cada hora, pregunta a GitHub si la lista cambió (`SushiListWatch.kt`: una
  petición HEAD con su ETag, sin descargar nada si no cambió) y, si cambió, la vuelve a sincronizar al tiro.
  Lo mismo para Mundo y la lista de Pluto: si cambian, Pluto TV, Música y Mundo se rehacen. Para arreglar un enlace basta con editar el archivo en GitHub.

Pendientes:
- **TVN** (señal principal): su reproductor web exige un token que cambia.
- **13 Entretención, 13 Prime, CNN Chile, Deportes13:** sus URLs públicas no entregaron video.
- **Solo por YouTube** (la app no reproduce YouTube en vivo): Cámara de Diputados, MegaDeportes,
  MegaTiempo, Teleseries TVN, UES TV, 24 Horas Valparaíso, entre otros.
- **Sin señal oficial en internet** (solo aparecen como retransmisiones desde IPs sueltas, que no se
  incluyen): Mega 2, Mega Ficción, Meganoticias Ahora.
- **Exclusivos de Zapping** (servicio de pago): los canales "Zapping" y "music" de Zapping.

## Menú lateral

En vez de un solo "TV en vivo", el menú tiene cuatro íconos (`features/setup/SushiRail.kt`,
`StageRail.kt`, `OwnTVShell.kt`). Cada uno abre TV en vivo con su lista elegida en el selector de
listas de OwnTV, así cada lista muestra solo sus propias secciones. `SushiDefaults.kt` deja TV en vivo
en el diseño **Separado** (columna de secciones siempre visible) y llama "TV chilena" a la lista chilena.

| Ícono | Lista | Qué trae |
|---|---|---|
| Bandera | TV chilena | `canales-chile.m3u` |
| Pluto TV (su ícono oficial) | Pluto TV | todos los canales de Pluto, con sus secciones (Películas, Series, Anime…) |
| Nota musical | Música | la música de las tres: grupos Música y Radios con video de Chile, 13 Festival, Música de Pluto y los géneros musicales de Mundo |
| Globo | Mundo | `mundo.m3u`: canales gratis de otros países |

Pluto TV, Música y Mundo las arma la tele (`SushiPluto.kt`) y las rehace cada 12 h. Hasta la versión
2026.11.8 hubo también "Películas" y "Series"; la app las borra sola (sus canales siguen en Pluto TV).
Los íconos salen de `sushitv/tools/make-rail-icons.ps1`.

## Pluto TV

**196 canales de Pluto TV** (gratis y legal, con publicidad) disponibles en Chile, en 17 secciones
(Películas, Series, Novelas, Comedia, Infantil, Anime, Documentales, Crimen, Reality, Deportes, etc.).

Desde noviembre de 2026 Pluto solo entrega programación con una **sesión que vence a las 24 h**
(`boot.pluto.tv/v4/start`); las URLs fijas antiguas reciben un video de *"Pluto TV is no longer available
on this device"*. Por eso:

- `app/src/main/assets/peliculas-series-gratis.m3u` es solo una **plantilla** (nombre, sección, logo e
  id `pluto-<id>` de cada canal, sin URLs de sesión).
- La app (`features/setup/SushiPluto.kt`) pide una sesión anónima en la tele, escribe la lista con URLs
  válidas en el almacenamiento de la app, la importa como "Pluto TV" (y su música en "Música") y la rehace cada 12 h. La
  sesión incluye la ubicación aproximada de la tele: queda en la tele y solo viaja a Pluto.
- Una sesión no acepta muchos canales abiertos a la vez: se ve un canal por vez (Multivista con canales
  de Pluto puede fallar).

Para regenerar la plantilla (desde Chile: la API de Pluto responde según el país de la conexión). Prueba
los canales de a uno con una sesión real y descarta los que entregan el aviso; tarda unos 3 minutos:

```powershell
python -I sushitv\tools\gratis\pluto.py app\src\main\assets\peliculas-series-gratis.m3u
```

Sin guía de programación por ahora: la guía pública de Pluto para Chile (i.mjh.nz) no corresponde a esta
programación.

No se incluyen canales de cable de pago (History, Cartoon Network, Discovery…): no tienen señal
gratuita oficial, y las que circulan son retransmisiones piratas.

## Mundo (canales de otros países)

`app/src/main/assets/mundo.m3u`: canales gratuitos y oficiales de fuera de Chile.

- **Anime:** MAX Anime (en español), RetroCrush, FilmRise Anime, Yu-Gi-Oh!
- **Japón y Corea:** NHK World Japan, Arirang TV, Rakuten Viki (doramas con subtítulos en inglés)
- **Cine y series:** Cine Friki, Cine Western, MyTime Movies, TVE Star, Todo Novelas
- **Música, por género** (sobre todo las señales gratuitas de Stingray, XITE, Vevo, Now y Totalmusic):
  K-pop · Rock y metal · Pop y éxitos · Latino y reggaetón · Rap, soul y R&B · Jazz · Country ·
  Conciertos (Qello, Totalmusic Concerts, Stingray Classica) · Retro 70-80-90 · Electrónica y dance ·
  Bandas sonoras y karaoke

La línea `#SUSHITV-MUSICA:` al inicio del archivo dice qué grupos son música: esos también aparecen en
la lista **Música** del menú. Para sumar un género nuevo basta con agregarlo ahí (lo escribe `mundo.py`).

Cada uno es la señal propia del canal o su señal gratuita oficial (FAST), probada desde Chile hasta el
video y mirada en pantalla. Como el resto de las listas, la app la lee desde GitHub: para sumar o
arreglar un canal basta con editar el archivo.

Para rehacerla (en una carpeta propia; los pasos de descarga están al inicio de `mundo_candidatos.py`):

```powershell
python -I sushitv\tools\gratis\mundo_candidatos.py $d; python -I sushitv\tools\canales\deepcheck.py $d
python -I sushitv\tools\gratis\envivo.py $d nombres.txt
python -I sushitv\tools\gratis\mundo.py $d app\src\main\assets\mundo.m3u
```

Los canales se eligen a mano en `PICKS` de `mundo.py`. Fuera a propósito: retransmisiones de canales
de pago (Cartoon Network, Tooncast, Disney, M6 Music…), canales solo de YouTube (KBS World, The K-POP),
los exclusivos de teles Samsung o LG (Anime Onegai, It's Anime), Trace (sus enlaces solo funcionan
haciéndose pasar por la web de Trace) y Kpop Mix (solo mostraba "En breve comenzamos"). No hay canal
de TV gratuito oficial de J-pop ni de música de videojuegos (solo radios sin imagen). Si un canal de
`PICKS` no responde al armar la lista, queda fuera y `mundo.py` lo avisa.

## Bienvenida

Al abrir la app (una vez por arranque, y cuando quieras desde **Acerca de › Ver bienvenida de Sushi**) se
ve una animación de 10 s sobre la app, toda dibujada con código en `features/setup/SushiWelcome.kt` (Sushi
misma está en `SushiCatArt.kt`, compartida con las animaciones secretas), sin imágenes. Como al encender la
tele la app tarda un momento en ir fluida, primero se ve la tele apagada con su luz roja de "en espera"
hasta que los cuadros salen parejos (máximo 3 s); recién ahí parte el espectáculo: una tele retro se enciende, Sushi sube, se agarra del
borde con las patitas, abre sus ojos verdes, mueve la oreja, ladea la cabeza, sale un corazón, asoma la
cola, cae "SushiTV" letra por letra y Sushi hace el parpadeo lento. Cualquier botón del control (menos el
volumen) la salta; si algo fallara al dibujarla, se salta sola, y nunca retiene el control más de 12 s.

El sonido es un solo archivo de 10 s, `res/raw/sushitv_bienvenida.wav`: música hecha con código (cajita
musical, efectos de la tele y las patitas) más 6 ruiditos reales de Sushi. Se arma en el laboratorio de
`sushitv/tools/bienvenida/`:

```powershell
python -I sushitv\tools\bienvenida\servidor.py sushitv\tools\bienvenida 8765
```

Luego abre `http://localhost:8765/mix.html`: "Ver con sonido" muestra la animación con el audio, la tabla
deja escuchar cada ruidito y elegir cuáles usar (`SEGS` en `mix.html`: archivo, inicio, fin y segundo
en que suena). Para guardar la mezcla, en la consola del navegador: `await exportWav(22050)`; queda en
`sushitv/tools/bienvenida/out/bienvenida.wav`, que se copia a `app/src/main/res/raw/sushitv_bienvenida.wav`.

### Sushi durmiendo en el logo

Sobre el logo del menú lateral duerme una Sushi chiquitita (`features/setup/SushiNap.kt`): respira, mueve la
cola, a ratos mueve una oreja y le sale una "z". Al seleccionar el logo abre los ojos y te mira; mientras corre
una animación secreta se va del logo. Se dibuja con código a 30 cuadros por segundo como máximo y solo
mientras la app está en pantalla. Preview: `http://localhost:8765/siesta.html`.

### Animaciones secretas

Con el logo de Sushi del menú lateral seleccionado, **OK** la hace dar un saltito y **mantener OK** lanza una
de 4 escenas al azar (nunca la misma dos veces seguidas), flotando sobre lo que haya en pantalla y sin quitar
el control (`features/setup/SushiSecrets.kt`):

1. **La caja** (7 s): asoma de una caja de cartón, salta con papelitos y se duerme en ella.
2. **Puntero láser** (7 s): sigue un puntito rojo, se agacha, salta, falla y se lame la patita.
3. **La siesta** (8 s): duerme soñando con sushi volador, despierta, se estira y bosteza.
4. **Lluvia de sushi** (8 s): atrapa nigiris, makis y pescaditos con la boca y los cuenta.

Cada una tiene su sonido (`res/raw/sushitv_secreto_1.wav` … `_4.wav`, 16 kHz): música y efectos hechos con
código (`synth.js`) más ruiditos de Sushi. Se ven y escuchan en `http://localhost:8765/secretos.html` (el
mismo laboratorio); `secretos.js` es la misma animación que `SushiSecrets.kt`. Para regenerar los sonidos,
en la consola del navegador: `await exportAll()`; quedan en `out/` y se copian a `app/src/main/res/raw/`.

Las grabaciones originales van en `sushitv/tools/bienvenida/audios/` (`a.ogg` … `d.ogg`, notas de voz de
WhatsApp) y quedan fuera de git; al APK solo llega la mezcla final. `anim.js` es la misma animación que
`SushiWelcome.kt`, para verla en el navegador: si cambias una, cambia la otra.

## Actualizaciones por Wi-Fi

El actualizador de OwnTV consulta `api.github.com/repos/<sushitv.githubRepo>/releases/latest`, compara
la etiqueta (`v1.2.3`) con la versión instalada y descarga el `.apk` adjunto. Requisitos:

1. El repo debe ser **público** (la consulta no usa login).
2. Cada release necesita una etiqueta `vX.Y.Z` mayor que la anterior, y el APK se compila con esa
   versión (`VERSION_NAME` / `VERSION_CODE`).
3. Todas las versiones se firman con **la misma llave** (keystore). Si cambia la llave, Android no
   instala la actualización encima.
