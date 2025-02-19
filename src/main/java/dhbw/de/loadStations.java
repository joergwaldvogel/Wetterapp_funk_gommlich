package dhbw.de;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
@Service
public class loadStations {
    private static final String NOAA_STATIONS_URL = "https://www1.ncdc.noaa.gov/pub/data/ghcn/daily/ghcnd-stations.txt";
    private static List<Station> stationList; //Werden die nur einmal geöaden und dann gecached? nochmal prüfen
    public record Station(String id, double latitude, double longitude) {}
    public static List<Station> getStations() {
        if (stationList == null) {
            stationList = loadStationsFromNOAA();
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

                stations.add(new Station(stationId, latitude, longitude));
            }
            System.out.println("Geladene Stationen: " + stations.size());
        } catch (Exception e) {
            System.err.println("Fehler beim Laden der NOAA-Stationen: " + e.getMessage());
        }
        return stations;
    }

   /* public static void main(String[] args) {
        List<Station> stations = getStations();
        System.out.println("Erste 5 Stationen:");
        stations.stream().limit(5).forEach(System.out::println);
    }*/
}
