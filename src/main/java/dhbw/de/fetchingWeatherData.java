package dhbw.de;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dhbw.de.determineStations;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;
@Service
public class fetchingWeatherData extends determineStations {
    private static final String BASE_URL = "https://www1.ncdc.noaa.gov/pub/data/ghcn/daily/by_station/";

   /* public static void main(String[] args) {
        String stationId = "AE000041196"; // beispieldaten zum direkt testen
        int startYear = 1949;
        int endYear = 2000;
        fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
    }*/

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

                    // Debugging
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

            return saveToJson(stationId, startYear, endYear, minTempsByYear, maxTempsByYear);

        } catch (Exception e) {
            e.printStackTrace();
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

    public static void fetchAndProcessWeatherDataBySeasons(String stationId, int startYear, int endYear){

    }
}
