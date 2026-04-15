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
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.martonegyed.core.util.revenueFormater
import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.Person
import kotlin.math.round
import com.martonegyed.domain.model.SimilarMovie

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
                                color = colors.scrim,
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
            MovieDetailContent(
                movie = m,
                logs = logs,
                isEnriching = isEnriching,
                paddingValues = paddingValues
            )
        }
    }

    @Composable
    private fun MovieDetailContent(
        movie: Movie,
        logs: List<MovieLog>,
        isEnriching: Boolean,
        paddingValues: PaddingValues
    ) {
        val colors = MaterialTheme.colorScheme
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background.copy(0.15f)),
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + 40.dp
            )
        ) {
            item { MovieBackdropSection(movie) }
            item {
                MovieHeroSection(
                    movie,
                    isEnriching
                )
            }
            item { MovieOverviewSection(movie) }

            if (!movie.userReview.isNullOrEmpty()) {
                item { UserReviewSection(movie.userReview!!) }
            }

            if (!movie.actors.isNullOrEmpty()) {
                item { CastSection(movie.actors!!) }
            }


            item {
                CrewSection(
                    directors = movie.directors,
                    crew = movie.crew.orEmpty()
                )
            }


            if (!movie.tmdbReviews.isNullOrEmpty()) {
                item { CommunityReviewsSection(movie.tmdbReviews!!) }
            }

            if (
                !movie.originalLanguage.isNullOrEmpty() ||
                !movie.productionCountries.isNullOrEmpty() ||
                !movie.studios.isNullOrEmpty()
            ) {
                item { ProductionDetailsSection(movie) }
            }

            if (!movie.similarMovies.isNullOrEmpty()) {
                item { SimilarMoviesSection(movie.similarMovies!!) }
            }

            item { LogsSection(logs) }
        }
    }

    @Composable
    fun MovieBackdropSection(movie: Movie) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
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
                            colors = listOf(Color.Transparent, colors.background.copy(0.15f)),
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
        isEnriching: Boolean
    ) {
        val colors = MaterialTheme.colorScheme
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-40).dp)
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(0.66f)
                    .clip(RoundedCornerShape(8.dp))
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

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.padding(top = 40.dp)) {
                Text(
                    movie.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                )

                if (!movie.originalTitle.isNullOrEmpty() && movie.originalTitle != movie.name) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Original: ${movie.originalTitle}",
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                if (!movie.hungarianTitle.isNullOrEmpty() && movie.hungarianTitle != movie.name) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "HU: ${movie.hungarianTitle}",
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                if (!movie.collectionName.isNullOrEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        movie.collectionName!!,
                        color = colors.scrim,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetaTag(movie.year.toString())
                    if (!movie.mpaaRating.isNullOrEmpty()) MetaTag(movie.mpaaRating!!)
                    if (movie.runtimeMinutes != null) {
                        MetaTag("${movie.runtimeMinutes} min")
                    } else if (isEnriching) {
                        MetaTag("...")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun MovieOverviewSection(movie: Movie) {
        val colors = MaterialTheme.colorScheme

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.onBackground.copy(alpha = 0.05f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RatingCol("YOU", movie.rating?.toString() ?: "-", colors.scrim, Icons.Default.Star)
                DividerCol()

                val tmdbScore = movie.tmdbVoteAverage?.let { round(it * 10.0).toString() } ?: "-"
                RatingCol("TMDB", tmdbScore, colors.primary, Icons.Default.Percent)

                if (movie.revenue != null && movie.revenue!! > 0) {
                    DividerCol()
                    FinanceCol(movie.revenue!!)
                }
            }
            if (!movie.tagline.isNullOrEmpty()) {
                Text(
                    movie.tagline!!,
                    fontStyle = FontStyle.Italic,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(12.dp))
            }

            Text(
                movie.overview ?: "No description available.",
                color = colors.onBackground.copy(alpha = 0.7f),
                lineHeight = 22.sp,
                fontSize = 15.sp
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
                            Text(genre, color = colors.onBackground, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    @Composable
    fun UserReviewSection(review: String) {
        val colors = MaterialTheme.colorScheme
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.background.copy(blue = 0.3f))
                    .border(1.dp, colors.onBackground.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "YOUR REVIEW", color = colors.scrim,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        review, color = colors.onBackground,
                        fontSize = 14.sp, lineHeight = 20.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    @Composable
    fun CastSection(cast: List<Person>) {
        val colors = MaterialTheme.colorScheme
        HorizontalDivider(
            color = colors.onBackground.copy(alpha = 0.1f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionTitle("Cast", paddingHorizontal = 16.dp)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cast) { actor -> CastMemberCard(actor) }
        }
        Spacer(Modifier.height(24.dp))


    }

    @Composable
    fun CrewSection(directors: List<Person>, crew: List<Person>) {
        val colors = MaterialTheme.colorScheme
        if (directors.isNotEmpty()) {

            HorizontalDivider(
                color = colors.onBackground.copy(alpha = 0.1f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))
            DirectorRow(directors)
            Spacer(Modifier.height(16.dp))

        }
        if (crew.isEmpty()) {

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                KeyCrewRow("Screenplay", listOf("Screenplay", "Writer", "Story"), crew)
                KeyCrewRow("Cinematography", listOf("Director of Photography"), crew)
            }


            Center {
                OutlinedButton(
                    onClick = { /* TODO: navigate to FullCrewScreen */ },
                    border = ButtonDefaults.outlinedButtonBorder.copy(
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
                        Icons.Default.People, contentDescription = null,
                        tint = colors.scrim, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("See Full Crew", color = colors.onBackground)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    @Composable
    fun CommunityReviewsSection(reviews: List<String>) {
        val colors = MaterialTheme.colorScheme
        HorizontalDivider(
            color = colors.onBackground.copy(alpha = 0.1f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionTitle("Community Reviews", paddingHorizontal = 16.dp)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reviews) { reviewRaw -> ReviewCard(reviewRaw) }
        }
        Spacer(Modifier.height(24.dp))


    }

    @Composable
    fun ProductionDetailsSection(movie: Movie) {
        val colors = MaterialTheme.colorScheme
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionTitle("Production Details")
            Spacer(Modifier.height(8.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val language = movie.originalLanguage
                if (language.isNullOrEmpty())
                    DetailItem("Language", language?.uppercase() ?: "")
                val prodCountry = movie.productionCountries
                if (prodCountry.isNullOrEmpty())
                    DetailItem("Country", prodCountry?.take(2)?.joinToString(", ") ?: "")
                val studios = movie.studios
                if (studios.isNullOrEmpty())
                    DetailItem("Studio", studios?.first() ?: "")
            }
            HorizontalDivider(
                color = colors.onBackground.copy(alpha = 0.1f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

    }


    @Composable
    private fun SimilarMoviesSection(movies: List<SimilarMovie>) {
        val colors = MaterialTheme.colorScheme
        HorizontalDivider(
            color = colors.onBackground.copy(alpha = 0.1f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(16.dp))
        SectionTitle("You Might Also Like", paddingHorizontal = 16.dp)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { similar -> SimilarMovieCard(similar) }
        }
        Spacer(Modifier.height(24.dp))


    }


    @Composable
    private fun LogsSection(logs: List<MovieLog>) {
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
                    "No logs yet.",
                    color = colors.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                logs.forEach { log ->
                    ListItem(
                        headlineContent = {
                            Text(
                                log.watchedDate ?: "Added to Watchlist",
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground
                            )
                        },
                        supportingContent = {
                            if (log.rating != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = colors.scrim,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(" ${log.rating}", color = colors.onBackground, fontSize = 12.sp)
                                }
                            }
                        },
                        leadingContent = {
                            Icon(
                                if (log.isRewatch) Icons.Default.Replay
                                else if (log.watchedDate == null) Icons.Default.Bookmark
                                else Icons.Default.Visibility,
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
    private fun MetaTag(text: String) {
        val colors = MaterialTheme.colorScheme

        Box(
            modifier = Modifier
                .border(1.dp, colors.onBackground.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) { Text(text, color = colors.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }

    @Composable
    private fun SectionTitle(title: String, paddingHorizontal: androidx.compose.ui.unit.Dp = 0.dp) {
        val colors = MaterialTheme.colorScheme

        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold, color = colors.onBackground.copy(alpha = 0.7f), letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(horizontal = paddingHorizontal)
        )
    }

    @Composable
    private fun DetailItem(label: String, value: String) {
        val colors = MaterialTheme.colorScheme

        Column {
            Text(label.uppercase(), fontSize = 10.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.onBackground)
        }
    }

    @Composable
    private fun RatingCol(
        label: String, value: String, color: Color,
        icon: androidx.compose.ui.graphics.vector.ImageVector
    ) {
        val colors = MaterialTheme.colorScheme

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
                Spacer(Modifier.width(4.dp))
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
        }
    }

    @Composable
    private fun FinanceCol(amount: Long) {
        val txt = revenueFormater(amount)
        val colors = MaterialTheme.colorScheme
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BOX OFFICE", fontSize = 10.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(txt, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.scrim)
        }
    }

    @Composable
    private fun DividerCol() {
        val colors = MaterialTheme.colorScheme

        Box(modifier = Modifier.width(1.dp).height(30.dp).background(colors.onBackground.copy(alpha = 0.2f)))
    }

    @Composable
    private fun Center(content: @Composable () -> Unit) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { content() }
    }

    @Composable
    private fun CastMemberCard(person: Person) {
        val colors = MaterialTheme.colorScheme

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(90.dp).clickable { }
        ) {
            Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(colors.surfaceVariant)) {
                if (person.profilePath != null) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w200${person.profilePath}",
                        contentDescription = person.name, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Person, contentDescription = null,
                        tint = colors.onBackground.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                person.name ?: "Unknown", color = colors.onBackground, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            if (!person.character.isNullOrEmpty()) {
                Text(
                    person.character, color = colors.onSurfaceVariant, fontSize = 10.sp,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    private fun DirectorRow(directors: List<Person>) {
        val colors = MaterialTheme.colorScheme

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Directed by:", color = colors.onBackground, fontSize = 16.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp)
            )
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End, reverseLayout = true
            ) {
                items(directors) { director ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(start = 16.dp).clickable { }) {
                        Box(
                            modifier = Modifier.size(50.dp).clip(CircleShape)
                                .border(2.dp, colors.onBackground, CircleShape).background(colors.surfaceVariant)
                        ) {
                            if (director.profilePath != null) {
                                AsyncImage(
                                    model = "https://image.tmdb.org/t/p/w200${director.profilePath}",
                                    contentDescription = director.name, contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            director.name ?: "", color = colors.onBackground, fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun KeyCrewRow(label: String, jobs: List<String>, crew: List<Person>) {
        val colors = MaterialTheme.colorScheme

        val matches = crew.filter { it.job in jobs }
        if (matches.isEmpty()) return
        val names = matches.mapNotNull { it.name }.toSet().joinToString(", ")
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                label, color = colors.onSurfaceVariant, fontSize = 13.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp)
            )
            Text(
                names, color = colors.onBackground, fontSize = 13.sp,
                modifier = Modifier.weight(1f), lineHeight = 18.sp
            )
        }
    }

    @Composable
    private fun ReviewCard(reviewRaw: String) {
        val splitIndex = reviewRaw.indexOf(": ")
        val author = if (splitIndex != -1) reviewRaw.substring(0, splitIndex) else "Unknown"
        val content = if (splitIndex != -1) reviewRaw.substring(splitIndex + 2) else reviewRaw
        val colors = MaterialTheme.colorScheme
        var showDialog by remember { mutableStateOf(false) }
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                containerColor = Color(0xFF2c3440),
                title = { Text(author, color = colors.onBackground) },
                text = {
                    Text(content, color = colors.onBackground.copy(alpha = 0.7f), lineHeight = 21.sp)
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Close", color = colors.primary)
                    }
                }
            )
        }

        Box(
            modifier = Modifier.width(280.dp).height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF202020))
                .border(1.dp, colors.onBackground.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .clickable { showDialog = true }
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(colors.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person, contentDescription = null,
                            tint = colors.background, modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        author, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = colors.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(
                    color = colors.onBackground.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    content, color = colors.onBackground.copy(alpha = 0.7f), fontSize = 13.sp,
                    lineHeight = 18.sp, maxLines = 4, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Read Full Review", color = colors.primary,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }

    @Composable
    private fun SimilarMovieCard(similar: SimilarMovie) {
        val colors = MaterialTheme.colorScheme
        Column(modifier = Modifier.width(100.dp).clickable { /* TODO: navigate */ }) {
            Box(
                modifier = Modifier.fillMaxWidth().height(150.dp)
                    .clip(RoundedCornerShape(8.dp)).background(colors.surfaceVariant)
            ) {
                if (similar.posterPath != null) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w200${similar.posterPath}",
                        contentDescription = similar.name, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Movie, contentDescription = null,
                        tint = colors.onSurfaceVariant, modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                similar.name ?: "", color = colors.onBackground, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
            if (similar.year != null) {
                Text("${similar.year}", color = colors.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}