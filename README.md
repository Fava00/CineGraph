# CineGraph

Egy Kotlin Multiplatform alkalmazás filmes statisztikák követésére, adatok vizualizálására és új filmek felfedezésére.
> Ez a projekt a Budapesti Műszaki és Gazdaságtudományi Egyetem (BME) MSc Önálló laboratórium 1. illetve 2. tárgyak keretein belül készült. Támogatott platformok: Android, iOS és Desktop.

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

## Önálló laboratórium 2 funkciók

0. **Adatmodell, Supabase-integráció** (10 óra)
   
  Repository réteg kialakítása, hogy a UI réteg számára ne számítson, hogy a lokális vagy szerver oldali adatbázisból érkezik az adat.
   
1. **Felhasználói fiók, többeszközös adattárolás** (20-25 óra)

   A felhasználónak képes regisztrálni, bejelentkezni, majd másik eszközön ugyanazzal a CineGraph-fiókkal hozzáférhet a filmjeihez, értékeléseihez, watchlistjéhez és később a listáihoz.

2. **Manuális film hozzáadás, filmnapló-kezelés, filmkeresése TMDB API-n direkten** (25-35 óra)

   Lehetősége van a felhasználónak nem csak a saját, már hozzáadott filmjei között keresni, hanem a TMDB API-ját kihasználva, minden ott elérhető film között. Erre épít a további másik két funkció. A manuális film hozzáadás alatt a felhasználó hozzáadhat olyan filmet amit látott, és/vagy értékelni szeretne vagy akár csak a megnézendő listájára tenne. Ezt korábban csak a CSV fájlokon keresztül volt lehetősége.
   A filmnapló-kezelés funkció teszi lehetővé majd a felhasználónak a már adatbázisában szereplő filmjének újabb naplóbejegyzését, vagyis megjelölheti, hogy újból látta, esetleg új értékelést helyezne rá vagy megnézendő listára helyezi azt.

3. **Közösségi alapfunkciók** (20-30 óra)

   - Felhasználó keresés:
     
     A felhasználókat a felhasználónevük azonosítja, mely segítségével egy másik felhasználó megkeresheti őket.
   - Más felhasználó profiljának megtekintése:
     
     A felhasználók miután megkeresték a másik felhasználót megtekinthetik annak profilját. Itt alapvető statisztikákat érhetnek el az adott felhasználókról( illetve, ha barátként szerepelnek, akkor annak megnézett, értékelt és megnézendő listás filmjeit is megtekinthetik.)
     
   - Barátjelölés:
  
     A felhasználóknak lehetősége van egy mésik felhasználót bejelölni barátként, illetve egy ilyen jelölést elfogadni, melynek segítségével más funkcionalitási lehetőségek megnyílnak számára, mint például az előbb említett.

4. **Értékelés összehasonlítása** (10-15 óra)

    A felhasználók a barátaik értékeléseit egy adott filmnek a profilján is láthatják, illetve azt, ha a barátjuk megnézendő listára rakta, vagy csak szimplán látta, de nem értékelte. Ezenkívül összehasonlíthatja a kettejük által értékelt filmeket, melyek a legnagyobb eltérések, egyezések, valamint mely filmek szerepelnek mindkettejük megnézendő listáján.

5. **Csoportos filmválasztás** (30-40 óra)

    A felhasználóknak lehetősége van közös filmválasztásra. Az egyik felhasználó létrehoz egy szobát melybe barátait meghívhatja közvetlenül egy gombbal vagy akár egy kód megosztásával is.
    Több mód között is választhat:
     - minden felhasználó ajánl X db filmet
     - A CineGraph generál X db filmet a felhasználók megnézendő listájáról
     - A CineGraph a beállított paraméterek alapján ajánl X db filmet a felhasználóknak
     - 
    Az alkalmazásban már máshol látott csúsztatós módszer segítségével dönthetnek a felhasználók, hogy megnéznék, vagy semlegesek, vagy nem néznék meg az adott filmet, majd ez alapján az alkalmazás eldönti mely filmet nézzék a felhasználók.
    
    Ezen funkcióhoz a Supabase Realtime-ot tervezem használni, melynek segítségével a résztvevők rögtön láthatják a frissítéseket.

6. **Listák létrehozása** (15-25 óra)

    A felhasználóknak lehetőségük van saját, egyéni listák létrehozására. Ez abban különbözik a megnézendő listától, hogy itt a felhasználó rangsorolhatja is a listákat, valamint tetszőleges tematikában készítheti el, nevet adhat a listának, valamint leírást is készíthet hozzá. Más felhasználó is láthatja, valamint kedvelheti is a listát. (Későbbi változatban más felhasználóval együtt kezelheti)

7. /+1 Ajánlórendszer (25-35 óra)

    Extra funkcióként egy ajánlórendszer is bekerül az alkalmazásba, mely a felhasználók számára a már megnézett, értékelt, megnézendő listás filmjei alapján ajánl filmeket. Ez a funkció a csoportos filmválasztásba (is) beépül, és ezt a módot is választhatják a felhasználók.


# English version
A Kotlin Multiplatform application for tracking movie statistics, discovering new films, etc.
> This project is mainly created for the MSc Independent Laboratory 1 and 2 courses at Budapesti Műszaki és Gazdságtudományi Egyetem.

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
  
