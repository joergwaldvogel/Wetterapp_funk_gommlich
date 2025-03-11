package dhbw.de;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static dhbw.de.WeatherAPIRESTController.logger;

@Service
public class FetchingWeatherData extends DetermineStationsInRadius {
    private static final String BASE_URL = "https://www1.ncdc.noaa.gov/pub/data/ghcn/daily/by_station/";

    public static String fetchAndProcessWeatherDataByYear(String stationId, int startYear, int endYear) {

        String fileUrl = BASE_URL + stationId + ".csv.gz";
        Map<Integer, List<Double>> minTempsByYear = new HashMap<>();
        Map<Integer, List<Double>> maxTempsByYear = new HashMap<>();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
            connection.setRequestMethod("GET");

            InputStream gzipStream = new GZIPInputStream(connection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 4)
                    continue;

                try {
                    int year = Integer.parseInt(parts[1].substring(0, 4));
                    if (year < startYear || year > endYear)
                        continue;

                    String recordType = parts[2];
                    double tempValue = Double.parseDouble(parts[3]) / 10.0;

                    if (recordType.equals("TMIN")) {
                        minTempsByYear.computeIfAbsent(year, k -> new ArrayList<>()).add(tempValue);
                    } else if (recordType.equals("TMAX")) {
                        maxTempsByYear.computeIfAbsent(year, k -> new ArrayList<>()).add(tempValue);
                    }
                } catch (Exception e) {
                    System.out.println("Fehler beim Parsen: " + line);
                    e.printStackTrace();
                }
            }
            reader.close();

            logger.info("Wetterdaten pro Jahr für "+ stationId +" gesammelt");
            return saveYearlyDataToJson(stationId, startYear, endYear, minTempsByYear, maxTempsByYear);

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("Wetterdaten pro Jahr für "+ stationId +" konnten nicht gesammelt werden");
            return "{}";

        }

    }

    private static String saveYearlyDataToJson(String stationId, int startYear, int endYear, Map<Integer, List<Double>> minTempsByYear, Map<Integer, List<Double>> maxTempsByYear) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("station", stationId);
            rootNode.put("zeitraum", startYear + "-" + endYear);

            ObjectNode yearlyData = objectMapper.createObjectNode();

            for (int year = startYear; year <= endYear; year++) {
                double avgMin = minTempsByYear.getOrDefault(year, Collections.emptyList())
                        .stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                double avgMax = maxTempsByYear.getOrDefault(year, Collections.emptyList())
                        .stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);

                ObjectNode yearNode = objectMapper.createObjectNode();
                yearNode.put("avgMin", avgMin);
                yearNode.put("avgMax", avgMax);
                yearlyData.set(String.valueOf(year), yearNode);
            }

            rootNode.set("jahreswerte", yearlyData);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("weather_data.json"), rootNode);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

        } catch (Exception e) {
            e.printStackTrace();
            return "{}";

        }
    }

    public static String fetchAndProcessWeatherDataBySeasons(String stationId, int startYear, int endYear) {

        String fileUrl = BASE_URL + stationId + ".csv.gz";

        Map<String, List<Double>> minTempsBySeason = new HashMap<>();
        Map<String, List<Double>> maxTempsBySeason = new HashMap<>();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
            connection.setRequestMethod("GET");

            InputStream gzipStream = new GZIPInputStream(connection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream));

            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                if (parts.length < 4)
                    continue;

                try {
                    int year = Integer.parseInt(parts[1].substring(0, 4));
                    int month = Integer.parseInt(parts[1].substring(4, 6));
                    String recordType = parts[2];
                    double tempValue = Double.parseDouble(parts[3]) / 10.0;

                    Double lat = getLatitudeByStationId(stationId);
                    String seasonKey = getSeasonKeyByHemisphere(year, month, lat);

                    if (recordType.equals("TMIN")) {
                        minTempsBySeason.computeIfAbsent(seasonKey, k -> new ArrayList<>()).add(tempValue);
                    } else if (recordType.equals("TMAX")) {
                        maxTempsBySeason.computeIfAbsent(seasonKey, k -> new ArrayList<>()).add(tempValue);
                    }
                } catch (Exception e) {
                    System.out.println("Fehler beim Parsen: " + line);
                    e.printStackTrace();
                }
            }
            reader.close();

            logger.info("Wetterdaten pro Jahreszeit für " + stationId + " gesammelt");
            return saveSeasonDataToJson(stationId, startYear, endYear, minTempsBySeason, maxTempsBySeason);

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("Wetterdaten pro Jahreszeit für "+ stationId +" konnten nicht gesammelt werden");
            return "{}";
        }
    }


    private static String getSeasonKeyByHemisphere(int year, int month, double latitude) {

        if (latitude < 0) {  //Südliche Hemisphäre bei negativer Latitude
            switch (month) {
                case 12:
                    return "Sommer_" + year;
                case 1:
                case 2:
                    return "Sommer_" + year;
                case 3:
                case 4:
                case 5:
                    return "Herbst_" + year;
                case 6:
                case 7:
                case 8:
                    return "Winter_" + year;
                case 9:
                case 10:
                case 11:
                    return "Frühling_" + year;
                default:
                    return "Unbekannt";
            }
        } else {
            switch (month) {
                case 12:
                    return "Winter_" + year;
                case 1:
                case 2:
                    return "Winter_" +  year;
                case 3:
                case 4:
                case 5:
                    return "Frühling_" + year;
                case 6:
                case 7:
                case 8:
                    return "Sommer_" + year;
                case 9:
                case 10:
                case 11:
                    return "Herbst_" + year;
                default:
                    return "Unbekannt";
            }
        }
    }


    public static double getLatitudeByStationId(String stationId) {
        return sortedStations.stream()
                .filter(station -> station.id().equals(stationId))
                .map(LoadStationsAndInventory.Station::latitude)
                .findFirst()
                .orElse(Double.NaN);
    }

    private static String saveSeasonDataToJson(String stationId, int startYear, int endYear, Map<String, List<Double>> minTempsBySeason, Map<String, List<Double>> maxTempsBySeason) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("station", stationId);
            rootNode.put("zeitraum", startYear + "-" + endYear);

            ObjectNode seasonsData = objectMapper.createObjectNode();

            for (String season : minTempsBySeason.keySet()) {
                double avgMin = minTempsBySeason.getOrDefault(season, Collections.emptyList())
                        .stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                double avgMax = maxTempsBySeason.getOrDefault(season, Collections.emptyList())
                        .stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);

                ObjectNode seasonNode = objectMapper.createObjectNode();
                seasonNode.put("avgMin", avgMin);
                seasonNode.put("avgMax", avgMax);

                seasonsData.set(season, seasonNode);
            }

            rootNode.set("jahreszeiten", seasonsData);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("seasonal_weather_data.json"), rootNode);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }

    }

}
