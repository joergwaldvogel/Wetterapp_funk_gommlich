package dhbw.de;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import static dhbw.de.WeatherAPIRESTController.logger;

@Service
public class LoadStationsAndInventory {
    private static final String NOAA_STATIONS_URL = "https://www.ncei.noaa.gov/pub/data/ghcn/daily/ghcnd-stations.txt";
    private static List<Station> stationList;
    private static final String INVENTORY_URL = "https://www.ncei.noaa.gov/pub/data/ghcn/daily/ghcnd-inventory.txt";
    public static Map<String, BitSet> inventoryData = new HashMap<>();
    public record Station(String id, double latitude, double longitude, String name, double distance) {}
    @PostConstruct
    public static List<Station> getNOAAStations() {

        if (stationList == null) {
            stationList = loadStationsFromNOAA();
            logger.info("Stationen wurden geladen " + stationList.size());
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

                if (line.length() > 41) {
                    name = line.length() > 71 ? line.substring(41, 71).trim() : line.substring(41).trim();
                } else {
                    name = "Unbekannt";
                }

                double distance = 0.0;

                stations.add(new Station(stationId, latitude, longitude, name, distance));
            }
            System.out.println("Geladene Stationen: " + stations.size());
        } catch (Exception e) {
            System.err.println("Fehler beim Laden der Stationenliste: " + e.getMessage());
        }
        return stations;
    }

    @PostConstruct
    public static Map<String, BitSet> getInventoryData() {

        if (inventoryData.isEmpty()) {
            inventoryData = loadInventoryData();
            logger.info("Inventory-Daten wurden geladen " + inventoryData.size());
        }
        return inventoryData;
    }

    public static Map<String, BitSet> loadInventoryData() {
        Map<String, BitSet> inventory = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(INVENTORY_URL).openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String stationId = line.substring(0, 11).trim();
                String element = line.substring(31, 35).trim();  // TMAX, TMIN, PRCP, etc,

                if (!element.equals("TMAX") && !element.equals("TMIN")) {
                    continue;
                }

                int startYear = Integer.parseInt(line.substring(36, 40).trim());
                int endYear = Integer.parseInt(line.substring(41, 45).trim());

                inventory.computeIfAbsent(stationId, k -> new BitSet()).set(startYear, endYear + 1);
            }
            System.out.println("Inventardaten geladen: " + inventory.size() + " Stationen mit Temperaturwerten.");
        } catch (Exception e) {
            System.err.println("Fehler beim Laden der Inventardaten: " + e.getMessage());
        }
        return inventory;
    }

}

