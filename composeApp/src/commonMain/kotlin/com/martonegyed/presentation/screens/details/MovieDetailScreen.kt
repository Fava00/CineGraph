package com.martonegyed.presentation.screens.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.martonegyed.core.util.revenueFormater
import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.Person
import kotlin.math.round
import com.martonegyed.domain.model.SimilarMovie
import com.martonegyed.core.ui.adaptive.AdaptiveLayout
import com.martonegyed.core.ui.adaptive.AdaptiveScaffoldTokens
import com.martonegyed.core.ui.adaptive.MovieDetailTokens
import com.martonegyed.core.ui.languageDisplayName
import com.martonegyed.presentation.components.common.openPersonCollection
import com.martonegyed.presentation.components.details.FullCrewSheet
import com.martonegyed.presentation.components.common.HorizontalRow
import com.martonegyed.presentation.components.details.MetaTag
import com.martonegyed.presentation.components.details.SectionTitle
import com.martonegyed.presentation.screens.statistics.StatEntityType

data class MovieDetailScreen(val movie: Movie) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val colors = MaterialTheme.colorScheme
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<MovieDetailScreenModel>()
        val movieState by screenModel.movie.collectAsState()
        val logs by screenModel.logs.collectAsState()
        val isEnriching by screenModel.isEnriching.collectAsState()
        val uriHandler = LocalUriHandler.current
        var showMoreMenu by remember { mutableStateOf(false) }
        var showFullCrewSheet by remember { mutableStateOf(false) }

        LaunchedEffect(movie.id) {
            screenModel.init(movie)
        }

        val m = movieState ?: movie

        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
        val isCollapsed = scrollBehavior.state.collapsedFraction > 0.8f

        Scaffold(
            modifier = Modifier
                .background(colors.background)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        AnimatedVisibility(visible = isCollapsed, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                m.name, color = colors.onBackground, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.onBackground
                            )
                        }
                    },
                    actions = {
                        if (isEnriching) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp),
                                color = colors.inversePrimary,
                                strokeWidth = 2.dp
                            )
                        }
                        m.trailerKey?.let { trailerKey ->
                            IconButton(
                                onClick = {
                                    uriHandler.openUri("https://www.youtube.com/watch?v=$trailerKey")
                                }
                            ) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = "Trailer",
                                    tint = colors.error
                                )
                            }
                        }
                        m.imdbId?.let { imdbId ->
                            IconButton(
                                onClick = {
                                    uriHandler.openUri("https://www.imdb.com/title/$imdbId")
                                }
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "IMDb",
                                    tint = colors.primary
                                )
                            }
                        }
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true }
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = colors.onBackground
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                containerColor = colors.surface
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Refresh details", color = colors.onSurface) },
                                    onClick = {
                                        showMoreMenu = false
                                        screenModel.refreshDetails()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Edit", color = colors.onSurface) },
                                    onClick = {
                                        showMoreMenu = false
                                        // navigator.push(EditMovieScreen(m))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = colors.error) },
                                    onClick = {
                                        showMoreMenu = false
                                        screenModel.requestDelete()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = colors.background.copy(blue = 0.15f)
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { paddingValues ->
            AdaptiveLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(colors.background)
            ) { adaptive ->
                MovieDetailContent(
                    movie = m,
                    logs = logs,
                    isEnriching = isEnriching,
                    scaffoldTokens = adaptive.tokens.scaffold,
                    detailTokens = adaptive.tokens.movieDetail,
                    navigator = navigator,
                    onShowFullCrew = { showFullCrewSheet = true }
                )

                if (showFullCrewSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showFullCrewSheet = false },
                        sheetState = rememberModalBottomSheetState(
                            skipPartiallyExpanded = true
                        ),
                        containerColor = colors.surface
                    ) {
                        FullCrewSheet(
                            directors = m.directors,
                            crew = m.crew.orEmpty(),
                            detailTokens = adaptive.tokens.movieDetail,
                            navigator = navigator,
                            onDismiss = { showFullCrewSheet = false }
                        )
                    }
                }
            }

        }
    }

    @Composable
    private fun MovieDetailContent(
        movie: Movie,
        logs: List<MovieLog>,
        isEnriching: Boolean,
        scaffoldTokens: AdaptiveScaffoldTokens,
        detailTokens: MovieDetailTokens,
        navigator: Navigator,
        onShowFullCrew: () -> Unit
    ) {
        val contentModifier = Modifier
            .fillMaxSize()
            .widthIn(max = scaffoldTokens.maxCenteredContentWidth)

        val colors = MaterialTheme.colorScheme

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = 0.15f)),
            contentAlignment = Alignment.TopCenter
        ) {

            if (detailTokens.useTwoPaneLayout) {
                MovieDetailExpandedContent(
                    movie = movie,
                    logs = logs,
                    isEnriching = isEnriching,
                    scaffoldTokens = scaffoldTokens,
                    detailTokens = detailTokens,
                    modifier = contentModifier,
                    navigator = navigator,
                    onShowFullCrew = onShowFullCrew
                )
            } else {
                MovieDetailCompactContent(
                    movie = movie,
                    logs = logs,
                    isEnriching = isEnriching,
                    scaffoldTokens = scaffoldTokens,
                    detailTokens = detailTokens,
                    modifier = contentModifier,
                    navigator = navigator,
                    onShowFullCrew = onShowFullCrew
                )
            }
        }
    }

    @Composable
    private fun MovieDetailCompactContent(
        movie: Movie,
        logs: List<MovieLog>,
        isEnriching: Boolean,
        scaffoldTokens: AdaptiveScaffoldTokens,
        detailTokens: MovieDetailTokens,
        modifier: Modifier = Modifier,
        navigator: Navigator,
        onShowFullCrew: () -> Unit
    ) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(
                bottom = scaffoldTokens.verticalPadding + 40.dp
            )
        ) {
            item { MovieBackdropSection(movie, detailTokens) }
            item { MovieHeroSection(movie, isEnriching, detailTokens, isTwoPane = false) }
            item { MovieOverviewSection(movie, detailTokens) }

            if (!movie.userReview.isNullOrEmpty()) {
                item { UserReviewSection(movie.userReview!!, detailTokens) }
            }

            if (!movie.actors.isNullOrEmpty()) {
                item { CastSection(movie.actors!!, detailTokens, navigator) }
            }

            item {
                CrewSection(
                    directors = movie.directors,
                    crew = movie.crew.orEmpty(),
                    detailTokens = detailTokens,
                    navigator = navigator,
                    onShowFullCrew = onShowFullCrew
                )
            }

            if (!movie.tmdbReviews.isNullOrEmpty()) {
                item { CommunityReviewsSection(movie.tmdbReviews!!, detailTokens) }
            }

            if (
                !movie.originalLanguage.isNullOrEmpty() ||
                !movie.productionCountries.isNullOrEmpty() ||
                !movie.studios.isNullOrEmpty()
            ) {
                item { ProductionDetailsSection(movie, detailTokens) }
            }

            if (!movie.similarMovies.isNullOrEmpty()) {
                item { SimilarMoviesSection(movie.similarMovies!!, detailTokens, navigator) }
            }

            item { LogsSection(logs, detailTokens) }
        }
    }

    @Composable
    private fun MovieDetailExpandedContent(
        movie: Movie,
        logs: List<MovieLog>,
        isEnriching: Boolean,
        scaffoldTokens: AdaptiveScaffoldTokens,
        detailTokens: MovieDetailTokens,
        modifier: Modifier = Modifier,
        navigator: Navigator,
        onShowFullCrew: () -> Unit
    ) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(
                start = scaffoldTokens.horizontalPadding,
                end = scaffoldTokens.horizontalPadding,
                bottom = scaffoldTokens.verticalPadding + 40.dp
            )
        ) {
            item { MovieBackdropSection(movie, detailTokens) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-56).dp),
                    horizontalArrangement = Arrangement.spacedBy(detailTokens.paneSpacing),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxWidth()
                    ) {
                        MovieHeroSection(movie, isEnriching, detailTokens, isTwoPane = true)
                        MovieOverviewSection(movie, detailTokens)

                        if (!movie.userReview.isNullOrEmpty()) {
                            UserReviewSection(movie.userReview!!, detailTokens)
                        }

                        LogsSection(logs, detailTokens)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxWidth()
                    ) {
                        if (!movie.actors.isNullOrEmpty()) {
                            CastSection(movie.actors!!, detailTokens, navigator)
                        }

                        CrewSection(
                            directors = movie.directors,
                            crew = movie.crew.orEmpty(),
                            detailTokens = detailTokens,
                            navigator = navigator,
                            onShowFullCrew = onShowFullCrew
                        )

                        if (!movie.tmdbReviews.isNullOrEmpty()) {
                            CommunityReviewsSection(movie.tmdbReviews!!, detailTokens)
                        }

                        if (
                            !movie.originalLanguage.isNullOrEmpty() ||
                            !movie.productionCountries.isNullOrEmpty() ||
                            !movie.studios.isNullOrEmpty()
                        ) {
                            ProductionDetailsSection(movie, detailTokens)
                        }

                        if (!movie.similarMovies.isNullOrEmpty()) {
                            SimilarMoviesSection(movie.similarMovies!!, detailTokens, navigator)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(1.dp)) }
        }
    }

    @Composable
    fun MovieBackdropSection(
        movie: Movie,
        detailTokens: MovieDetailTokens
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(detailTokens.heroHeight)
        ) {
            val colors = MaterialTheme.colorScheme
            val backdropUrl = movie.backdropPath ?: movie.posterPath

            if (backdropUrl != null) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w780$backdropUrl",
                    contentDescription = "Backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, colors.background.copy(alpha = 0.15f)),
                            startY = 100f
                        )
                    )
            )
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun MovieHeroSection(
        movie: Movie,
        isEnriching: Boolean,
        detailTokens: MovieDetailTokens,
        isTwoPane: Boolean
    ) {
        val colors = MaterialTheme.colorScheme
        val horizontalPadding = if (isTwoPane) 0.dp else 16.dp
        val verticalOffset = if (isTwoPane) 0.dp else (-40).dp
        val topPadding = if (isTwoPane) 8.dp else 40.dp
        val gap = if (isTwoPane) detailTokens.paneSpacing else 16.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .offset(y = verticalOffset),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(detailTokens.posterWidth)
                    .aspectRatio(0.66f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant)
            ) {
                if (movie.posterPath != null) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w300${movie.posterPath}",
                        contentDescription = "Poster",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.width(gap))

            Column(
                modifier = Modifier
                    .padding(top = topPadding)
                    .weight(1f)
            ) {
                Text(
                    text = movie.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = detailTokens.titleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                )

                if (!movie.originalTitle.isNullOrEmpty() && movie.originalTitle != movie.name) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Original: ${movie.originalTitle}",
                        color = colors.onSurfaceVariant,
                        fontSize = detailTokens.metaFontSize
                    )
                }

                if (!movie.hungarianTitle.isNullOrEmpty() && movie.hungarianTitle != movie.name) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "HU: ${movie.hungarianTitle}",
                        color = colors.onSurfaceVariant,
                        fontSize = detailTokens.metaFontSize
                    )
                }

                if (!movie.collectionName.isNullOrEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        movie.collectionName!!,
                        color = colors.inversePrimary,
                        fontSize = detailTokens.metaFontSize
                    )
                }

                Spacer(Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetaTag(movie.year.toString(), detailTokens)
                    if (!movie.mpaaRating.isNullOrEmpty()) MetaTag(movie.mpaaRating!!, detailTokens)
                    if (movie.runtimeMinutes != null) {
                        MetaTag("${movie.runtimeMinutes} min", detailTokens)
                    } else if (isEnriching) {
                        MetaTag("...", detailTokens)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun MovieOverviewSection(
        movie: Movie,
        detailTokens: MovieDetailTokens
    ) {
        val colors = MaterialTheme.colorScheme

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.onBackground.copy(alpha = 0.05f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RatingCol(
                    "YOU",
                    movie.rating?.toString() ?: "-",
                    colors.inversePrimary,
                    Icons.Default.Star,
                    detailTokens
                )
                DividerCol()

                val tmdbScore = movie.tmdbVoteAverage?.let { round(it * 10.0).toString() } ?: "-"
                RatingCol("TMDB", tmdbScore, colors.primary, Icons.Default.Percent, detailTokens)

                if (movie.revenue != null && movie.revenue!! > 0) {
                    DividerCol()
                    FinanceCol(movie.revenue!!, detailTokens)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!movie.tagline.isNullOrEmpty()) {
                Text(
                    movie.tagline!!,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                    fontSize = detailTokens.bodyFontSize
                )
                Spacer(Modifier.height(12.dp))
            }

            Text(
                movie.overview ?: "No description available.",
                color = colors.onBackground.copy(alpha = 0.7f),
                fontSize = detailTokens.bodyFontSize,
                lineHeight = detailTokens.bodyFontSize * 1.45f
            )

            Spacer(Modifier.height(16.dp))

            if (!movie.genres.isNullOrEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    movie.genres!!.forEach { genre ->
                        Box(
                            modifier = Modifier
                                .background(colors.surfaceVariant, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                genre,
                                color = colors.onBackground,
                                fontSize = detailTokens.metaFontSize
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    @Composable
    fun UserReviewSection(
        review: String,
        detailTokens: MovieDetailTokens
    ) {
        val colors = MaterialTheme.colorScheme

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.background.copy(blue = 0.3f))
                    .border(
                        1.dp,
                        colors.onBackground.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "YOUR REVIEW",
                        color = colors.inversePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = detailTokens.metaFontSize
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = review,
                        color = colors.onBackground,
                        fontSize = detailTokens.bodyFontSize,
                        lineHeight = detailTokens.bodyFontSize * 1.4f
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    @Composable
    fun CastSection(
        cast: List<Person>,
        detailTokens: MovieDetailTokens,
        navigator: Navigator
    ) {
        val colors = MaterialTheme.colorScheme

        HorizontalDivider(
            color = colors.onBackground.copy(alpha = 0.1f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionTitle("Cast", paddingHorizontal = 16.dp)
        Spacer(Modifier.height(12.dp))

        HorizontalRow(items = cast) { actor ->
            CastMemberCard(
                person = actor,
                detailTokens = detailTokens,
                navigator = navigator
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    @Composable
    fun CrewSection(
        directors: List<Person>,
        crew: List<Person>,
        detailTokens: MovieDetailTokens,
        navigator: Navigator,
        onShowFullCrew: () -> Unit
    ) {
        val colors = MaterialTheme.colorScheme

        if (directors.isNotEmpty()) {
            HorizontalDivider(
                color = colors.onBackground.copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))
            DirectorRow(
                directors = directors,
                detailTokens = detailTokens,
                navigator = navigator
            )
            Spacer(Modifier.height(16.dp))
        }

        if (crew.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                KeyCrewRow(
                    label = "Screenplay",
                    jobs = listOf("Screenplay", "Writer", "Story"),
                    crew = crew,
                    detailTokens = detailTokens
                )
                KeyCrewRow(
                    label = "Cinematography",
                    jobs = listOf("Director of Photography"),
                    crew = crew,
                    detailTokens = detailTokens
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onShowFullCrew,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = Brush.horizontalGradient(
                            listOf(
                                colors.onBackground.copy(alpha = 0.3f),
                                colors.onBackground.copy(alpha = 0.3f)
                            )
                        )
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = colors.inversePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "See Full Crew",
                        color = colors.onBackground,
                        fontSize = detailTokens.metaFontSize
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    @Composable
    fun CommunityReviewsSection(
        reviews: List<String>,
        detailTokens: MovieDetailTokens
    ) {
        val colors = MaterialTheme.colorScheme

        HorizontalDivider(
            color = colors.onBackground.copy(alpha = 0.1f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionTitle("Community Reviews", paddingHorizontal = 16.dp)
        Spacer(Modifier.height(12.dp))

        HorizontalRow(items = reviews) { reviewRaw ->
            ReviewCard(
                reviewRaw = reviewRaw,
                detailTokens = detailTokens
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun ProductionDetailsSection(
        movie: Movie,
        detailTokens: MovieDetailTokens
    ) {
        val colors = MaterialTheme.colorScheme

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionTitle("Production Details")
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val language = movie.originalLanguage
                if (!language.isNullOrEmpty()) {
                    DetailItem(
                        label = "Language",
                        value = languageDisplayName(language),
                        detailTokens = detailTokens
                    )
                }

                val prodCountry = movie.productionCountries
                if (!prodCountry.isNullOrEmpty()) {
                    DetailItem(
                        label = "Country",
                        value = prodCountry.take(2).joinToString(", "),
                        detailTokens = detailTokens
                    )
                }

                val studios = movie.studios
                if (!studios.isNullOrEmpty()) {
                    DetailItem(
                        label = "Studio",
                        value = studios.first(),
                        detailTokens = detailTokens
                    )
                }
            }

            HorizontalDivider(
                color = colors.onBackground.copy(alpha = 0.1f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }

    @Composable
    private fun SimilarMoviesSection(
        movies: List<SimilarMovie>,
        detailTokens: MovieDetailTokens,
        navigator: Navigator
    ) {
        val colors = MaterialTheme.colorScheme

        HorizontalDivider(
            color = colors.onBackground.copy(alpha = 0.1f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionTitle("You Might Also Like", paddingHorizontal = 16.dp)
        Spacer(Modifier.height(12.dp))

        HorizontalRow(items = movies) { similar ->
            SimilarMovieCard(
                similar = similar,
                detailTokens = detailTokens,
                navigator = navigator
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    @Composable
    private fun LogsSection(
        logs: List<MovieLog>,
        detailTokens: MovieDetailTokens
    ) {
        val colors = MaterialTheme.colorScheme

        Column {
            HorizontalDivider(
                color = colors.onBackground.copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))
            SectionTitle("Your Logs", paddingHorizontal = 16.dp)
            Spacer(Modifier.height(8.dp))

            if (logs.isEmpty()) {
                Text(
                    text = "No logs yet.",
                    color = colors.onSurfaceVariant,
                    fontSize = detailTokens.metaFontSize,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                logs.forEach { log ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = log.watchedDate ?: "Added to Watchlist",
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground,
                                fontSize = detailTokens.bodyFontSize
                            )
                        },
                        supportingContent = {
                            if (log.rating != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = colors.inversePrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = " ${log.rating}",
                                        color = colors.onBackground,
                                        fontSize = detailTokens.metaFontSize
                                    )
                                }
                            }
                        },
                        leadingContent = {
                            Icon(
                                imageVector = when {
                                    log.isRewatch -> Icons.Default.Replay
                                    log.watchedDate == null -> Icons.Default.Bookmark
                                    else -> Icons.Default.Visibility
                                },
                                contentDescription = null,
                                tint = colors.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }
    }


    @Composable
    private fun DetailItem(
        label: String,
        value: String,
        detailTokens: MovieDetailTokens
    ) {
        val colors = MaterialTheme.colorScheme

        Column {
            Text(
                text = label.uppercase(),
                fontSize = detailTokens.metaFontSize,
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = detailTokens.bodyFontSize,
                fontWeight = FontWeight.Medium,
                color = colors.onBackground
            )
        }
    }

    @Composable
    private fun RatingCol(
        label: String,
        value: String,
        color: Color,
        icon: ImageVector,
        detailTokens: MovieDetailTokens
    ) {
        val colors = MaterialTheme.colorScheme

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = detailTokens.metaFontSize,
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = (detailTokens.bodyFontSize.value + 2f).sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
                Spacer(Modifier.width(4.dp))
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
        }
    }

    @Composable
    private fun FinanceCol(amount: Long, detailTokens: MovieDetailTokens) {
        val colors = MaterialTheme.colorScheme
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "BOX OFFICE",
                fontSize = detailTokens.metaFontSize,
                color = colors.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = revenueFormater(amount),
                fontSize = detailTokens.bodyFontSize,
                fontWeight = FontWeight.Bold,
                color = colors.inversePrimary
            )
        }
    }

    @Composable
    private fun DividerCol() {
        val colors = MaterialTheme.colorScheme

        Box(modifier = Modifier.width(1.dp).height(30.dp).background(colors.onBackground.copy(alpha = 0.2f)))
    }

    @Composable
    private fun CastMemberCard(
        person: Person,
        detailTokens: MovieDetailTokens,
        navigator: Navigator
    ) {
        val colors = MaterialTheme.colorScheme
        val avatarSize = if (detailTokens.useTwoPaneLayout) 82.dp else 70.dp
        val cardWidth = if (detailTokens.useTwoPaneLayout) 104.dp else 90.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(cardWidth)
                .clickable {
                    openPersonCollection(
                        navigator = navigator,
                        personName = person.name,
                        entityType = StatEntityType.ACTORS
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(colors.surfaceVariant)
            ) {
                if (person.profilePath != null) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w200${person.profilePath}",
                        contentDescription = person.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = person.name ?: "Unknown",
                color = colors.onBackground,
                fontSize = detailTokens.metaFontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!person.character.isNullOrEmpty()) {
                Text(
                    text = person.character,
                    color = colors.onSurfaceVariant,
                    fontSize = detailTokens.metaFontSize,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    private fun DirectorRow(
        directors: List<Person>,
        detailTokens: MovieDetailTokens,
        navigator: Navigator
    ) {
        val colors = MaterialTheme.colorScheme
        val avatarSize = if (detailTokens.useTwoPaneLayout) 58.dp else 50.dp
        val labelWidth = if (detailTokens.useTwoPaneLayout) 120.dp else 100.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Directed by:",
                color = colors.onBackground,
                fontSize = detailTokens.bodyFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(labelWidth)
            )

            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                reverseLayout = true
            ) {
                items(directors) { director ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .clickable {
                                openPersonCollection(
                                    navigator = navigator,
                                    personName = director.name,
                                    entityType = StatEntityType.DIRECTORS
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(avatarSize)
                                .clip(CircleShape)
                                .border(2.dp, colors.onBackground, CircleShape)
                                .background(colors.surfaceVariant)
                        ) {
                            if (director.profilePath != null) {
                                AsyncImage(
                                    model = "https://image.tmdb.org/t/p/w200${director.profilePath}",
                                    contentDescription = director.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = director.name ?: "",
                            color = colors.onBackground,
                            fontSize = detailTokens.metaFontSize,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun KeyCrewRow(
        label: String,
        jobs: List<String>,
        crew: List<Person>,
        detailTokens: MovieDetailTokens
    ) {
        val colors = MaterialTheme.colorScheme
        val matches = crew.filter { it.job in jobs }
        if (matches.isEmpty()) return

        val names = matches.mapNotNull { it.name }.toSet().joinToString(", ")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                color = colors.onSurfaceVariant,
                fontSize = detailTokens.metaFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(110.dp)
            )
            Text(
                text = names,
                color = colors.onBackground,
                fontSize = detailTokens.bodyFontSize,
                modifier = Modifier.weight(1f),
                lineHeight = detailTokens.bodyFontSize * 1.35f
            )
        }
    }


    @Composable
    private fun ReviewCard(
        reviewRaw: String,
        detailTokens: MovieDetailTokens
    ) {
        val splitIndex = reviewRaw.indexOf(": ")
        val author = if (splitIndex != -1) reviewRaw.substring(0, splitIndex) else "Unknown"
        val content = if (splitIndex != -1) reviewRaw.substring(splitIndex + 2) else reviewRaw
        val colors = MaterialTheme.colorScheme
        var showDialog by remember { mutableStateOf(false) }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { },
                containerColor = Color(0xFF2c3440),
                title = { Text(author, color = colors.onBackground) },
                text = {
                    Text(
                        text = content,
                        color = colors.onBackground.copy(alpha = 0.7f),
                        lineHeight = detailTokens.bodyFontSize * 1.4f,
                        fontSize = detailTokens.bodyFontSize
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Close", color = colors.primary)
                    }
                }
            )
        }

        val cardWidth = if (detailTokens.useTwoPaneLayout) 320.dp else 280.dp
        val cardHeight = if (detailTokens.useTwoPaneLayout) 180.dp else 160.dp

        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF202020))
                .border(1.dp, colors.onBackground.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .clickable { showDialog = true }
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = colors.background,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = author,
                        fontWeight = FontWeight.Bold,
                        fontSize = detailTokens.bodyFontSize,
                        color = colors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(
                    color = colors.onBackground.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    text = content,
                    color = colors.onBackground.copy(alpha = 0.7f),
                    fontSize = detailTokens.metaFontSize,
                    lineHeight = detailTokens.metaFontSize * 1.4f,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Read Full Review",
                    color = colors.primary,
                    fontSize = detailTokens.metaFontSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    private fun SimilarMovieCard(
        similar: SimilarMovie,
        detailTokens: MovieDetailTokens,
        navigator: Navigator
    ) {
        val colors = MaterialTheme.colorScheme
        val cardWidth = if (detailTokens.useTwoPaneLayout) 120.dp else 100.dp
        val posterHeight = if (detailTokens.useTwoPaneLayout) 180.dp else 150.dp

        Column(
            modifier = Modifier
                .width(cardWidth)
                .clickable {
                    navigator.push(
                        MovieDetailScreen(
                            Movie(
                                id = 0,
                                tmdbId = similar.tmdbId,
                                name = similar.name.orEmpty(),
                                year = similar.year ?: 0,
                                posterPath = similar.posterPath,
                                backdropPath = similar.backdropPath,
                                overview = similar.overview,
                                tagline = similar.tagline,
                                runtimeMinutes = similar.runtimeMinutes,
                                originalTitle = similar.originalTitle,
                                originalLanguage = similar.originalLanguage,
                                budget = similar.budget,
                                revenue = similar.revenue,
                                tmdbPopularity = similar.tmdbPopularity,
                                tmdbVoteAverage = similar.tmdbVoteAverage,
                                tmdbVoteCount = similar.tmdbVoteCount,
                                trailerKey = similar.trailerKey,
                                mpaaRating = similar.mpaaRating,
                                imdbId = similar.imdbId,
                                genres = similar.genres,
                                studios = similar.studios,
                                productionCountries = similar.productionCountries,
                                spokenLanguages = similar.spokenLanguages,
                                actors = similar.actors,
                                crew = similar.crew,
                                similarMovies = null,
                                tmdbReviews = null,
                                letterboxdUri = null
                            )
                        )
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(posterHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant)
            ) {
                if (similar.posterPath != null) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w200${similar.posterPath}",
                        contentDescription = similar.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = similar.name ?: "",
                color = colors.onBackground,
                fontSize = detailTokens.metaFontSize,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = detailTokens.metaFontSize * 1.2f
            )

            if (similar.year != null) {
                Text(
                    text = "${similar.year}",
                    color = colors.onSurfaceVariant,
                    fontSize = detailTokens.metaFontSize
                )
            }
        }
    }
}