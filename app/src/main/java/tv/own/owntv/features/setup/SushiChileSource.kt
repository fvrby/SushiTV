package tv.own.owntv.features.setup

import android.content.Context
import java.io.File
import tv.own.owntv.BuildConfig
import tv.own.owntv.core.settings.PlaylistAutoRefresh
import tv.own.owntv.core.settings.PlaylistRefresh

/**
 * SushiTV: the built-in "Canales de Chile" playlist (app/src/main/assets/canales-chile.m3u).
 *
 * With a GitHub repo configured (sushitv.githubRepo) the playlist is read from that repo's default
 * branch and re-synced on every start, so a dead stream link is fixed by editing the file on GitHub —
 * no new APK. Without one, the copy bundled in the APK is written to app storage and imported from
 * there (core's M3U syncer reads absolute paths straight from disk).
 */
object SushiChileSource {
    private const val ASSET = "canales-chile.m3u"

    val refresh: PlaylistRefresh
        get() = if (remote) PlaylistRefresh(PlaylistAutoRefresh.STARTUP) else PlaylistRefresh.OFF

    private val remote: Boolean get() = BuildConfig.SUSHITV_REPO.isNotBlank()

    /** The URL or absolute file path to hand to the M3U importer. */
    fun location(context: Context): String {
        if (remote) {
            return "https://raw.githubusercontent.com/${BuildConfig.SUSHITV_REPO}/HEAD/app/src/main/assets/$ASSET"
        }
        val out = File(context.filesDir, ASSET)
        context.assets.open(ASSET).use { input -> out.outputStream().use { input.copyTo(it) } }
        return out.absolutePath
    }
}
