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

    public static void main(String[] args) {
        String stationId = "AE000041196"; // beispieldaten zum direkt testen
        int startYear = 1949;
        int endYear = 2000;

        String DataByYear = fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
        String DataBySeason = fetchAndProcessWeatherDataBySeasons(stationId, startYear, endYear);

        Map<String, Object> WeatherDataResponse = new HashMap<>();
        WeatherDataResponse.put("DataByYear", DataByYear);
        WeatherDataResponse.put("DataBySeason", DataBySeason);

        System.out.println(WeatherDataResponse);
    }

    public static String fetchAndProcessWeatherDataByYear(String stationId, int startYear, int endYear) {
        String fileUrl = BASE_URL + stationId + ".csv.gz"; // GZIP-Datei
        Map<Integer, List<Double>> minTempsByYear = new HashMap<>();
        Map<Integer, List<Double>> maxTempsByYear = new HashMap<>();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
            connection.setRequestMethod("GET");

            InputStream gzipStream = new GZIPInputStream(connection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(","); // CSV-Datei is komma-separiert
                if (parts.length < 4) continue;

                // debugging
                //System.out.println("Zeile gelesen: " + line);

                try {
                    int year = Integer.parseInt(parts[1].substring(0, 4)); // Jahr aus Datum ziehen
                    if (year < startYear || year > endYear)
                        continue;

                    String recordType = parts[2]; // Temperaturtyp (TMIN oder TMAX gesucht)
                    double tempValue = Double.parseDouble(parts[3]) / 10.0; // Temperatur umrechnen

                    // debugging
                    //System.out.println("Jahr: " + year + ", Typ: " + recordType + ", Wert: " + tempValue);

                    if (recordType.equals("TMIN")) {
                        minTempsByYear.computeIfAbsent(year, k -> new ArrayList<>()).add(tempValue);
                    } else if (recordType.equals("TMAX")) {
                        maxTempsByYear.computeIfAbsent(year, k -> new ArrayList<>()).add(tempValue);
                    }
                } catch (Exception e) {
                    System.out.println("Fehler beim Parsen von: " + line);
                    e.printStackTrace();
                }
            }
            reader.close();
            logger.info("Wetterdaten pro Jahr für "+ stationId +" gesammelt");

            return saveToJson(stationId, startYear, endYear, minTempsByYear, maxTempsByYear);

        } catch (Exception e) {
            e.printStackTrace();
            logger.info("Wetterdaten pro Jahr für "+ stationId +" konnten nicht ermittelt werden");
            return "{}";

        }

    }

    private static String saveToJson(String stationId, int startYear, int endYear,
                                     Map<Integer, List<Double>> minTempsByYear,
                                     Map<Integer, List<Double>> maxTempsByYear) {
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
                yearNode.put("tmin", avgMin);
                yearNode.put("zmax", avgMax);
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

        String[] seasons = {"Frühling", "Sommer", "Herbst", "Winter"};
        for (String season : seasons) {
            minTempsBySeason.put(season, new ArrayList<>());
            maxTempsBySeason.put(season, new ArrayList<>());
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
            connection.setRequestMethod("GET");

            InputStream gzipStream = new GZIPInputStream(connection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream));

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(","); // CSV-Datei ist komma-separiert
                if (parts.length < 4)
                    continue;

                try {
                    int year = Integer.parseInt(parts[1].substring(0, 4));
                    int month = Integer.parseInt(parts[1].substring(4, 6));
                    if (year < startYear || year > endYear)
                        continue;

                    String recordType = parts[2]; // Temperaturtyp (TMIN oder TMAX)
                    double tempValue = Double.parseDouble(parts[3]) / 10.0; // Temperatur umrechnen

                    Double lat = getLatitudeByStationId(stationId);
                    String season = getSeason(month, lat);

                    if (recordType.equals("TMIN")) {
                        minTempsBySeason.get(season).add(tempValue);
                    } else if (recordType.equals("TMAX")) {
                        maxTempsBySeason.get(season).add(tempValue);
                    }
                } catch (Exception e) {
                    System.out.println("Fehler beim Parsen von: " + line);
                    e.printStackTrace();
                }
            }

            reader.close();
            logger.info("Wetterdaten pro Jahreszeit für " + stationId + " gesammelt");

            return saveSeasonalDataToJson(stationId, startYear, endYear, minTempsBySeason, maxTempsBySeason);

        } catch (Exception e) {
            e.printStackTrace();

            logger.info("Wetterdaten pro Jahreszeit für " + stationId + " konnten nicht ermittelt werden");
            return "{}";

        }
    }

    private static String getSeason(int month, double lat) {
        // Wenn südliche Hemisphäre (Breitengrad negativ), Jahreszeiten umkehren
        if (lat < 0) {
            switch (month) {
                case 3:
                case 4:
                case 5:
                    return "Herbst";   // Frühling -> Herbst
                case 6:
                case 7:
                case 8:
                    return "Winter";   // Sommer -> Winter
                case 9:
                case 10:
                case 11:
                    return "Frühling"; // Herbst -> Frühling
                case 12:
                case 1:
                case 2:
                    return "Sommer";   // Winter -> Sommer
                default:
                    return "Unbekannt";
            }
        } else {
            // Nördliche Hemisphäre (Breitengrad positiv oder Äquator)
            switch (month) {
                case 3:
                case 4:
                case 5:
                    return "Frühling";
                case 6:
                case 7:
                case 8:
                    return "Sommer";
                case 9:
                case 10:
                case 11:
                    return "Herbst";
                case 12:
                case 1:
                case 2:
                    return "Winter";
                default:
                    return "Unbekannt";
            }
        }

    }

    public static double getLatitudeByStationId(String stationId) {
        return sortedStations.stream()
                .filter(station -> station.id().equals(stationId))
                .map(LoadStationsFromNOAA.Station::latitude)
                .findFirst()
                .orElse(Double.NaN); // Falls keine Station gefunden wird, NaN zurückgeben
    }

    private static String saveSeasonalDataToJson(String stationId, int startYear, int endYear,
                                                 Map<String, List<Double>> minTempsBySeason,
                                                 Map<String, List<Double>> maxTempsBySeason) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("station", stationId);
            rootNode.put("zeitraum", startYear + "-" + endYear);

            ObjectNode seasonalData = objectMapper.createObjectNode();

            for (String season : minTempsBySeason.keySet()) {
                double min = minTempsBySeason.get(season)
                        .stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
                double max = maxTempsBySeason.get(season)
                        .stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);

                ObjectNode seasonNode = objectMapper.createObjectNode();
                seasonNode.put("tmin", min);
                seasonNode.put("tmax", max);

                seasonalData.set(season, seasonNode);
            }

            rootNode.set("saisonwerte", seasonalData);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("seasonal_weather_data.json"), rootNode);

            System.out.println("Daten pro Jahreszeit gespeichert in seasonal_weather_data.json");

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

        } catch (Exception e) {
            e.printStackTrace();
            return "[]"; // Leeres JSON-Array als Fallback
        }
    }

}
