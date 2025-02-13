import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class loadStations {

    private static final String NOAA_STATIONS_URL = "https://www1.ncdc.noaa.gov/pub/data/ghcn/daily/ghcnd-stations.txt";
    private static List<Station> stations; //Werden die nur einmal geöaden und dann gecached? nochmal prüfen

    public static List<Station> getStations() {
        if (stations == null) {
            stations = loadStationsFromNOAA();
        }
        return stations;
    }

    static List<Station> loadStationsFromNOAA() {
        List<Station> stationsList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(NOAA_STATIONS_URL).openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {

                String stationId = line.substring(0, 11).trim();  // filter id
                double latitude = Double.parseDouble(line.substring(12, 20).trim());  // filter latitidude
                double longitude = Double.parseDouble(line.substring(21, 30).trim()); // filter longitude

                stationsList.add(new Station(stationId, latitude, longitude));
            }
            System.out.println("Geladene Stationen: " + stationsList.size());
        } catch (Exception e) {
            System.err.println("Fehler beim Laden der NOAA-Stationen: " + e.getMessage());
        }
        return stationsList;
    }

    public static void main(String[] args) {
        List<Station> stations = getStations();
        System.out.println("Erste 5 Stationen:");
        stations.stream().limit(5).forEach(System.out::println);
    }
}
