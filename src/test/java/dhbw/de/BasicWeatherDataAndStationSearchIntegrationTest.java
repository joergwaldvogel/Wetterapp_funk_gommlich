package dhbw.de;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UnitandIntegrationtests{

    //Basis-Testwerte für Berlin Mitte
    String stationId = "GME00127850";
    double lat = 52.52;
    double lon = 13.405;
    double radius = 50.0;
    int limit = 10;
    int startYear = 1949;
    int endYear = 2000;

    /**
     * Test: Basis-Suche von Stationen
     */
    @Test
    void testStationSearch() {

        String result = DetermineStationsInRadius.stationSearch(lat, lon, radius, limit, startYear, endYear);
        Assertions.assertNotNull(result, "Das Ergebnis sollte nicht null sein.");
        Assertions.assertFalse(result.isEmpty(), "Das Ergebnis sollte nicht leer sein.");
    }

    /**
     * Test: Basis-Suche von Jährlichen Wetter-Daten einer festgelegten Station
     */
    @Test
    void testFetchAndProcessWeatherDataByYear() {
        // StationSearch zuerst, damit sortedStations nicht null ist
        DetermineStationsInRadius.stationSearch(lat, lon, radius, limit, startYear, endYear);

        String jsonResult = FetchingWeatherData.fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
        Assertions.assertNotNull(jsonResult, "JSON-Ergebnis sollte nicht null sein");
        Assertions.assertFalse(jsonResult.isEmpty(), "JSON-Ergebnis sollte nicht leer sein");

        // Optional: JSON parsen und prüfen, ob expected keys enthalten sind
    }

    /**
     * Test: Basis-Suche von Wetter-Daten nach Jahreszeit einer festgelegten Station
     */

    @Test
    void testFetchAndProcessWeatherDataBySeasons() {
        DetermineStationsInRadius.stationSearch(lat, lon, radius, limit, startYear, endYear);

        String seasonJson = FetchingWeatherData.fetchAndProcessWeatherDataBySeasons(stationId, startYear, endYear);
        Assertions.assertNotNull(seasonJson);
        Assertions.assertFalse(seasonJson.isEmpty());

    }


    /**
     * Test: Radius = 0 (keine Station sollte im Umkreis 0 km liegen, außer exakte Koordinate)
     */
    @Test
    void testStationSearchRadiusZero() {
        double lat = 52.52;
        double lon = 13.405;
        double radius = 0.0;
        int limit = 10;

        String result = DetermineStationsInRadius.stationSearch(lat, lon, radius, limit, startYear, endYear);

        // Evtl. bekommst du ein leeres Array, wenn keine Station exakt auf 52.52, 13.405 liegt
        Assertions.assertNotNull(result);
        // Hier könnte man erwarten, dass result vmtl. "[]" ist, je nachdem ob ihr eine exakte Station in Berlin-Koordinaten habt.
        // Also z.B.:
        Assertions.assertTrue(result.contains("[]") || result.length() < 10,
                "Erwartung: Entweder leeres JSON oder nur eine Station, wenn exakt Koordinate vorhanden");
    }

    /**
     * Test: negative Werte für Limit
     */
    @Test
    void testStationSearchNegativeLimit_shouldThrowException() {
        double lat = 52.52;
        double lon = 13.405;
        double radius = 50.0;
        int limit = -1;

        // Variante: Wir erwarten, dass subList(0, -1) eine IllegalArgumentException wirft.
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DetermineStationsInRadius.stationSearch(lat, lon, radius, limit, endYear, startYear);
        });
    }

    /**
     * Test: StationSearch mit extremen Koordinaten (theoretisch ungültig: lat>90, lon>180).
     */
    @Test
    void testStationSearchInvalidCoordinates() {
        double lat = 9999;
        double lon = 9999;
        double radius = 10.0;
        int limit = 5;

        String result = DetermineStationsInRadius.stationSearch(lat, lon, radius, limit, startYear, endYear);
        Assertions.assertNotNull(result);
        // Erwartung: Vermutlich "[]" oder leere JSON-Liste, weil dort keine Stationen gefunden werden.
        Assertions.assertTrue(result.contains("[ ]") || result.isEmpty(),
                "Erwartung: Keine gültigen Stationen für absurde Koordinaten.");
    }

    /**
     * Test: fetchAndProcessWeatherDataByYear mit 'invalidem' StationId
     * => Falls Station nicht gefunden wird, erhaltet ihr "{}" zurück.
     */
    @Test
    void testFetchWeatherDataInvalidStationId() {

        DetermineStationsInRadius.stationSearch(52.52, 13.405, 50.0, 10, 2000, 2010);

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
        DetermineStationsInRadius.stationSearch(52.52, 13.405, 50.0, 10, 2020, 2000);

        String stationId = "GME00127850";
        int startYear = 2020;
        int endYear = 2000;  // absichtlich kleiner

        String jsonResult = FetchingWeatherData.fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
        Assertions.assertNotNull(jsonResult);

        // Code geht Zeile für Zeile durch und ignoriert, wenn year < startYear ODER year > endYear
        // => Alle Datensätze werden ignoriert => JSON wohl "{}"
        Assertions.assertTrue(jsonResult.contains("{ }") || jsonResult.equals("{ }"),
                "Erwartung: Keine Daten, da 'startYear' > 'endYear'");
    }

    /**
     * Test: fetchAndProcessWeatherDataBySeasons mit realer Station,
     *       aber ein sehr kurzer Zeitraum.
     */
    @Test
    void testFetchWeatherDataBySeasonsShortRange() {
        DetermineStationsInRadius.stationSearch(52.52, 13.405, 50.0, 10, 1950, 1951);

        String stationId = "GME00127850";
        int startYear = 1950;
        int endYear = 1951;  // nur 2 Jahre

        String seasonJson = FetchingWeatherData.fetchAndProcessWeatherDataBySeasons(stationId, startYear, endYear);
        Assertions.assertNotNull(seasonJson);
        Assertions.assertFalse(seasonJson.isEmpty());
    }

    /**
     * Test: fetchAndProcessWeatherDataBySeasons mit Station,
     *       die nicht im R-Tree sortStations enthalten ist => führt zu lat=NaN.
     */
    @Test
    void testFetchWeatherDataSeasonsStationNotInRadius() {
        // Suchen wir Stationen in ganz anderer Region,
        // so dass GME00127850 NICHT in sortedStations landet:
        DetermineStationsInRadius.stationSearch(-89.0, 0.0, 10.0, 5, 1949, 1950);

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

