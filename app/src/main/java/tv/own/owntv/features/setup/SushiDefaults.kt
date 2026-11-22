package tv.own.owntv.features.setup

import android.content.Context
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext
import tv.own.owntv.R
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.database.dao.resolveExistingProfileId
import tv.own.owntv.core.repository.SourceRepository
import tv.own.owntv.core.settings.SettingsRepository

/**
 * SushiTV look, applied once per [VERSION] (the user can change any of it afterwards):
 * Live TV in the "Separate" layout, so the section column is always on screen, and the Chilean
 * playlist named "TV chilena" like its rail entry.
 */
object SushiDefaults {
    private const val VERSION = 2 // 2: TV chilena re-synced every 12 h, not on every start
    private const val PREFS = "sushitv"
    private const val KEY_VERSION = "defaults_version"

    suspend fun apply(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val done = prefs.getInt(KEY_VERSION, 0)
        if (done >= VERSION) return
        val koin = GlobalContext.get()
        val settings = koin.get<SettingsRepository>()
        if (done < 1) settings.setLiveLayout(SettingsRepository.LiveLayout.SEPARATE)

        val profileId = koin.get<ProfileDao>().resolveExistingProfileId(settings.activeProfileId.first())
        if (profileId != null) {
            val name = context.getString(R.string.sushitv_chile_title)
            val dao = koin.get<SourceDao>()
            koin.get<SourceRepository>().observeSources(profileId).first()
                .filter { it.url.endsWith(SushiChileSource.CHILE) }
                .forEach { s ->
                    if (done < 1 && s.name != name) dao.getById(s.id)?.let { dao.update(it.copy(name = name)) }
                    if (done < 2) settings.setPlaylistAutoRefresh(s.id, SushiChileSource.refresh)
                }
        }
        prefs.edit().putInt(KEY_VERSION, VERSION).apply()
    }
}
