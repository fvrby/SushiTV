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
 * SushiTV: the "Películas y series" playlist — free, legal, ad-supported Pluto TV channels.
 *
 * Pluto only serves programming through a session that expires after 24 h (boot.pluto.tv); fixed URLs
 * get a "Pluto TV is no longer available on this device" slate. So the playlist is built here, on the
 * TV: the template (assets/peliculas-series-gratis.m3u: names, sections, logos, `pluto-<id>` ids) gets
 * one URL per channel from a fresh anonymous session, is written to app storage and imported from
 * there, and is rebuilt every [MAX_AGE_MS]. The session (it carries the TV's approximate location) never
 * leaves the TV except to Pluto itself.
 */
object SushiPluto {
    private const val PREFS = "sushitv"
    private const val KEY_CLIENT = "pluto_client_id"
    private const val KEY_BUILT_AT = "pluto_built_at"
    private const val LOCAL_FILE = "pluto-en-vivo.m3u"
    private const val MAX_AGE_MS = 12L * 60 * 60 * 1000
    private const val BOOT_URL = "https://boot.pluto.tv/v4/start"
    private const val ID_PREFIX = "pluto-"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 12; TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

    private val mutex = Mutex()

    /** This playlist, whether built here or 2026.11.4's fixed-URL version still pending migration. */
    fun isPluto(url: String): Boolean = url.endsWith(LOCAL_FILE) || url.endsWith(SushiChileSource.FREE_TV)

    /** Build or refresh the playlist when it is missing or older than [MAX_AGE_MS]. Safe to call often. */
    suspend fun refresh(context: Context) = mutex.withLock {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val koin = GlobalContext.get()
        val settings = koin.get<SettingsRepository>()
        val profileId = koin.get<ProfileDao>().resolveExistingProfileId(settings.activeProfileId.first()) ?: return@withLock
        val sources = koin.get<SourceRepository>().observeSources(profileId).first()
        if (sources.isEmpty()) return@withLock // setup not finished: try again later

        val file = File(context.filesDir, LOCAL_FILE)
        val existing = sources.firstOrNull { it.url == file.absolutePath }
            ?: sources.firstOrNull { it.url.endsWith(SushiChileSource.FREE_TV) } // 2026.11.4's fixed-URL list
        val age = System.currentTimeMillis() - prefs.getLong(KEY_BUILT_AT, 0L)
        if (existing?.url == file.absolutePath && file.exists() && age < MAX_AGE_MS) return@withLock

        val clientId = prefs.getString(KEY_CLIENT, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_CLIENT, it).apply()
        }
        writePlaylist(file, template(context), boot(clientId))

        val name = context.getString(R.string.sushitv_free_tv_title)
        when {
            existing == null -> {
                val importer = koin.get<SourceImporter>()
                importer.useProfile(profileId)
                importer.m3u(name, file.absolutePath, autoRefresh = PlaylistRefresh.OFF)
                if (importer.state.value !is SourceImporter.ImportState.Success) return@withLock
            }
            else -> {
                if (existing.url != file.absolutePath || existing.name != name) {
                    val dao = koin.get<SourceDao>()
                    dao.getById(existing.id)?.let { dao.update(it.copy(url = file.absolutePath, name = name)) }
                    settings.setPlaylistAutoRefresh(existing.id, PlaylistRefresh.OFF)
                }
                resync(existing)
            }
        }
        prefs.edit().putLong(KEY_BUILT_AT, System.currentTimeMillis()).apply()
    }

    private suspend fun resync(source: SourceEntity) {
        val koin = GlobalContext.get()
        val counts = koin.get<ImportFinalizer>().contentCounts(source.id)
        koin.get<CatalogSyncScheduler>().enqueueSync(
            source.id,
            reason = "sushitv_pluto",
            contentTypes = SyncContentTypes.enabledOf(source),
            baseItemCount = counts.channels + counts.movies + counts.series,
            policy = androidx.work.ExistingWorkPolicy.REPLACE,
        )
    }

    /** The template from the GitHub repo (so the lineup can change without an APK), else the bundled copy. */
    private fun template(context: Context): String {
        if (BuildConfig.SUSHITV_REPO.isNotBlank()) {
            runCatching { httpGet(SushiChileSource.location(context, SushiChileSource.FREE_TV)) }
                .getOrNull()?.takeIf { it.startsWith("#EXTM3U") }?.let { return it }
        }
        return context.assets.open(SushiChileSource.FREE_TV).bufferedReader().use { it.readText() }
    }

    private class Session(val stitcher: String, val params: String, val token: String)

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

    /** Template -> playable playlist: every `pluto-<id>` channel gets this session's stream URL. */
    private fun writePlaylist(file: File, template: String, session: Session) {
        var channelId: String? = null
        val out = template.lineSequence().map { line ->
            when {
                line.startsWith("#EXTINF") -> {
                    channelId = Regex("tvg-id=\"$ID_PREFIX([0-9a-f]+)\"").find(line)?.groupValues?.get(1)
                    line
                }
                line.isNotBlank() && !line.startsWith("#") && channelId != null -> {
                    val id = channelId; channelId = null
                    "${session.stitcher}/v2/stitch/hls/channel/$id/master.m3u8?${session.params}" +
                        "&jwt=${session.token}&masterJWTPassthrough=true"
                }
                else -> line
            }
        }.joinToString("\n")
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(out)
        if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
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
