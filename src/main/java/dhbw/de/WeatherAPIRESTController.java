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

@RestController
@SpringBootApplication(scanBasePackages = "dhbw.de")
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173") //Anfragen vom Frontend
public class WeatherAPIRESTController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherAPIRESTController.class);
    private final DetermineStations stationService;
    private final FetchingWeatherData weatherDataService;

    public WeatherAPIRESTController(@Qualifier("determineStations") DetermineStations stationService,
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

    @GetMapping("/test")
    public String getTest() {
        logger.info("GET-Request auf /api/test empfangen!");
        return "Hello from Backend!";
    }

    @PostMapping("/test")
    public String postTest(@RequestBody String data) {
        logger.info("POST-Request auf /api/test mit Daten: " + data);
        return "Data received: " + data;
    }

    @GetMapping("/get_stations")
    public String getStations(@RequestParam double lat, @RequestParam double lon, @RequestParam double radius) throws IOException {
        logger.info("GET-Request auf /get_station empfangen!");
        return stationService.stationSearch(lat, lon, radius).toString();

        //Path path = Path.of("station_data_test.json");
        //String jsonContent = Files.readString(path);

        //return ResponseEntity.ok(jsonContent);
    }

    @GetMapping("/get_weather_data")
    public String getWeatherData(@RequestParam String stationId, @RequestParam int startYear, @RequestParam int endYear) throws IOException {
        logger.info("GET-Request auf /get_weather_data empfangen!");
        return weatherDataService.fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
        //Path path = Path.of("weather_data_test.json");
        //String jsonContent = Files.readString(path);

        //return ResponseEntity.ok(jsonContent);
    }
}