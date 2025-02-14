import org.jfree.data.json.impl.JSONArray;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8080") // Erlaubt Anfragen vom Frontend
public class WeatherAPIRESTController {

    private final loadStations stationService = new loadStations();
    private final fetchingWeatherData weatherDataService;

    public WeatherAPIRESTController(fetchingWeatherData weatherDataService) {
        this.weatherDataService = weatherDataService;
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