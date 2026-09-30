package com.markleaf.notes.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColor
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import kotlin.math.min

/**
 * Where a note-open started, so the editor can grow out of exactly that spot and
 * shrink back into it.
 *
 * A screen-specific key namespace (not one shared `note-<id>`) is deliberate: the
 * list and the search results both draw the same notes, and Navigation keeps the
 * outgoing and incoming destinations composed together. With one shared key,
 * opening Search from the list would pair each list row with its search-result
 * twin and start a shared-bounds animation nobody asked for.
 */
internal enum class NoteSource {
    LIST, TILE, SEARCH, ARCHIVE, FAB;

    /**
     * Input: the note being opened. Output: the shared-element key of the
     * source composable.
     * Why: the FAB is drawn before the note exists, so it can only own a fixed
     * key; every other source is one row per note.
     */
    fun keyFor(noteId: String): String = when (this) {
        LIST -> "note-$noteId"
        TILE -> "note-tile-$noteId"
        SEARCH -> "note-search-$noteId"
        ARCHIVE -> "note-archive-$noteId"
        FAB -> FAB_KEY
    }

    /** True when [route] is the destination this source lives on. */
    fun isOn(route: String?): Boolean = when (this) {
        LIST, TILE, FAB -> route == NavRoutes.NOTES
        // The search destination's route carries a `?query={query}` suffix.
        SEARCH -> route?.startsWith(NavRoutes.SEARCH) == true
        ARCHIVE -> route == NavRoutes.ARCHIVE
    }

    /**
     * Input: the active colour scheme. Output: the colour the growing container
     * starts as (and shrinks back to) — the source's own surface.
     *
     * Why the container carries a colour of its own: the source's content and the
     * editor scale by width, so their aspect ratios only agree at the two ends.
     * Between them the opaque editor surface is a different shape from the card
     * on top of it, and against the page a two-tone patch shows. Starting the
     * container in the card's colour makes the two read as one card that becomes
     * the page. Rows without a fill of their own start as the page colour, which
     * is a no-op.
     */
    fun startSurface(scheme: ColorScheme): Color = when (this) {
        TILE -> scheme.surfaceVariant.copy(alpha = TILE_FILL_ALPHA).compositeOver(scheme.background)
        FAB -> scheme.primaryContainer
        LIST, SEARCH, ARCHIVE -> scheme.background
    }

    private companion object {
        const val FAB_KEY = "note-fab"

        /** Mirrors the tile background in `NoteCard` (`surfaceVariant.copy(alpha = 0.35f)`). */
        const val TILE_FILL_ALPHA = 0.35f
    }
}

internal data class NoteOrigin(val noteId: String, val source: NoteSource)

/**
 * Input: the two ends of a navigation (route + `noteId` argument each) and the
 * origin the user just tapped from. Output: true when this navigation is the
 * container transform hop — a note-source screen opening the origin note, or
 * that editor returning to the same source.
 *
 * Why it matches on the origin instead of on routes alone: the container
 * transform only reads as one gesture when the NavHost's own slide is switched
 * off for it. That is only safe when a source really exists to grow out of. A
 * widget or share intent opens the editor with no tapped row, and a wikilink hops
 * editor to editor; both keep the plain slide instead of appearing from nowhere.
 * Pure, so the rule is testable without a NavController.
 */
internal fun isContainerHop(
    initialRoute: String?,
    initialNoteId: String?,
    targetRoute: String?,
    targetNoteId: String?,
    origin: NoteOrigin?
): Boolean {
    if (origin == null) return false
    val opening = origin.source.isOn(initialRoute) &&
        targetRoute == NavRoutes.EDITOR && targetNoteId == origin.noteId
    val closing = initialRoute == NavRoutes.EDITOR && initialNoteId == origin.noteId &&
        origin.source.isOn(targetRoute)
    return opening || closing
}

