package dhbw.de;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Startet den kompletten SpringBootContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class WeatherAPIRESTControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetStations_successful() throws Exception {
        mockMvc.perform(
                        get("/api/get_stations")
                                .param("lat", "52.52")
                                .param("lon", "13.405")
                                .param("radius", "50.0")
                                .param("limit", "5")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[")));
        // Erwartet, dass mind. ein JSON-Array zurückkommt
        // Ggf. mit jsonPath(...) detailierter prüfen
    }

    @Test
    void testGetStations_missingParam() throws Exception {
        // Beispiel: lat oder lon fehlt
        // => Du kannst prüfen, ob dein Controller eine Fehlerantwort schickt.
        mockMvc.perform(
                        get("/api/get_stations")
                                .param("lat", "52.52")
                                // lon fehlt absichtlich
                                .param("radius", "50.0")
                                .param("limit", "5")
                )
                .andExpect(status().isBadRequest())
        // Oder .andExpect(status().is4xxClientError())
        // falls du in diesem Fall 400 wirfst (wenn du das so konfigurierst)
        ;
    }

    @Test
    void testGetWeatherData_successful() throws Exception {
        // StationId, startYear, endYear
        mockMvc.perform(
                        get("/api/get_weather_data")
                                .param("stationId", "GME00127850")
                                .param("startYear", "1949")
                                .param("endYear", "2000")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jahreswerte")));
        // Bzw. Du weißt, dass DataByYear JSON zurückkommt
        // und im Code "jahreswerte" existiert.
        // Oder checke "DataByYear" je nach Umsetzung.
    }

    @Test
    void testGetWeatherData_invalidYearRange() throws Exception {
        // startYear > endYear?
        mockMvc.perform(
                        get("/api/get_weather_data")
                                .param("stationId", "GME00127850")
                                .param("startYear", "2025")
                                .param("endYear", "2000")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("{ }")));
        // Oder .containsString("NaN") oder "jahreswerte" leer...
        // Hängt davon ab, wie dein Code reagiert.
    }
    // --- NEUE TESTS FÜR FEHLENDE UND EDGE CASES ---

    /**
     * Fehlt z.B. "lon" -> der Controller sollte i.d.R. einen 400-Bad Request werfen,
     * da @RequestParam double lon nicht befüllbar ist.
     */
    @Test
    void testGetStations_missingLon() throws Exception {
        mockMvc.perform(
                        get("/api/get_stations")
                                .param("lat", "52.52")
                                // .param("lon", "13.405")  --> fehlt absichtlich
                                .param("radius", "50.0")
                                .param("limit", "5")
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * Negative Limit -> Kann u.U. zu Fehler führen (z.B. subList(0, -1)).
     * Je nachdem wie Code reagiert, erwarten wir 200 mit leerem Array oder 4xx-Fehler.
     */
    @Test
    void testGetStations_zeroLimit() throws Exception {
        mockMvc.perform(
                        get("/api/get_stations")
                                .param("lat", "52.52")
                                .param("lon", "13.405")
                                .param("radius", "50.0")
                                .param("limit", "0")
                )
                // Erlaubt im Frontend nur eingaben von 0-10
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[ ]")));
    }

    /**
     * Radius negativ -> Auch ein Edge Case (z.B. "radius=-10").
     * Ggf. erwartest du 400-BadRequest, falls das im Code abgefangen wird.
     */
    @Test
    void testGetStations_negativeRadius() throws Exception {
        mockMvc.perform(
                        get("/api/get_stations")
                                .param("lat", "52.52")
                                .param("lon", "13.405")
                                .param("radius", "-10.0")  // negativ
                                .param("limit", "5")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[ ]")));
    }

    /**
     * Fehlender Parameter "stationId" für /get_weather_data.
     * --> Der Controller hat @RequestParam stationId => Muss also 400 werfen.
     */
    @Test
    void testGetWeatherData_missingStationId() throws Exception {
        mockMvc.perform(
                        get("/api/get_weather_data")
                                //.param("stationId", "GME00127850")  // fehlt absichtlich
                                .param("startYear", "1949")
                                .param("endYear", "2000")
                )
                .andExpect(status().isBadRequest());
    }

    /**
     * Inverted Year Range: startYear > endYear
     * --> Code liefert evtl. "{}" oder leere Daten
     */
    @Test
    void testGetWeatherData_invertedYearRange() throws Exception {
        mockMvc.perform(
                        get("/api/get_weather_data")
                                .param("stationId", "GME00127850")
                                .param("startYear", "2025")
                                .param("endYear", "2000")
                )
                .andExpect(status().isOk())
                // In manchen Codes: "{}" oder leere Felder =>
                // matchers.containsString("{}") oder "jahreswerte"
                .andExpect(content().string(org.hamcrest.Matchers.containsString("{ }")));
    }

    /**
     * Leerer stationId => Code kann ggf. "{}" zurückgeben
     * oder 400 werfen, je nach Umsetzung.
     */
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

    /**
     * Sehr großer Zeitraum => Testet Performance oder
     * ob euer Code stabil bleibt (keine Exception).
     */
    @Test
    void testGetWeatherData_largeYearRange() throws Exception {
        mockMvc.perform(
                        get("/api/get_weather_data")
                                .param("stationId", "GME00127850")
                                .param("startYear", "1900")
                                .param("endYear", "2025")
                )
                .andExpect(status().isOk());
        // Evtl. "jahreswerte" im JSON sehr groß;
        // Hier könntest du Timeout-Grenzen oder JSON-Größe checken.
    }
}

