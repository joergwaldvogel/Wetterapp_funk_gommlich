package dhbw.de;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@SpringBootApplication(scanBasePackages = "dhbw.de")
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173") //Anfragen vom Frontend
public class WeatherAPIRESTController {

    //TODO ZU beachtende, fehlende Funktionen:
    //      - wetterdaten pro jahr und pro meterologischer Jahreszeit (Je Jahreszeit ein Datenpunkt pro Jahr)
    //          *Das als Feld im Frontend beachten
    //      - Die anzahl der zu anzeigenden Stationen soll konfigurierbar sein (max aber 10)
    //  -> welche java version???
    // Ladebalken und eventuell mehr logger

    public static final Logger logger = LoggerFactory.getLogger(WeatherAPIRESTController.class);
    private final DetermineStationsInRadius stationService;
    private final FetchingWeatherData weatherDataService;

    public WeatherAPIRESTController(@Qualifier("determineStationsInRadius") DetermineStationsInRadius stationService,
                                    @Qualifier("fetchingWeatherData") FetchingWeatherData weatherDataService) {
        this.stationService = stationService;
        this.weatherDataService = weatherDataService;
    }

   public static void main(String[] args) {
        SpringApplication.run(dhbw.de.WeatherAPIRESTController.class, args);
    }
    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
        return factory -> factory.setPort(8080);
    }

    /*@GetMapping("/test")
    public String getTest() {
        logger.info("GET-Request auf /api/test empfangen!");
        return "Hello from Backend!";
    }

    @PostMapping("/test")
    public String postTest(@RequestBody String data) {
        logger.info("POST-Request auf /api/test mit Daten: " + data);
        return "Daten erhalten: " + data;
    }
*/
    @GetMapping("/get_stations")
    public String getStations(@RequestParam double lat, @RequestParam double lon, @RequestParam double radius, @RequestParam int limit) throws IOException {
        logger.info("GET-Request auf /get_station empfangen!");
        return stationService.stationSearch(lat, lon, radius, limit).toString();

        // Path path = Path.of("station_data_test.json");
        //String jsonContent = Files.readString(path);

        //return ResponseEntity.ok(jsonContent);
    }

    @GetMapping("/get_weather_data")
    public String getWeatherData(@RequestParam String stationId, @RequestParam int startYear, @RequestParam int endYear) throws IOException {

        //TODO ZU beachtende, fehlende Funktionen:
        // Problem im Code: Latitute der station wird benötigt, nicht der wert, der der benutzer zur suche eingegebenhat
        // Funktion nochmal gründlich Testen
        logger.info("GET-Request auf /get_weather_data empfangen!");
        String DataByYear = weatherDataService.fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
        String DataBySeason = weatherDataService.fetchAndProcessWeatherDataBySeasons(stationId, startYear, endYear);

        Map<String, Object> WeatherDataResponse = new HashMap<>();
        WeatherDataResponse.put("DataByYear", DataByYear);
        WeatherDataResponse.put("DataBySeason", DataBySeason);

        return WeatherDataResponse.toString(); //das wieder einkommentieren, wenn beide datenarten verwendet werden sollen
        //return DataByYear.toString(); //nur daten pro jahr, solang frontend grafik noch nicht angepasst hat

        //Path path = Path.of("weather_data_test.json");
        // String jsonContent = Files.readString(path);

        //return ResponseEntity.ok(jsonContent);
    }
}