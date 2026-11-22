package tv.own.owntv.features.setup

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import tv.own.owntv.BuildConfig
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.resolveExistingProfileId
import tv.own.owntv.core.repository.SourceRepository
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.sync.ImportFinalizer
import tv.own.owntv.core.sync.SyncContentTypes
import tv.own.owntv.core.sync.work.CatalogSyncScheduler

/**
 * SushiTV: notices when the lists on GitHub changed, so a channel added or fixed there reaches the TV
 * the next time the app opens instead of after the 12 h refresh.
 *
 * One HEAD request per list compares GitHub's ETag with the last one seen (nothing is downloaded when
 * nothing changed). TV chilena changed → it re-syncs (a normal sync, which also drops channels that left
 * the list). Any template of the lists built on the TV changed (TV chilena feeds Música; Mundo; Pluto's
 * lineup) → SushiPluto rebuilds them on its next pass. The first check after installing counts as a change.
 */
object SushiListWatch {
    private const val PREFS = "sushitv"
    private const val KEY_PREFIX = "etag_"

    suspend fun check(context: Context) {
        if (BuildConfig.SUSHITV_REPO.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val changed = listOf(SushiChileSource.CHILE, SushiChileSource.MUNDO, SushiChileSource.FREE_TV).filter { asset ->
            val etag = runCatching { etagOf(SushiChileSource.location(context, asset)) }.getOrNull() ?: return@filter false
            val key = KEY_PREFIX + asset
            val seen = prefs.getString(key, null)
            prefs.edit().putString(key, etag).apply()
            etag != seen
        }
        if (changed.isEmpty()) return
        if (SushiChileSource.CHILE in changed) resyncChile()
        SushiPluto.invalidate(context)
    }

    private suspend fun resyncChile() {
        val koin = GlobalContext.get()
        val settings = koin.get<SettingsRepository>()
        val profileId = koin.get<ProfileDao>().resolveExistingProfileId(settings.activeProfileId.first()) ?: return
        val source = koin.get<SourceRepository>().observeSources(profileId).first()
            .firstOrNull { it.url.endsWith(SushiChileSource.CHILE) } ?: return
        val counts = koin.get<ImportFinalizer>().contentCounts(source.id)
        koin.get<CatalogSyncScheduler>().enqueueSync(
            source.id,
            reason = "sushitv_list_changed",
            contentTypes = SyncContentTypes.enabledOf(source),
            baseItemCount = counts.channels + counts.movies + counts.series,
        )
    }

    private fun etagOf(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "HEAD"
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        try {
            if (conn.responseCode !in 200..299) return null
            return conn.getHeaderField("ETag")
        } finally {
            conn.disconnect()
        }
    }
}
