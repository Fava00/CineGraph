# CineGraph
Egy Kotlin Multiplatform alkalmazás filmes statisztikák követésére, adatok vizualizálására és új filmek felfedezésére.
> Ez a projekt a Budapesti Műszaki és Gazdaságtudományi Egyetem (BME) MSc Önálló laboratórium 1. tárgyának keretein belül készült. Támogatott platformok: Android, iOS és Web.

## Funkciók

### Adatimport, -export és Adatbővítés:
* **CSV Feldolgozás:**A felhasználók könnyedén importálhatják korábbi filmes adataikat, a Letterboxd vagy az IMDb platformokról származó CSV fájlokból.
* **Adatbővítés (TMDB API):** Az alkalmazás a nyers CSV adatokat a TMDB API segítségével automatikusan kiegészíti részletes információkkal (pl. rendezők, színészek és karaktereik, cselekményleírás, TMDB értékelés, bevételi adatok).
* **Adatexportálás:** A felhasználók kiexportálhatják a kibővített adataikat olyan formátumban, amely visszatölthető a Letterboxd/IMDb rendszerekbe. Emellett lehetőség van egy optimalizált "CineGraph mentés" létrehozására is, amellyel egyetlen fájl segítségével migrálhatók az adatok egy másik eszközre.
### Részletes Analitika 
* **Átfogó Statisztikák:**
  * Összesített filmnézési idő kiszámítása.
  * Személyre szabott rangsorok (átlagos értékelés, megtekintések száma, összesített játékidő, filmek bevétele alapján) színészekre, rendezőkre, országokra, műfajokra és stúdiókra bontva.
* **Interaktív Entitások:** A statisztikákban szereplő színészekre, rendezőkre, országokra vagy stúdiókra kattintva megjelenik az összes hozzájuk kapcsolódó, felhasználó által látott/értékelt film. Ezek a kártyák egyenesen a filmek részletes adatlapjára vezetnek.
* **Szokások és Trendek Elemzése:** Évtizedek szerinti eloszlás, filmes szokások, valamint "Duók" (leggyakrabban együtt dolgozó rendező-színész vagy színész-színész párosok) listázása.
* **CineGraph Wrapped ("A te filmes éved"):** A népszerű zenei "Wrapped" trendhez hasonló funkció, amely látványos, megosztásra kész formátumban foglalja össze a felhasználó adott évi filmnézési statisztikáit és mérföldköveit.
### Filmtár és Megnézendő Lista (Watchlist) Saját Könyvtár: A felhasználó által megtekintett, értékelt, illetve a megnézendő listára (watchlist) helyezett filmek áttekinthető listázása.
* **Részletes Adatlapok:** Egy film kártyájára kattintva teljeskörű információ jelenik meg: rendező, cselekményleírás, stáb (színészek és karakterek), pénzügyi adatok, értékelések (saját és globális), valamint hasonló filmek ajánlása.
### Felfedezés és Ajánlások
* **Watchlist Randomizer:** A felhasználó egy gomb segítségével véletlenszerűen kiválaszthat egy filmet a listájáról.
* **TMDB Okos Ajánló:** A felhasználó a TMDB API segítségével böngészhet/lekérhet filmajánlásokat a beállított beállítások (például évtized, műfaj, már megtekintett filmek stb.) alapján.
* **Közös Munkák Keresője (Crossover Search):** A felhasználó megadhat két vagy több stábtagot (színészt vagy rendezőt), az alkalmazás pedig kilistázza az összes olyan filmet, amelyen ezek a személyek közösen dolgoztak.

# English version
A Kotlin Multiplatform application for tracking movie statistics, discovering new films, etc.
> This project is mainly created for the MSc Independent Laboratory 1 course at Budapesti Műszaki és Gazdságtudományi Egyetem.

#### This application will support Android, iOS, and web browser.

## Features

### Data Import, export
* **CSV Parsing:** The application will be able to import/parse the users' data from Letterboxd's/IMDb's user exported data.
* **Data Enrichment:** The data from the CSV files will be enriched via TMDB's API. The movie's data will be enriched with director(ies), actor(s) and their character(s), description, rating on TMDB, revenue, etc.
* **Flexible Data Export:** The user will be able to export their data, in the form, which the user will be able to import it to letterboxd/IMDb. Additionally, users can generate a unified "CineGraph Backup" file to easily restore or transfer their entire profile to another device.

### Analytics
* **Comprehensive Metrics:**
  * Calculation of total lifetime watch time.
  * Custom rankings based on average rating, total movies watched, total watch time, or box office revenue, categorized by actors, directors, countries, genres, and studios.
* **Interactive Entities:** Clicking on a specific actor, director, country, or studio reveals a dedicated page showcasing all associated movies the user has watched or rated. These interactive movie cards navigate directly to detailed movie profiles.
* **Habits & Trend Analysis:** Statistical breakdowns based on release decades, viewing habits, and frequent "Duos" (e.g., director-actor or actor-actor collaborations).
* **Year in Review (CineGraph Wrapped):** Inspired by the popular "Wrapped" trend, this feature provides a highly visual, engaging, and shareable summary of the user's movie-watching journey throughout the past year.

###Library & Watchlist Management
* **Personal Library:** A clean interface to browse all watched, rated, and watchlisted movies.
* **Detailed Movie Profiles:** Selecting a movie displays its comprehensive data, including the director, plot, full cast and characters, revenue, global and user rating, and similar movies.

### Discovery & Recommendations
* **Watchlist Randomizer:** The user will be able to pick a random movie from their watchlist using a button.

* **Advanced TMDB Discovery:** The user will be able to browse/fetch movie recommendations using the TMDB API based on the preferences the user set(like decade, genre, already watched, etc.)

* **Crossover Search:** This feature will allow users to input two or more actors/directors to discover all movies where those specific individuals collaborated.
  
