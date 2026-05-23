package com.martonegyed.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class AdaptiveScaffoldTokens(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val sectionSpacing: Dp,
    val cardCornerRadius: Dp,
    val maxCenteredContentWidth: Dp
)

@Immutable
data class MovieCollectionTokens(
    val minGridItemWidth: Dp,
    val maxPosterWidth: Dp,
    val gridSpacing: Dp,
    val titleFontSize: TextUnit,
    val metaFontSize: TextUnit,
    val countFontSize: TextUnit
)

@Immutable
data class MovieDetailTokens(
    val useTwoPaneLayout: Boolean,
    val heroHeight: Dp,
    val posterWidth: Dp,
    val paneSpacing: Dp,
    val titleFontSize: TextUnit,
    val bodyFontSize: TextUnit,
    val metaFontSize: TextUnit
)

@Immutable
data class ImportScreenTokens(
    val useTwoPaneLayout: Boolean,
    val paneSpacing: Dp,
    val sectionCardPadding: Dp,
    val titleFontSize: TextUnit,
    val bodyFontSize: TextUnit
)

@Immutable
data class StatisticsScreenTokens(
    val useTwoPaneLayout: Boolean,
    val leftPaneWidth: Dp,
    val heroCardMinWidth: Dp,
    val paneSpacing: Dp,
    val sectionCardPadding: Dp,
    val titleFontSize: TextUnit,
    val bodyFontSize: TextUnit,
    val valueFontSize: TextUnit
)

@Immutable
data class AdaptiveTokens(
    val scaffold: AdaptiveScaffoldTokens,
    val movieCollection: MovieCollectionTokens,
    val movieDetail: MovieDetailTokens,
    val importScreen: ImportScreenTokens,
    val statisticsScreen: StatisticsScreenTokens
)

fun adaptiveTokensFor(windowInfo: AdaptiveWindowInfo): AdaptiveTokens {
    return when (windowInfo.widthSizeClass) {
        WindowWidthSizeClass.Compact -> AdaptiveTokens(
            scaffold = AdaptiveScaffoldTokens(
                horizontalPadding = 10.dp,
                verticalPadding = 10.dp,
                sectionSpacing = 16.dp,
                cardCornerRadius = 12.dp,
                maxCenteredContentWidth = 600.dp
            ),
            movieCollection = MovieCollectionTokens(
                minGridItemWidth = 120.dp,
                maxPosterWidth = 120.dp,
                gridSpacing = 12.dp,
                titleFontSize = 14.sp,
                metaFontSize = 12.sp,
                countFontSize = 12.sp
            ),
            movieDetail = MovieDetailTokens(
                useTwoPaneLayout = false,
                heroHeight = 250.dp,
                posterWidth = 100.dp,
                paneSpacing = 0.dp,
                titleFontSize = 26.sp,
                bodyFontSize = 15.sp,
                metaFontSize = 12.sp
            ),
            importScreen = ImportScreenTokens(
                useTwoPaneLayout = false,
                paneSpacing = 0.dp,
                sectionCardPadding = 16.dp,
                titleFontSize = 18.sp,
                bodyFontSize = 14.sp
            ),
            statisticsScreen = StatisticsScreenTokens(
                useTwoPaneLayout = false,
                leftPaneWidth = 0.dp,
                heroCardMinWidth = 150.dp,
                paneSpacing = 0.dp,
                sectionCardPadding = 16.dp,
                titleFontSize = 18.sp,
                bodyFontSize = 14.sp,
                valueFontSize = 18.sp
            )
        )

        WindowWidthSizeClass.Medium -> AdaptiveTokens(
            scaffold = AdaptiveScaffoldTokens(
                horizontalPadding = 20.dp,
                verticalPadding = 16.dp,
                sectionSpacing = 20.dp,
                cardCornerRadius = 14.dp,
                maxCenteredContentWidth = 900.dp
            ),
            movieCollection = MovieCollectionTokens(
                minGridItemWidth = 150.dp,
                maxPosterWidth = 150.dp,
                gridSpacing = 14.dp,
                titleFontSize = 16.sp,
                metaFontSize = 13.sp,
                countFontSize = 13.sp
            ),
            movieDetail = MovieDetailTokens(
                useTwoPaneLayout = false,
                heroHeight = 300.dp,
                posterWidth = 120.dp,
                paneSpacing = 20.dp,
                titleFontSize = 30.sp,
                bodyFontSize = 16.sp,
                metaFontSize = 13.sp
            ),
            importScreen = ImportScreenTokens(
                useTwoPaneLayout = false,
                paneSpacing = 20.dp,
                sectionCardPadding = 18.dp,
                titleFontSize = 20.sp,
                bodyFontSize = 15.sp
            ),
            statisticsScreen = StatisticsScreenTokens(
                useTwoPaneLayout = true,
                leftPaneWidth = 0.dp,
                heroCardMinWidth = 180.dp,
                paneSpacing = 20.dp,
                sectionCardPadding = 18.dp,
                titleFontSize = 20.sp,
                bodyFontSize = 15.sp,
                valueFontSize = 20.sp
            )
        )

        WindowWidthSizeClass.Expanded -> AdaptiveTokens(
            scaffold = AdaptiveScaffoldTokens(
                horizontalPadding = 28.dp,
                verticalPadding = 20.dp,
                sectionSpacing = 24.dp,
                cardCornerRadius = 16.dp,
                maxCenteredContentWidth = 1400.dp
            ),
            movieCollection = MovieCollectionTokens(
                minGridItemWidth = 152.dp,
                maxPosterWidth = 152.dp,
                gridSpacing = 18.dp,
                titleFontSize = 17.sp,
                metaFontSize = 14.sp,
                countFontSize = 14.sp
            ),
            movieDetail = MovieDetailTokens(
                useTwoPaneLayout = true,
                heroHeight = 340.dp,
                posterWidth = 220.dp,
                paneSpacing = 24.dp,
                titleFontSize = 34.sp,
                bodyFontSize = 16.sp,
                metaFontSize = 14.sp
            ),
            importScreen = ImportScreenTokens(
                useTwoPaneLayout = true,
                paneSpacing = 24.dp,
                sectionCardPadding = 20.dp,
                titleFontSize = 20.sp,
                bodyFontSize = 15.sp
            ),
            statisticsScreen = StatisticsScreenTokens(
                useTwoPaneLayout = true,
                leftPaneWidth = 340.dp,
                heroCardMinWidth = 190.dp,
                paneSpacing = 24.dp,
                sectionCardPadding = 20.dp,
                titleFontSize = 22.sp,
                bodyFontSize = 15.sp,
                valueFontSize = 22.sp
            )
        )
    }
}