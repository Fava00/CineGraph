package com.martonegyed.presentation.components.insights
data class GeoCountryShape(
    val name: String,
    val polygons: List<List<Pair<Double, Double>>>
)

fun normalizeCountryName(name: String): String =
    when (name.trim().lowercase()) {
        "usa", "united states" -> "united states of america"
        "uk" -> "united kingdom"
        "czech republic" -> "czechia"
        else -> name.trim().lowercase()
    }

val worldCountryShapes: List<GeoCountryShape> by lazy {
    worldShapesAE() +
        worldShapesFL() +
        worldShapesMR() +
        worldShapesSZ()
}
