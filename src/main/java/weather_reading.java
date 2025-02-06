

import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/weather")
public class weather_reading {

    private static final String NOAA_URL = "https://www1.ncdc.noaa.gov/pub/data/ghcn/daily/all/";

    //Auslese der Wetterdaten aus (testweise) nur einer station per Get-request
    @GetMapping("/station/{stationId}")
    public Map<Integer, Map<String, Double>> getWeatherData(@PathVariable String stationId,
                                                           @RequestParam int startYear,
                                                         @RequestParam int endYear) throws Exception {


        String fileUrl = NOAA_URL + stationId + ".dly";
        HttpURLConnection conn = (HttpURLConnection) new URL(fileUrl).openConnection();
        conn.setRequestMethod("GET");


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return processWeatherData(reader, startYear, endYear);
        }
    }

    private Map<Integer, Map<String, Double>> processWeatherData(BufferedReader reader, int startYear, int endYear) {
        Map<Integer, List<Double>> minTemps = new HashMap<>();
        Map<Integer, List<Double>> maxTemps = new HashMap<>();

        reader.lines().forEach(line -> {
            String yearStr = line.substring(11, 15);
            int year = Integer.parseInt(yearStr);
            if (year >= startYear && year <= endYear) {
                String type = line.substring(17, 21);
                if (type.equals("TMIN") || type.equals("TMAX")) {
                    for (int i = 0; i < 31; i++) {
                        int startIdx = 21 + i * 8;
                        if (startIdx + 5 > line.length()) break;
                        String valueStr = line.substring(startIdx, startIdx + 5).trim();
                        if (!valueStr.equals("-9999")) {
                            double temp = Integer.parseInt(valueStr) / 10.0;
                            if (type.equals("TMIN")) minTemps.computeIfAbsent(year, k -> new ArrayList<>()).add(temp);
                            if (type.equals("TMAX")) maxTemps.computeIfAbsent(year, k -> new ArrayList<>()).add(temp);
                        }
                    }
                }
            }
        });

        return minTemps.keySet().stream().collect(Collectors.toMap(
                year -> year,
                year -> Map.of(
                        "TMIN", minTemps.get(year).stream().min(Double::compare).orElse(Double.NaN),
                        "TMAX", maxTemps.get(year).stream().max(Double::compare).orElse(Double.NaN)
                )
        ));
    }
}
