package tv.own.owntv.features.setup

import android.content.Context
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import tv.own.owntv.R
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.resolveExistingProfileId
import tv.own.owntv.core.repository.SourceRepository
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.setup.SourceImporter

/**
 * SushiTV: adds the "Películas y series gratis" playlist (free, legal, ad-supported Pluto TV
 * channels — assets/peliculas-series-gratis.m3u) to the active profile by itself. It shows up as its
 * own playlist in the rail's playlist switcher, next to "Canales de Chile".
 *
 * Waits until setup is finished (an active profile that already has a playlist); until then it just
 * tries again on the next start. Done once per [VERSION]: if the user deletes the playlist afterwards
 * it stays deleted. Uses core's [SourceImporter] — a Koin factory, so this never touches the state of
 * an add the user is doing on screen.
 */
object SushiFreeTv {
    private const val VERSION = 1
    private const val PREFS = "sushitv"
    private const val KEY_VERSION = "free_tv_version"

    suspend fun ensure(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_VERSION, 0) >= VERSION) return
        val koin = GlobalContext.get()
        val settings = koin.get<SettingsRepository>()
        val profileId = koin.get<ProfileDao>().resolveExistingProfileId(settings.activeProfileId.first()) ?: return
        val sources = koin.get<SourceRepository>().observeSources(profileId).first()
        if (sources.isEmpty()) return // setup not finished yet

        val location = SushiChileSource.location(context, SushiChileSource.FREE_TV)
        val already = sources.any { it.url == location || it.url.endsWith(SushiChileSource.FREE_TV) }
        if (!already) {
            val importer = koin.get<SourceImporter>()
            importer.useProfile(profileId)
            importer.m3u(
                context.getString(R.string.sushitv_free_tv_title),
                location,
                autoRefresh = SushiChileSource.refresh,
            )
            if (importer.state.value !is SourceImporter.ImportState.Success) return // retry next start
        }
        prefs.edit().putInt(KEY_VERSION, VERSION).apply()
    }
}
