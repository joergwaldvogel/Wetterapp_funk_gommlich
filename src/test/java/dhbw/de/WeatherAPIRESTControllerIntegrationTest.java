package dhbw.de;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integrationstests für WeatherAPIRESTController.
 * Getestet wird, ob korrekte Responses kommen,
 * wenn bestimmte Parameter gesetzt oder fehlen.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class WeatherAPIRESTControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Korrekte Anfrage an /api/get_stations mit allen Pflichtparametern.
     * Erwartung: HTTP 200 und ein JSON-Array im Body.
     */
    @Test
    void testGetStations_successful() throws Exception {
        mockMvc.perform(
                        get("/api/get_stations")
                                .param("lat", "52.52")
                                .param("lon", "13.405")
                                .param("radius", "50.0")
                                .param("limit", "5")
                                // neu hinzugefügt:
                                .param("startYear", "1949")
                                .param("endYear", "2000")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[")));
    }

    /**
     * Test: Parameter "lon" fehlt -> wir übergeben absichtlich nur lat, radius, limit, startYear, endYear.
     * Erwartung: 400 Bad Request (fehlender RequestParam).
     */
    @Test
    void testGetStations_missingLon() throws Exception {
        mockMvc.perform(
                        get("/api/get_stations")
                                .param("lat", "52.52")
                                // kein lon
                                .param("radius", "50.0")
                                .param("limit", "5")
                                .param("startYear", "1949")
                                .param("endYear", "2000")
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: limit=0, um zu prüfen, ob leeres Array oder Fehler kommt.
     * Hier MUSS startYear und endYear ebenfalls gesetzt werden.
     */
    @Test
    void testGetStations_zeroLimit() throws Exception {
        mockMvc.perform(
                        get("/api/get_stations")
                                .param("lat", "52.52")
                                .param("lon", "13.405")
                                .param("radius", "50.0")
                                .param("limit", "0")
                                .param("startYear", "1949")
                                .param("endYear", "2000")
                )
                .andExpect(status().isOk())
                // Erwartung: leeres Array => "[ ]" oder so
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[ ]")));
    }

    /**
     * Test: Negativer radius => ggf. 400 oder leeres Array,
     * abhängig davon, wie dein Code es handhabt.
     */
    @Test
    void testGetStations_negativeRadius() throws Exception {
        mockMvc.perform(
                        get("/api/get_stations")
                                .param("lat", "52.52")
                                .param("lon", "13.405")
                                .param("radius", "-10.0")
                                .param("limit", "5")
                                .param("startYear", "1949")
                                .param("endYear", "2000")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[ ]")));
    }

    /**
     * Test: Falls wir "startYear" oder "endYear" weglassen, sollte es 400 geben.
     * Hier fehlt endYear absichtlich.
     */
    @Test
    void testGetStations_missingEndYear() throws Exception {
        mockMvc.perform(
                        get("/api/get_stations")
                                .param("lat", "52.52")
                                .param("lon", "13.405")
                                .param("radius", "50.0")
                                .param("limit", "5")
                                .param("startYear", "1949")
                        // endYear fehlt
                )
                .andExpect(status().isBadRequest());
    }

    // --- Tests für /get_weather_data ---

    @Test
    void testGetWeatherData_successful() throws Exception {
        mockMvc.perform(
                        get("/api/get_weather_data")
                                .param("stationId", "GME00127850")
                                .param("startYear", "1949")
                                .param("endYear", "2000")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jahreswerte")));
    }

    @Test
    void testGetWeatherData_invalidYearRange() throws Exception {
        // startYear > endYear => "{}" oder leeres Feld möglich
        mockMvc.perform(
                        get("/api/get_weather_data")
                                .param("stationId", "GME00127850")
                                .param("startYear", "2025")
                                .param("endYear", "2000")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("{ }")));
    }

    /**
     * Fehlender Parameter "stationId".
     */
    @Test
    void testGetWeatherData_missingStationId() throws Exception {
        mockMvc.perform(
                        get("/api/get_weather_data")
                                // .param("stationId", "GME00127850") -> fehlt absichtlich
                                .param("startYear", "1949")
                                .param("endYear", "2000")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetWeatherData_invertedYearRange() throws Exception {
        // startYear > endYear
        mockMvc.perform(
                        get("/api/get_weather_data")
                                .param("stationId", "GME00127850")
                                .param("startYear", "2025")
                                .param("endYear", "2000")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("{ }")));
    }

    @Test
    void testGetWeatherData_emptyStationId() throws Exception {
        mockMvc.perform(
                        get("/api/get_weather_data")
                                .param("stationId", "")  // leer
                                .param("startYear", "1949")
                                .param("endYear", "1950")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("{}")));
    }

    @Test
    void testGetWeatherData_largeYearRange() throws Exception {
        mockMvc.perform(
                        get("/api/get_weather_data")
                                .param("stationId", "GME00127850")
                                .param("startYear", "1900")
                                .param("endYear", "2025")
                )
                .andExpect(status().isOk());
    }
}