/**
 * The NavHost-side view of [isContainerHop]: reads both ends of the transition
 * being specced. Shared by the host and the transition test so they cannot drift.
 */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.opensOrClosesNoteContainer(
    origin: NoteOrigin?
): Boolean = isContainerHop(
    initialRoute = initialState.destination.route,
    initialNoteId = initialState.arguments?.getString("noteId"),
    targetRoute = targetState.destination.route,
    targetNoteId = targetState.arguments?.getString("noteId"),
    origin = origin
)

/**
 * Input: the corner radius of the card being opened, the current animated
 * bounds, and the window size. Output: the corner radius to clip with right now.
 *
 * Why derived from the bounds instead of animated separately: the library has no
 * shape animation, and a second animation would have to be kept in step with the
 * bounds spring. Coverage is the smaller of the two axis ratios, so a full-width
 * row (already 100% wide) keeps its corners until it is also tall, and a grid
 * tile has to grow on both axes. The corners reach 0 exactly when the container
 * fills the window, where a rounded edge would show the list behind it.
 */
internal fun containerCornerRadiusPx(
    cardRadiusPx: Float,
    boundsWidth: Float,
    boundsHeight: Float,
    windowWidth: Float,
    windowHeight: Float
): Float {
    if (windowWidth <= 0f || windowHeight <= 0f) return cardRadiusPx
    val coverage = min(boundsWidth / windowWidth, boundsHeight / windowHeight).coerceIn(0f, 1f)
    return cardRadiusPx * (1f - coverage)
}

/**
 * The feel of the note → editor container transform, in one place.
 *
 * Fade timings follow Material's container-transform "fade through": the
 * outgoing content is gone within 90ms and the incoming content arrives after
 * it, so the two are never both legible at once — with no opaque stage between
 * them a growing card would show the list text through the editor.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
internal object NoteTransition {
    const val FADE_OUT_MS = 90
    const val FADE_IN_MS = 210

    /**
     * Softer than the library default (400): the expansion should read as
     * unfolding rather than snapping open. Critically damped on purpose — an
     * underdamped spring would overshoot past the window edge.
     */
    const val BOUNDS_STIFFNESS = 320f

    const val SOURCE_Z_INDEX = 1f

    val boundsTransform = BoundsTransform { _, _ ->
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = BOUNDS_STIFFNESS,
            visibilityThreshold = Rect.VisibilityThreshold
        )
    }

    /**
     * Scale, never remeasure: remeasuring would re-lay-out the whole editor
     * (text field, toolbar, highlighting) on every frame of the transition. Width
     * decides the scale so a full-width row keeps the editor at its natural size
     * and a narrow grid tile zooms it up from a smaller copy.
     */
    val resizeMode: SharedTransitionScope.ResizeMode =
        SharedTransitionScope.ResizeMode.ScaleToBounds(ContentScale.FillWidth, Alignment.Center)

    val outgoing: ExitTransition = fadeOut(tween(FADE_OUT_MS))
    val incoming: EnterTransition = fadeIn(tween(FADE_IN_MS, delayMillis = FADE_OUT_MS))
}

