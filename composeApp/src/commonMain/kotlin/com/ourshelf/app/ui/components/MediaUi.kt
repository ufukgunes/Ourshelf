package com.ourshelf.app.ui.components

import com.ourshelf.app.resources.Res
import com.ourshelf.app.resources.length_episodes
import com.ourshelf.app.resources.length_hours
import com.ourshelf.app.resources.length_minutes
import com.ourshelf.app.resources.length_pages
import com.ourshelf.app.resources.length_tracks
import com.ourshelf.app.resources.status_abandoned
import com.ourshelf.app.resources.status_backlog
import com.ourshelf.app.resources.status_completed
import com.ourshelf.app.resources.status_in_progress
import com.ourshelf.app.resources.type_album
import com.ourshelf.app.resources.type_book
import com.ourshelf.app.resources.type_game
import com.ourshelf.app.resources.type_movie
import com.ourshelf.app.resources.type_podcast
import com.ourshelf.app.resources.type_tv_show
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.pluralStringResource
import com.ourshelf.app.core.model.LengthUnit
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.core.model.lengthUnit

val MediaType.labelRes: StringResource
    get() = when (this) {
        MediaType.MOVIE -> Res.string.type_movie
        MediaType.TV_SHOW -> Res.string.type_tv_show
        MediaType.BOOK -> Res.string.type_book
        MediaType.GAME -> Res.string.type_game
        MediaType.PODCAST -> Res.string.type_podcast
        MediaType.ALBUM -> Res.string.type_album
    }

val MediaType.icon: ImageVector
    get() = when (this) {
        MediaType.MOVIE -> Icons.Filled.Movie
        MediaType.TV_SHOW -> Icons.Filled.Tv
        MediaType.BOOK -> Icons.Filled.AutoStories
        MediaType.GAME -> Icons.Filled.SportsEsports
        MediaType.PODCAST -> Icons.Filled.Podcasts
        MediaType.ALBUM -> Icons.Filled.Album
    }

/** Albüm ve podcast kapakları kare, diğerleri 2:3 poster. */
val MediaType.posterAspectRatio: Float
    get() = when (this) {
        MediaType.ALBUM, MediaType.PODCAST -> 1f
        else -> 2f / 3f
    }

val MediaStatus.labelRes: StringResource
    get() = when (this) {
        MediaStatus.BACKLOG -> Res.string.status_backlog
        MediaStatus.IN_PROGRESS -> Res.string.status_in_progress
        MediaStatus.COMPLETED -> Res.string.status_completed
        MediaStatus.ABANDONED -> Res.string.status_abandoned
    }

private val LengthUnit.pluralRes: PluralStringResource
    get() = when (this) {
        LengthUnit.MINUTES -> Res.plurals.length_minutes
        LengthUnit.PAGES -> Res.plurals.length_pages
        LengthUnit.HOURS -> Res.plurals.length_hours
        LengthUnit.EPISODES -> Res.plurals.length_episodes
        LengthUnit.TRACKS -> Res.plurals.length_tracks
    }

@Composable
fun formatLength(type: MediaType, value: Int): String =
    pluralStringResource(type.lengthUnit.pluralRes, value, value)

/** İlerleme +/- butonlarının adım büyüklüğü. */
val MediaType.progressStep: Int
    get() = when (lengthUnit) {
        LengthUnit.PAGES, LengthUnit.MINUTES -> 10
        else -> 1
    }
