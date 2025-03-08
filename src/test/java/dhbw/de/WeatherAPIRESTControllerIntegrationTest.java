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
                .andExpect(content().string(org.hamcrest.Matchers.containsString("{}")));
        // Oder .containsString("NaN") oder "jahreswerte" leer...
        // Hängt davon ab, wie dein Code reagiert.
    }
}

