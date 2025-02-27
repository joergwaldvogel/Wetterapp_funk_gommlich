package dhbw.de;

import java.util.HashMap;
import java.util.Map;

import static dhbw.de.DetermineStationsInRadius.stationSearch;
import static dhbw.de.FetchingWeatherData.fetchAndProcessWeatherDataBySeasons;
import static dhbw.de.FetchingWeatherData.fetchAndProcessWeatherDataByYear;

public class WeatherAPIEssentials {

    //Test für Backend mit vordefinierten Inputs, damit ich kurz ohne Frontend testen kann
    public static void main(String[] args) {

        double searchLatitude = 52.52;
        double searchLongitude = 13.405;
        double searchRadius = 50.0;
        int limit = 10;
        stationSearch(searchLatitude, searchLongitude, searchRadius, limit);

        String stationId = "GME00127850";
        int startYear = 1949;
        int endYear = 2000;

        String DataByYear = fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
        String DataBySeason = fetchAndProcessWeatherDataBySeasons(stationId, startYear, endYear);

        Map<String, Object> WeatherDataResponse = new HashMap<>();
        WeatherDataResponse.put("DataByYear", DataByYear);
        WeatherDataResponse.put("DataBySeason", DataBySeason);

        System.out.println(WeatherDataResponse);


    }
}