/**
 * Input: nothing (reads the theme, density and window size). Output: an overlay
 * clip whose corners relax from the card's radius to square as the container
 * grows.
 * Why remembered: the clip object is read every frame of the transition, so it
 * must not be rebuilt by recomposition.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun rememberContainerClip(): SharedTransitionScope.OverlayClip {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val cardRadiusPx = MaterialTheme.shapes.medium.topStart.toPx(Size(1f, 1f), density)
    val windowWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val windowHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    return remember(cardRadiusPx, windowWidthPx, windowHeightPx) {
        object : SharedTransitionScope.OverlayClip {
            override fun getClipPath(
                sharedContentState: SharedTransitionScope.SharedContentState,
                bounds: Rect,
                layoutDirection: LayoutDirection,
                density: Density
            ): Path {
                val radius = containerCornerRadiusPx(
                    cardRadiusPx, bounds.width, bounds.height, windowWidthPx, windowHeightPx
                )
                // Absolute coordinates: the overlay clip is applied in the shared
                // transition scope's space, which is where `bounds` lives.
                return Path().apply { addRoundRect(RoundRect(bounds, CornerRadius(radius))) }
            }
        }
    }
}

/**
 * Source half of the container transform: tags this composable as the spot the
 * editor grows out of.
 *
 * Input: the note id and which screen the source lives on. Output: the
 * modifier, or the receiver unchanged when no scope is published (the tablet
 * in-pane editor and previews have nothing to morph through).
 * Why the source sits above the target in the overlay: on the way out its
 * content fades over the already-opaque editor surface, which is what makes the
 * hand-over a cross-fade rather than a cut.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.noteSharedBounds(
    noteId: String,
    source: NoteSource = NoteSource.LIST
): Modifier = sourceSharedBounds(source.keyFor(noteId))

/** The FAB variant of [noteSharedBounds]; it owns a fixed key (see [NoteSource.keyFor]). */
@Composable
internal fun Modifier.fabSharedBounds(): Modifier = sourceSharedBounds(NoteSource.FAB.keyFor(""))

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.sourceSharedBounds(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val visibilityScope = LocalNavAnimatedVisibilityScope.current
    if (sharedScope == null || visibilityScope == null) return this
    val clip = rememberContainerClip()
    return with(sharedScope) {
        this@sourceSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = visibilityScope,
            enter = NoteTransition.incoming,
            exit = NoteTransition.outgoing,
            boundsTransform = NoteTransition.boundsTransform,
            resizeMode = NoteTransition.resizeMode,
            zIndexInOverlay = NoteTransition.SOURCE_Z_INDEX,
            clipInOverlayDuringTransition = clip
        )
    }
}

/**
 * Target half of the container transform: the editor surface that grows out of
 * the source.
 *
 * Input: the key of the source it pairs with, the colour the source starts as,
 * the destination's visibility scope and the editor content. Output: the content wrapped in the shared container,
 * or unwrapped when no shared scope is published.
 *
 * Why the surface and the content are separate layers: the container itself
 * carries no enter/exit, so its opaque background is there from the first frame
 * and masks the list behind it. Only the editor content fades (after the
 * source's own content has gone). Fading the whole container instead would let
 * the list show through mid-transition — the "two screens overlaid" look.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun NoteContainerTarget(
    sharedKey: String,
    startSurface: Color,
    animatedVisibilityScope: AnimatedVisibilityScope,
    content: @Composable () -> Unit
) {
    val sharedScope = LocalSharedTransitionScope.current
    if (sharedScope == null) {
        content()
        return
    }
    val clip = rememberContainerClip()
    val pageColor = MaterialTheme.colorScheme.background
    // Follows the destination's own enter/exit state, so it runs forward on the
    // way in and backward on the way out without a second clock to keep in step.
    val surface by animatedVisibilityScope.transition.animateColor(
        transitionSpec = { tween(NoteTransition.FADE_IN_MS) },
        label = "note container surface"
    ) { state -> if (state == EnterExitState.Visible) pageColor else startSurface }
    with(sharedScope) {
        val sharedState = rememberSharedContentState(key = sharedKey)
        Box(
            Modifier
                .fillMaxSize()
                .sharedBounds(
                    sharedContentState = sharedState,
                    animatedVisibilityScope = animatedVisibilityScope,
                    enter = EnterTransition.None,
                    exit = ExitTransition.None,
                    boundsTransform = NoteTransition.boundsTransform,
                    resizeMode = NoteTransition.resizeMode,
                    clipInOverlayDuringTransition = clip
                )
                // Drawn, not `background(surface)`: the colour changes every frame
                // and must not recompose the editor's wrapper. The source's colour
                // only applies while a source is actually paired: the origin note's
                // editor is also re-entered by popping back from a wikilinked note,
                // where nothing is on screen to grow from and a green FAB-coloured
                // surface sliding in would be wrong.
                .drawBehind { drawRect(if (sharedState.isMatchFound) surface else pageColor) }
        ) {
            with(animatedVisibilityScope) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .animateEnterExit(
                            enter = NoteTransition.incoming,
                            exit = NoteTransition.outgoing
                        )
                ) { content() }
            }
        }
    }
}
