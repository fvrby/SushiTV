package tv.own.owntv.features.setup

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import tv.own.owntv.R
import tv.own.owntv.core.database.entity.SourceEntity
import tv.own.owntv.features.shell.components.RailShortcut
import tv.own.owntv.ui.components.OwnTVIcon

/**
 * SushiTV: the rail's entries — TV chilena, Pluto TV, Música, Películas, Series. Each opens Live TV
 * with its own playlist chosen in OwnTV's playlist switcher, so every list keeps its own section column.
 */
object SushiRail {
    private class Spec(val nameRes: Int, val icon: OwnTVIcon, val image: Int, val matches: (String) -> Boolean)

    private val SPECS = listOf(
        Spec(R.string.sushitv_rail_tv_chilena, OwnTVIcon.LIVE_TV, R.drawable.sushitv_rail_chile) { it.endsWith(SushiChileSource.CHILE) },
        Spec(R.string.sushitv_rail_pluto, OwnTVIcon.PLAY_CIRCLE, R.drawable.sushitv_rail_pluto) { SushiPluto.isPluto(it) },
        Spec(R.string.sushitv_rail_music, OwnTVIcon.HEADPHONES, R.drawable.sushitv_rail_music) { it.endsWith(SushiPluto.MUSIC_FILE) },
        Spec(R.string.sushitv_rail_movies, OwnTVIcon.MOVIES, R.drawable.sushitv_rail_movies) { it.endsWith(SushiPluto.MOVIES_FILE) },
        Spec(R.string.sushitv_rail_series, OwnTVIcon.SERIES, R.drawable.sushitv_rail_series) { it.endsWith(SushiPluto.SERIES_FILE) },
    )

    @Composable
    fun shortcuts(
        playlists: List<SourceEntity>,
        activePlaylistId: Long,
        liveSelected: Boolean,
        open: (Long) -> Unit,
    ): List<RailShortcut> = SPECS.mapNotNull { spec ->
        val source = playlists.firstOrNull { spec.matches(it.url) } ?: return@mapNotNull null
        RailShortcut(stringResource(spec.nameRes), spec.icon, liveSelected && activePlaylistId == source.id, spec.image) { open(source.id) }
    }
}
