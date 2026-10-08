package com.martonegyed.domain.model

data class CsvParseResult(val movies: List<StagedMovie>, val skippedTvCount: Int = 0)
