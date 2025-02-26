package dhbw.de;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static dhbw.de.WeatherAPIRESTController.logger;

@Service
public class LoadStationsFromNOAA {
    private static final String NOAA_STATIONS_URL = "https://www1.ncdc.noaa.gov/pub/data/ghcn/daily/ghcnd-stations.txt";
    private static List<Station> stationList; //Werden die nur einmal geladen und dann gecached? nochmal prüfen
    public record Station(String id, double latitude, double longitude, String name, double distance) {}
    @PostConstruct
    public static List<Station> getNOAAStations() {
        if (stationList == null) {
            stationList = loadStationsFromNOAA();
            logger.info("Stationen wurden geladen");
        }
        return stationList;
    }
    static List<Station> loadStationsFromNOAA() {
        List<Station> stations = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(NOAA_STATIONS_URL).openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String stationId = line.substring(0, 11).trim();
                double latitude = Double.parseDouble(line.substring(12, 20).trim());
                double longitude = Double.parseDouble(line.substring(21, 30).trim());
                String name;
                //Manchmal sind Zeilen nicht 71 zeilen lang, heißt fehler kommt wenn man versucht den namen zu trimmen
                if (line.length() > 41) {
                    name = line.length() > 71 ? line.substring(41, 71).trim() : line.substring(41).trim();
                } else {
                    name = "Unbekannt";
                }

                double distance = 0.0; //Distanz wird erst später ermittelt, wird vorerst auf 0 gesetzt

                stations.add(new Station(stationId, latitude, longitude, name, distance));
            }
            System.out.println("Geladene Stationen: " + stations.size());
        } catch (Exception e) {
            System.err.println("Fehler beim Laden der NOAA-Stationen: " + e.getMessage());
        }
        return stations;
    }

   public static void main(String[] args) {
        List<Station> stations = getNOAAStations();
        System.out.println("Erste 5 Stationen:");
        stations.stream().limit(5).forEach(System.out::println);
    }
}
