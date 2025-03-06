package dhbw.de;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DetermineStationsInRadiusIntegrationTest {

    @Test
    void testStationSearch() {
        // Beispielwerte für Berlin
        double lat = 52.52;
        double lon = 13.405;
        double radius = 50.0;
        int limit = 10;

        // Aufruf der statischen Methode
        String result = DetermineStationsInRadius.stationSearch(lat, lon, radius, limit);

        // Erwartung: Irgendein JSON-String sollte zurückkommen
        Assertions.assertNotNull(result, "Das Ergebnis sollte nicht null sein.");
        Assertions.assertFalse(result.isEmpty(), "Das Ergebnis sollte nicht leer sein.");

        // Optional: JSON parsen und z.B. prüfen, ob das Array Stationen enthält
        // ...
    }
    @Test
    void testFetchAndProcessWeatherDataByYear() {
        DetermineStationsInRadius.stationSearch(52.52, 13.405, 50.0, 10);
        String stationId = "GME00127850"; // Beispiel-Station
        int startYear = 1949;
        int endYear = 2000;

        String jsonResult = FetchingWeatherData.fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
        Assertions.assertNotNull(jsonResult, "JSON-Ergebnis sollte nicht null sein");
        Assertions.assertFalse(jsonResult.isEmpty(), "JSON-Ergebnis sollte nicht leer sein");

        // Weitere Checks möglich: z.B. ob bestimmte Keys enthalten sind
        // ...
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

        // Weitere Validierungen, z. B. mit JSON-Parser
        // ...
    }
}
