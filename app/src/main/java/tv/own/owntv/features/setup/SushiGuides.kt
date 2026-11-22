package tv.own.owntv.features.setup

import android.content.Context
import org.koin.core.context.GlobalContext
import tv.own.owntv.R
import tv.own.owntv.core.epg.EpgSourceStore
import tv.own.owntv.core.repository.EpgRepository
import tv.own.owntv.core.settings.EpgAutoRefresh
import tv.own.owntv.core.settings.EpgRefresh
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.sync.work.EpgSyncScheduler

/**
 * SushiTV: sets up the Chilean program guides by itself, so nobody types an XMLTV address with the
 * remote. Removes feeds known to ship only placeholder programmes, adds the open-epg feeds with
 * "refresh on start" and queues their first download — the same calls Settings › EPG sources makes.
 *
 * Runs once per [VERSION]: a guide the user deletes afterwards stays deleted. Bump [VERSION] when
 * [GUIDES] changes so existing installs pick the new list up.
 */
object SushiGuides {
    private const val VERSION = 2 // 2: refresh daily instead of on every start
    private const val PREFS = "sushitv"
    private const val KEY_VERSION = "guides_version"

    /** In priority order: chile2 has the national channels, chile1 adds La Red, UCV, TVU, Agricultura. */
    private val GUIDES = listOf(
        R.string.sushitv_guide_chile_2 to "https://www.open-epg.com/files/chile2.xml",
        R.string.sushitv_guide_chile_1 to "https://www.open-epg.com/files/chile1.xml",
    )

    /** Hosts whose Chilean guide is nothing but "No Data" placeholders. */
    private val BROKEN_HOSTS = listOf("iptv-epg.org")

    suspend fun ensure(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_VERSION, 0) >= VERSION) return
        val koin = GlobalContext.get()
        val store = koin.get<EpgSourceStore>()
        val epgRepository = koin.get<EpgRepository>()
        val settings = koin.get<SettingsRepository>()
        val scheduler = koin.get<EpgSyncScheduler>()

        store.getAll()
            .filter { source -> BROKEN_HOSTS.any { source.url.contains(it, ignoreCase = true) } }
            .forEach { source ->
                scheduler.cancelSync(source.id)
                epgRepository.clear(source.id)
                store.remove(source.id)
            }

        val present = store.getAll().map { it.url.trim() }.toSet()
        for ((nameRes, url) in GUIDES) {
            if (url in present) continue
            val source = store.add(context.getString(nameRes), url, null)
            scheduler.enqueueSync(source.id, "sushitv_guides")
        }
        // Once a day is plenty (the guides hold ~1.5 days) and spares the TV a 7 MB parse at every start.
        val urls = GUIDES.map { it.second }.toSet()
        store.getAll().filter { it.url.trim() in urls }
            .forEach { settings.setEpgAutoRefresh(it.id, EpgRefresh(EpgAutoRefresh.HOURS_24)) }
        prefs.edit().putInt(KEY_VERSION, VERSION).apply()
    }
}
