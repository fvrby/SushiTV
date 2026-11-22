package tv.own.owntv.features.setup

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.koin.core.context.GlobalContext
import tv.own.owntv.BuildConfig
import tv.own.owntv.R
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.dao.resolveExistingProfileId
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.core.repository.SourceRepository
import tv.own.owntv.core.settings.PlaylistRefresh
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.setup.SourceImporter
import tv.own.owntv.core.sync.ImportFinalizer
import tv.own.owntv.core.sync.SyncContentTypes
import tv.own.owntv.core.sync.work.CatalogSyncScheduler

/**
 * SushiTV: the playlists built on the TV — "Pluto TV" (every free, legal Pluto channel) and the themed
 * "Música", "Películas" and "Series" lists that mix Chilean and Pluto channels. Each one is its own rail
 * entry (SushiRail).
 *
 * Pluto only serves programming through a session that expires after 24 h (boot.pluto.tv); fixed URLs
 * get a "Pluto TV is no longer available on this device" slate. So the lists are written here from the
 * two templates (assets/canales-chile.m3u, assets/peliculas-series-gratis.m3u — `pluto-<id>` ids, no
 * URLs), Pluto channels getting this session's URL, and rebuilt every [MAX_AGE_MS]. The session (it
 * carries the TV's approximate location) never leaves the TV except to Pluto itself. One session plays
 * one channel at a time.
 */
object SushiPluto {
    private const val PREFS = "sushitv"
    private const val KEY_CLIENT = "pluto_client_id"
    private const val KEY_BUILT_AT = "pluto_built_at"
    private const val KEY_LISTS = "lists_version"
    private const val LISTS_VERSION = 2 // bump when THEMES change, to rebuild at once
    private const val MAX_AGE_MS = 12L * 60 * 60 * 1000
    private const val BOOT_URL = "https://boot.pluto.tv/v4/start"
    private const val ID_PREFIX = "pluto-"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 12; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

    const val PLUTO_FILE = "pluto-en-vivo.m3u"
    const val MUSIC_FILE = "musica.m3u"
    const val MOVIES_FILE = "peliculas.m3u"
    const val SERIES_FILE = "series.m3u"

    /** A themed list: which Chilean groups / channel names and which Pluto groups go in it. */
    private class Theme(
        val file: String,
        val nameRes: Int,
        val chileGroups: Set<String> = emptySet(),
        val chileNames: Set<String> = emptySet(),
        val plutoGroups: Set<String>? = null, // null = every Pluto channel
    )

    private val THEMES = listOf(
        Theme(PLUTO_FILE, R.string.sushitv_rail_pluto),
        Theme(MUSIC_FILE, R.string.sushitv_rail_music, chileGroups = setOf("Música", "Radios con video"), chileNames = setOf("13 Festival"), plutoGroups = setOf("Música")),
        Theme(MOVIES_FILE, R.string.sushitv_rail_movies, plutoGroups = setOf("Películas")),
        Theme(SERIES_FILE, R.string.sushitv_rail_series, chileNames = setOf("13 Teleseries"), plutoGroups = setOf("Series", "Clásicos y retro", "Novelas", "Comedia")),
    )

    private val mutex = Mutex()

    /** A list this object builds (or 2026.11.4's fixed-URL Pluto list, migrated on the next refresh). */
    fun isPluto(url: String): Boolean = url.endsWith(PLUTO_FILE) || url.endsWith(SushiChileSource.FREE_TV)

    /** Build or refresh the lists when missing, outdated or older than [MAX_AGE_MS]. Safe to call often. */
    suspend fun refresh(context: Context) = mutex.withLock {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val koin = GlobalContext.get()
        val settings = koin.get<SettingsRepository>()
        val profileId = koin.get<ProfileDao>().resolveExistingProfileId(settings.activeProfileId.first()) ?: return@withLock
        val sources = koin.get<SourceRepository>().observeSources(profileId).first()
        if (sources.isEmpty()) return@withLock // setup not finished: try again later

        val files = THEMES.associateWith { File(context.filesDir, it.file) }
        val age = System.currentTimeMillis() - prefs.getLong(KEY_BUILT_AT, 0L)
        // Due: the session is getting old or the lists changed -> rewrite and re-sync all of them.
        // Otherwise only a list that is missing (a failed import) is added, and nothing else is touched,
        // so a failure never turns into an hourly re-sync of everything.
        val due = age >= MAX_AGE_MS || prefs.getInt(KEY_LISTS, 0) < LISTS_VERSION
        val missing = THEMES.filter { t -> !(files.getValue(t).exists() && sources.any { it.url == files.getValue(t).absolutePath }) }
        if (!due && missing.isEmpty()) return@withLock

        val clientId = prefs.getString(KEY_CLIENT, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_CLIENT, it).apply()
        }
        val session = boot(clientId)
        val chile = parse(template(context, SushiChileSource.CHILE))
        val pluto = parse(template(context, SushiChileSource.FREE_TV)).map { it.withUrl(session.urlFor(it.plutoId ?: "")) }
        for (t in THEMES) {
            val entries = chile.filter { it.group in t.chileGroups || it.name in t.chileNames } +
                pluto.filter { t.plutoGroups == null || it.group in t.plutoGroups }
            write(files.getValue(t), entries)
        }

