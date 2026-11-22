package tv.own.owntv.features.setup

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.features.shell.components.RailShortcut
import tv.own.owntv.ui.components.OwnTVIcon

/**
 * SushiTV: the rail's "TV chilena" and "Películas y series" entries. Each one opens Live TV with its
 * playlist chosen in OwnTV's own playlist switcher, so every list keeps its own section column.
 */
object SushiRail {
    @Composable
    fun shortcuts(
        playlists: List<SourceEntity>,
        activePlaylistId: Long,
        liveSelected: Boolean,
        open: (Long) -> Unit,
    ): List<RailShortcut> {
        val chile = playlists.firstOrNull { it.url.endsWith(SushiChileSource.CHILE) }
        val pluto = playlists.firstOrNull { SushiPluto.isPluto(it.url) }
        return listOfNotNull(
            chile?.let {
                RailShortcut(stringResource(R.string.sushitv_rail_tv_chilena), OwnTVIcon.LIVE_TV, liveSelected && activePlaylistId == it.id) { open(it.id) }
            },
            pluto?.let {
                RailShortcut(stringResource(R.string.sushitv_rail_movies), OwnTVIcon.MOVIES, liveSelected && activePlaylistId == it.id) { open(it.id) }
            },
        )
    }
}
