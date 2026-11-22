package tv.own.owntv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import tv.own.owntv.R

/**
 * SushiTV: the profile avatars are the family's cat photos (res/drawable-nodpi/sushitv_avatar_NN.png,
 * made by sushitv/tools/make-avatars.ps1) instead of OwnTV's drawn object tiles and silhouette.
 */
object OwnTVAvatars {
    const val COUNT = 11
}

private val SushiAvatars = intArrayOf(
    R.drawable.sushitv_avatar_01, R.drawable.sushitv_avatar_02, R.drawable.sushitv_avatar_03,
    R.drawable.sushitv_avatar_04, R.drawable.sushitv_avatar_05, R.drawable.sushitv_avatar_06,
    R.drawable.sushitv_avatar_07, R.drawable.sushitv_avatar_08, R.drawable.sushitv_avatar_09,
    R.drawable.sushitv_avatar_10, R.drawable.sushitv_avatar_11,
)

@Composable
fun OwnTVAvatar(avatarId: Int, modifier: Modifier = Modifier, imagePath: String = "") {
    // -1 was OwnTV's "no avatar": SushiTV has none, so it shows the first cat.
    val i = if (avatarId < 0) 0 else avatarId % OwnTVAvatars.COUNT
    Box(modifier = modifier) {
        // The cat stays underneath the user's own picture: it fills the gap while the file loads and
        // is what shows if the picture has gone.
        Image(
            painter = painterResource(SushiAvatars[i]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().clip(AvatarShape),
        )
        if (imagePath.isNotBlank()) {
            AsyncImage(
                model = java.io.File(imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().clip(AvatarShape),
            )
        }
    }
}

/** The avatar shape everywhere a profile picture appears. */
private val AvatarShape = RoundedCornerShape(percent = 30)