        for (t in THEMES) {
            if (!due && t !in missing) continue
            val path = files.getValue(t).absolutePath
            val name = context.getString(t.nameRes)
            val existing = sources.firstOrNull { it.url == path }
                ?: if (t.file == PLUTO_FILE) sources.firstOrNull { it.url.endsWith(SushiChileSource.FREE_TV) } else null
            if (existing == null) {
                val importer = koin.get<SourceImporter>()
                importer.useProfile(profileId)
                importer.m3u(name, path, autoRefresh = PlaylistRefresh.OFF)
                // A failed import is simply missing next time and retried then, on its own.
            } else {
                if (existing.url != path || existing.name != name) {
                    val dao = koin.get<SourceDao>()
                    dao.getById(existing.id)?.let { dao.update(it.copy(url = path, name = name)) }
                    settings.setPlaylistAutoRefresh(existing.id, PlaylistRefresh.OFF)
                }
                resync(existing)
            }
        }
        if (due) prefs.edit().putLong(KEY_BUILT_AT, System.currentTimeMillis()).putInt(KEY_LISTS, LISTS_VERSION).apply()
    }

    private suspend fun resync(source: SourceEntity) {
        val koin = GlobalContext.get()
        val counts = koin.get<ImportFinalizer>().contentCounts(source.id)
        koin.get<CatalogSyncScheduler>().enqueueSync(
            source.id,
            reason = "sushitv_lists",
            contentTypes = SyncContentTypes.enabledOf(source),
            baseItemCount = counts.channels + counts.movies + counts.series,
            policy = androidx.work.ExistingWorkPolicy.REPLACE,
        )
    }

    /** A template from the GitHub repo (so lineups change without an APK), else the bundled copy. */
    private fun template(context: Context, asset: String): String {
        if (BuildConfig.SUSHITV_REPO.isNotBlank()) {
            runCatching { httpGet(SushiChileSource.location(context, asset)) }
                .getOrNull()?.takeIf { it.startsWith("#EXTM3U") }?.let { return it }
        }
        return context.assets.open(asset).bufferedReader().use { it.readText() }
    }

    private class Entry(val extinf: String, val extra: List<String>, val url: String) {
        val name: String get() = extinf.substringAfterLast(',').trim()
        val group: String get() = GROUP.find(extinf)?.groupValues?.get(1).orEmpty()
        val plutoId: String? get() = PLUTO_ID.find(extinf)?.groupValues?.get(1)
        fun withUrl(newUrl: String) = Entry(extinf, extra, newUrl)
    }

    private val GROUP = Regex("group-title=\"([^\"]*)\"")
    private val PLUTO_ID = Regex("tvg-id=\"$ID_PREFIX([0-9a-f]+)\"")

    private fun parse(text: String): List<Entry> {
        val out = ArrayList<Entry>()
        var extinf: String? = null
        val extra = ArrayList<String>()
        for (line in text.lineSequence()) {
            when {
                line.startsWith("#EXTINF") -> { extinf = line; extra.clear() }
                line.startsWith("#") -> if (extinf != null) extra += line
                line.isNotBlank() && extinf != null -> { out += Entry(extinf, extra.toList(), line.trim()); extinf = null }
            }
        }
        return out
    }

    private fun write(file: File, entries: List<Entry>) {
        val text = buildString {
            append("#EXTM3U\n")
            entries.forEach { e -> append(e.extinf).append('\n'); e.extra.forEach { append(it).append('\n') }; append(e.url).append('\n') }
        }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
    }

    private class Session(val stitcher: String, val params: String, val token: String) {
        fun urlFor(channelId: String) =
            "$stitcher/v2/stitch/hls/channel/$channelId/master.m3u8?$params&jwt=$token&masterJWTPassthrough=true"
    }

    /** A fresh anonymous session, the way Pluto's own web player starts one. */
    private fun boot(clientId: String): Session {
        val query = listOf(
            "appName" to "web", "appVersion" to "9.10.0", "deviceVersion" to "12", "deviceModel" to "web",
            "deviceMake" to "chrome", "deviceType" to "web", "clientID" to clientId,
            "clientModelNumber" to "1.0.0", "serverSideAds" to "false", "drmCapabilities" to "", "blockingMode" to "",
        ).joinToString("&") { (k, v) -> k + "=" + URLEncoder.encode(v, "UTF-8") }
        val json = JSONObject(httpGet("$BOOT_URL?$query"))
        return Session(
            stitcher = json.getJSONObject("servers").getString("stitcher"),
            params = json.getString("stitcherParams"),
            token = json.getString("sessionToken"),
        )
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 20_000
        conn.setRequestProperty("User-Agent", USER_AGENT)
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
