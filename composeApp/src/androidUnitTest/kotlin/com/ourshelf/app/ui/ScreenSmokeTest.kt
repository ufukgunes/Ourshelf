package com.ourshelf.app.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ourshelf.app.core.model.MediaCollection
import com.ourshelf.app.core.model.MediaItem
import com.ourshelf.app.core.model.MediaMetadata
import com.ourshelf.app.core.model.MediaStatus
import com.ourshelf.app.core.model.MediaType
import com.ourshelf.app.core.model.Shelf
import com.ourshelf.app.ui.detail.DetailActions
import com.ourshelf.app.ui.detail.DetailScreen
import com.ourshelf.app.ui.detail.DetailUiState
import com.ourshelf.app.ui.lounge.LoungeScreen
import com.ourshelf.app.ui.lounge.LoungeUiState
import com.ourshelf.app.ui.onboarding.OnboardingScreen
import com.ourshelf.app.ui.theme.OurshelfTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import android.content.ContentProvider
import org.junit.Before
import org.robolectric.Robolectric
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Instant

/**
 * Ekranları Robolectric üzerinde gerçekten render eder ve temel etkileşimleri dener.
 * Amaç cihaz olmadan çalışma zamanı çökmelerini ve bağlantı hatalarını yakalamak.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-w411dp-h891dp")
class ScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * Compose Multiplatform kaynakları Android Context'ini, uygulama açılışında manifest'ten başlatılan bir
     * ContentProvider ile alır. Robolectric bunu kendiliğinden başlatmadığı için testte elle oluşturulur.
     */
    @Before
    fun initComposeResources() {
        @Suppress("UNCHECKED_CAST")
        val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider") as Class<ContentProvider>
        Robolectric.buildContentProvider(provider).create()
    }

    private fun item(
        id: String,
        title: String,
        status: MediaStatus = MediaStatus.BACKLOG,
        type: MediaType = MediaType.MOVIE,
    ) = MediaItem(
        id = id, externalId = "mock:$id", mediaType = type, title = title, subtitle = "Someone • 2020",
        posterUrl = null, localPosterFile = null, backdropUrl = null, dominantColor = null, accentColor = null,
        releaseYear = 2020, lengthValue = 120, genres = listOf("Drama"), overview = "An overview.",
        metadata = MediaMetadata(platforms = listOf("PC")), status = status, progressValue = 30, userRating = 3.5f,
        userNote = "Nice", isPinned = false, addedAt = Instant.fromEpochMilliseconds(0), startedAt = null, completedAt = null,
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun actions(
        onComplete: () -> Unit = {},
        onStatus: (MediaStatus) -> Unit = {},
    ) = DetailActions(
        onStatusChange = onStatus, onRatingChange = {}, onProgressChange = {}, onNoteChange = {},
        onPinnedChange = {}, onToggleCollection = { _, _ -> }, onComplete = onComplete, onFinisherClosed = {},
        onDelete = {},
    )

    @Test
    fun detail_renders_and_hold_to_complete_has_accessible_one_tap_action() {
        var completed = 0
        compose.setContent {
            OurshelfTheme {
                DetailScreen(
                    state = DetailUiState.Ready(item("1", "Arrival", MediaStatus.IN_PROGRESS), emptyList(), emptySet()),
                    sharedKey = "test",
                    onBack = {},
                    actions = actions(onComplete = { completed++ }),
                )
            }
        }
        // Başlık hem posterin yedek kapağında hem sayfa başlığında görünür.
        assertEquals(2, compose.onAllNodesWithText("Arrival").fetchSemanticsNodes().size)
        compose.onNodeWithText("30 / 120").assertExists()

        // TalkBack'in çift dokunuşu = semantik OnClick; basılı tutmadan tamamlar.
        val holdButton = SemanticsMatcher("OnClick label = hold") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label == "Hold to mark as done"
        }
        compose.onNode(holdButton).performSemanticsAction(SemanticsActions.OnClick)
        compose.runOnIdle { assertEquals(1, completed) }
    }

    @Test
    fun completed_detail_shows_stamp_and_hides_hold_button() {
        compose.setContent {
            OurshelfTheme {
                DetailScreen(
                    state = DetailUiState.Ready(item("1", "Coco", MediaStatus.COMPLETED), emptyList(), emptySet()),
                    sharedKey = "test",
                    onBack = {},
                    actions = actions(),
                )
            }
        }
        compose.onNodeWithText("FIN.").assertExists()
        compose.onNodeWithText("Hold to mark as done").assertDoesNotExist()
    }

    @Test
    fun status_chip_changes_status() {
        val statuses = mutableListOf<MediaStatus>()
        compose.setContent {
            OurshelfTheme {
                DetailScreen(
                    state = DetailUiState.Ready(item("1", "Dune", type = MediaType.BOOK), emptyList(), emptySet()),
                    sharedKey = "test",
                    onBack = {},
                    actions = actions(onStatus = { statuses += it }),
                )
            }
        }
        compose.onNodeWithText("Abandoned").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(MediaStatus.ABANDONED), statuses) }
    }

    @Test
    fun lounge_renders_sections_shelves_and_opens_shelf() {
        val opened = mutableListOf<String>()
        val shelf = Shelf(MediaCollection("c1", "Rainy weekend", "🌧️", null, 0), listOf(item("2", "Amélie")))
        compose.setContent {
            OurshelfTheme {
                LoungeScreen(
                    state = LoungeUiState(
                        isLoading = false,
                        currentlyUnwinding = listOf(item("1", "Hades", MediaStatus.IN_PROGRESS, MediaType.GAME)),
                        upNext = listOf(item("3", "Arrival")),
                        shelves = listOf(shelf),
                    ),
                    onOpenItem = { _, _ -> },
                    onOpenSearch = {},
                    onOpenShelf = { opened += it },
                    onManageShelves = {},
                    onOpenSettings = {},
                    onCreateShelf = { _, _ -> },
                )
            }
        }
        compose.onNodeWithText("Currently Unwinding").assertIsDisplayed()
        compose.onNodeWithText("Hades").assertExists()
        compose.onNodeWithText("🌧️  Rainy weekend").performClick()
        compose.runOnIdle { assertEquals(listOf("c1"), opened) }
        compose.onNodeWithContentDescription("Amélie").assertExists()
    }

    @Test
    @Config(qualifiers = "en-w320dp-h470dp")
    fun onboarding_text_fits_on_small_screens() {
        compose.setContent {
            OurshelfTheme { OnboardingScreen(onFinish = {}) } }
        compose.onNodeWithText("All your downtime, on one shelf").assertIsDisplayed()
        compose.onNodeWithText("Next").assertIsDisplayed()
    }

    @Test
    fun onboarding_can_be_completed() {
        var finished = false
        compose.setContent {
            OurshelfTheme { OnboardingScreen(onFinish = { finished = true }) } }
        compose.onNodeWithText("All your downtime, on one shelf").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Let’s unwind").performClick()
        compose.runOnIdle { assertTrue(finished) }
    }
}
