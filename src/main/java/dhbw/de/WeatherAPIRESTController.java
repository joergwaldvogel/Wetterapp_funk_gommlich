package dhbw.de;

import dhbw.de.fetchingWeatherData;
import dhbw.de.loadStations;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@RestController
@SpringBootApplication
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:60218") // Erlaubt Anfragen vom Frontend
public class WeatherAPIRESTController {

    private final loadStations stationService = new loadStations();
    private final fetchingWeatherData weatherDataService;

    public WeatherAPIRESTController(fetchingWeatherData weatherDataService) {
        this.weatherDataService = weatherDataService;
    }

   public static void main(String[] args) {
        SpringApplication.run(dhbw.de.WeatherAPIRESTController.class, args);
    }

    @GetMapping("/get_stations")
    public String getStations(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam double radius) {
        return stationService.getStations().toString();
    }

    @GetMapping("/get_weather_data")
    public String getWeatherData(
            @RequestParam String stationId,
            @RequestParam int startYear,
            @RequestParam int endYear) {
        return weatherDataService.fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
    }
}