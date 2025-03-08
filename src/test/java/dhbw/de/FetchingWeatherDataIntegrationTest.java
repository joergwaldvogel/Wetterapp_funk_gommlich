package dhbw.de;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FetchingWeatherDataIntegrationTest{

    // --- Bereits vorhandene Tests ---

    @Test
    void testStationSearch() {
        double lat = 52.52;
        double lon = 13.405;
        double radius = 50.0;
        int limit = 10;

        String result = DetermineStationsInRadius.stationSearch(lat, lon, radius, limit);
        Assertions.assertNotNull(result, "Das Ergebnis sollte nicht null sein.");
        Assertions.assertFalse(result.isEmpty(), "Das Ergebnis sollte nicht leer sein.");
        // Optional: JSON parsen, überprüfen ob Stationen enthalten sind
    }

    @Test
    void testFetchAndProcessWeatherDataByYear() {
        // StationSearch zuerst, damit sortedStations nicht null ist
        DetermineStationsInRadius.stationSearch(52.52, 13.405, 50.0, 10);

        String stationId = "GME00127850"; // Beispiel-Station
        int startYear = 1949;
        int endYear = 2000;

        String jsonResult = FetchingWeatherData.fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
        Assertions.assertNotNull(jsonResult, "JSON-Ergebnis sollte nicht null sein");
        Assertions.assertFalse(jsonResult.isEmpty(), "JSON-Ergebnis sollte nicht leer sein");

        // Optional: JSON parsen und prüfen, ob expected keys enthalten sind
    }

    @Test
    void testFetchAndProcessWeatherDataBySeasons() {
        DetermineStationsInRadius.stationSearch(52.52, 13.405, 50.0, 10);

        String stationId = "GME00127850";
        int startYear = 1949;
        int endYear = 2000;

        String seasonJson = FetchingWeatherData.fetchAndProcessWeatherDataBySeasons(stationId, startYear, endYear);
        Assertions.assertNotNull(seasonJson);
        Assertions.assertFalse(seasonJson.isEmpty());
        // Weitere Validierungen
    }

    // --- NEUE TESTS FÜR WEITERE FÄLLE ---

    /**
     * Test: Radius = 0 (keine Station sollte im Umkreis 0 km liegen, außer exakte Koordinate)
     */
    @Test
    void testStationSearchRadiusZero() {
        double lat = 52.52;
        double lon = 13.405;
        double radius = 0.0;
        int limit = 10;

        String result = DetermineStationsInRadius.stationSearch(lat, lon, radius, limit);

        // Evtl. bekommst du ein leeres Array, wenn keine Station exakt auf 52.52, 13.405 liegt
        Assertions.assertNotNull(result);
        // Hier könnte man erwarten, dass result vmtl. "[]" ist, je nachdem ob ihr eine exakte Station in Berlin-Koordinaten habt.
        // Also z.B.:
        Assertions.assertTrue(result.contains("[]") || result.length() < 10,
                "Erwartung: Entweder leeres JSON oder nur eine Station, wenn exakt Koordinate vorhanden");
    }

    /**
     * Test: negative Werte für Limit =>
     * je nach Implementierung kann das ungewollt sein. Schauen, wie euer Code reagiert.
     * Evtl. würdet ihr hier eine Exception oder "[]" erwarten.
     */
    @Test
    void testStationSearchNegativeLimit() {
        double lat = 52.52;
        double lon = 13.405;
        double radius = 50.0;
        int limit = -1;

        String result = DetermineStationsInRadius.stationSearch(lat, lon, radius, limit);
        Assertions.assertNotNull(result, "Sollte nicht null sein - ggf. ein leeres JSON/[] als Fallback.");
        // Je nach Code-Verhalten:
        // Hier könnte man z.B. checken, ob "[]" enthalten ist,
        // wenn negative Limits nicht erlaubt sind.
    }

    /**
     * Test: StationSearch mit extremen Koordinaten (theoretisch ungültig: lat>90, lon>180).
     * Je nach Implementierung sollte euer Code das handhaben, z.B. leere Liste liefern.
     */
    @Test
    void testStationSearchInvalidCoordinates() {
        double lat = 9999;   // ungültige Breite
        double lon = 9999;   // ungültige Länge
        double radius = 10.0;
        int limit = 5;

        String result = DetermineStationsInRadius.stationSearch(lat, lon, radius, limit);
        Assertions.assertNotNull(result);
        // Erwartung: Vermutlich "[]" oder leere JSON-Liste, weil dort keine Stationen gefunden werden.
        Assertions.assertTrue(result.contains("[]") || result.isEmpty(),
                "Erwartung: Keine gültigen Stationen für absurde Koordinaten.");
    }

    /**
     * Test: fetchAndProcessWeatherDataByYear mit 'invalidem' StationId
     * => Falls Station nicht gefunden wird, erhaltet ihr "{}" zurück.
     */
    @Test
    void testFetchWeatherDataInvalidStationId() {
        DetermineStationsInRadius.stationSearch(52.52, 13.405, 50.0, 10);

        String stationId = "FAKE12345678";
        int startYear = 2000;
        int endYear = 2010;

        String jsonResult = FetchingWeatherData.fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
        Assertions.assertNotNull(jsonResult);

        // Laut Code: Falls die Station nicht gefunden wird oder fehlschlägt, return "{}"
        // => Check:
        Assertions.assertTrue(jsonResult.equals("{}") || jsonResult.isEmpty(),
                "Erwartung: {} wenn Station unbekannt ist");
    }

    /**
     * Test: fetchAndProcessWeatherDataByYear mit 'startYear' > 'endYear'.
     * => Code filtert Datensätze. Evtl. kriegt man "{}" oder leeres JSON.
     */
    @Test
    void testFetchWeatherDataYearRangeInverted() {
        DetermineStationsInRadius.stationSearch(52.52, 13.405, 50.0, 10);

        String stationId = "GME00127850";
        int startYear = 2020;
        int endYear = 2000;  // absichtlich kleiner

        String jsonResult = FetchingWeatherData.fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
        Assertions.assertNotNull(jsonResult);

        // Euer Code geht Zeile für Zeile durch und ignoriert, wenn year < startYear ODER year > endYear
        // => Alle Datensätze werden ignoriert => JSON wohl "{}"
        Assertions.assertTrue(jsonResult.contains("{}") || jsonResult.equals("{}"),
                "Erwartung: Keine Daten, da 'startYear' > 'endYear'");
    }

    /**
     * Test: fetchAndProcessWeatherDataBySeasons mit realer Station,
     *       aber ein sehr kurzer Zeitraum.
     */
    @Test
    void testFetchWeatherDataBySeasonsShortRange() {
        DetermineStationsInRadius.stationSearch(52.52, 13.405, 50.0, 10);

        String stationId = "GME00127850";
        int startYear = 1950;
        int endYear = 1951;  // nur 2 Jahre

        String seasonJson = FetchingWeatherData.fetchAndProcessWeatherDataBySeasons(stationId, startYear, endYear);
        Assertions.assertNotNull(seasonJson);
        // Auch hier kann man auf Felder prüfen, z.B. "jahreszeiten" in JSON
        Assertions.assertFalse(seasonJson.isEmpty());
    }

    /**
     * Test: fetchAndProcessWeatherDataBySeasons mit Station,
     *       die nicht im R-Tree sortStations enthalten ist => führt zu lat=NaN.
     *       => getSeasonKeyByHemisphere(...) => nördliche Hemisphere?
     *          Falls lat=NaN => "Unbekannt" oder unhandled?
     *
     * Achtung: um lat != NaN zu bekommen, muss sortedStations die ID kennen.
     * Wenn wir "GME00127850" NICHT in stationSearch radius haben,
     * kann es zu "NaN" kommen.
     */
    @Test
    void testFetchWeatherDataSeasonsStationNotInRadius() {
        // Suchen wir Stationen in ganz anderer Region (Südpol?),
        // so dass GME00127850 NICHT in sortedStations landet:
        DetermineStationsInRadius.stationSearch(-89.0, 0.0, 10.0, 5);

        // Station existiert, taucht aber nicht in sortedStations auf => getLatitudeByStationId => NaN
        String stationId = "GME00127850";
        int startYear = 1949;
        int endYear = 1950;

        String result = FetchingWeatherData.fetchAndProcessWeatherDataBySeasons(stationId, startYear, endYear);
        Assertions.assertNotNull(result);
        // Möglicherweise "{}" oder die Saisons sind alle "Unbekannt"
        // (Code lauft evtl. weiter, lat=NaN => isSouthernHemisphere = false => "Winter_xxxx"...)
    }

}

