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
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173") // Frontendport
public class WeatherAPIRESTController {
    public static final Logger logger = LoggerFactory.getLogger(WeatherAPIRESTController.class);
    private final DetermineStationsInRadius stationService;
    private final FetchingWeatherData weatherDataService;

    public WeatherAPIRESTController(@Qualifier("determineStationsInRadius") DetermineStationsInRadius stationService,
                                    @Qualifier("fetchingWeatherData") FetchingWeatherData weatherDataService) {
        this.stationService = stationService;
        this.weatherDataService = weatherDataService;
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
        return factory -> factory.setPort(8080);
    }

    @GetMapping("/get_stations")
    public String getStations(@RequestParam double lat, @RequestParam double lon, @RequestParam double radius, @RequestParam int limit) throws IOException {
        logger.info("GET-Request auf /get_station empfangen");
        return stationService.stationSearch(lat, lon, radius, limit).toString();

        // Path path = Path.of("station_data_test.json");
        //String jsonContent = Files.readString(path);

        //return ResponseEntity.ok(jsonContent);
    }

    @GetMapping("/get_seasonal_weather_data")
    public String getSeasonalWeatherData(@RequestParam String stationId, @RequestParam int startYear, @RequestParam int endYear) throws IOException {

        logger.info("GET-Request auf /get_seasonal_weather_data empfangen");
        return weatherDataService.fetchAndProcessWeatherDataBySeasons(stationId, startYear, endYear).toString();

    }

    @GetMapping("/get_weather_data")
    public String getWeatherData(@RequestParam String stationId, @RequestParam int startYear, @RequestParam int endYear) throws IOException {

        logger.info("GET-Request auf /get_weather_data empfangen");
        return weatherDataService.fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);


        //Path path = Path.of("weather_data_test.json");
        // String jsonContent = Files.readString(path);

        //return ResponseEntity.ok(jsonContent);
    }
}