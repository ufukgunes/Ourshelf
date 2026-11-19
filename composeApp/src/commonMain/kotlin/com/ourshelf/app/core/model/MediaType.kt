package com.ourshelf.app.core.model

enum class MediaType {
    MOVIE,
    TV_SHOW,
    BOOK,
    GAME,
    PODCAST,
    ALBUM,
}

enum class MediaStatus {
    BACKLOG,
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
}

/** `lengthValue` / `progressValue` alanlarının tür bazında birimi. */
enum class LengthUnit { MINUTES, PAGES, HOURS, EPISODES, TRACKS }

val MediaType.lengthUnit: LengthUnit
    get() = when (this) {
        MediaType.MOVIE -> LengthUnit.MINUTES
        MediaType.TV_SHOW -> LengthUnit.EPISODES
        MediaType.BOOK -> LengthUnit.PAGES
        MediaType.GAME -> LengthUnit.HOURS
        MediaType.PODCAST -> LengthUnit.EPISODES
        MediaType.ALBUM -> LengthUnit.TRACKS
    }
