package tv.own.owntv.features.setup

import android.content.Context
import java.io.File
import tv.own.owntv.BuildConfig
import tv.own.owntv.core.settings.PlaylistAutoRefresh
import tv.own.owntv.core.settings.PlaylistRefresh

/**
 * SushiTV: the playlists that ship with the app (app/src/main/assets/).
 *
 * With a GitHub repo configured (sushitv.githubRepo) a playlist is read from that repo's default
 * branch and re-synced on every start, so a dead stream link is fixed by editing the file on GitHub —
 * no new APK. Without one, the copy bundled in the APK is written to app storage and imported from
 * there (core's M3U syncer reads absolute paths straight from disk).
 */
object SushiChileSource {
    const val CHILE = "canales-chile.m3u"
    const val FREE_TV = "peliculas-series-gratis.m3u"

    val refresh: PlaylistRefresh
        get() = if (remote) PlaylistRefresh(PlaylistAutoRefresh.HOURS_12) else PlaylistRefresh.OFF

    private val remote: Boolean get() = BuildConfig.SUSHITV_REPO.isNotBlank()

    /** The Chilean free-to-air playlist (setup wizard card). */
    fun location(context: Context): String = location(context, CHILE)

    /** The URL or absolute file path of the bundled playlist [asset], for the M3U importer. */
    fun location(context: Context, asset: String): String {
        if (remote) {
            return "https://raw.githubusercontent.com/${BuildConfig.SUSHITV_REPO}/HEAD/app/src/main/assets/$asset"
        }
        val out = File(context.filesDir, asset)
        context.assets.open(asset).use { input -> out.outputStream().use { input.copyTo(it) } }
        return out.absolutePath
    }
}
