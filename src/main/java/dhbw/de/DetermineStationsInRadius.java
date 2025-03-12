package dhbw.de;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static dhbw.de.WeatherAPIRESTController.logger;

@Service
public class DetermineStationsInRadius extends LoadStationsAndInventory {
    public static Map<String, BitSet> inventoryData = new HashMap<>();
    static List<LoadStationsAndInventory.Station> sortedStations;

    public static String stationSearch(double lat, double lon, double radius, int limit, int startYear, int endYear) {


        inventoryData = getInventoryData();

        List<LoadStationsAndInventory.Station> stations = LoadStationsAndInventory.getNOAAStations();
        STRtree stRtree = buildRTree(stations);

        List<LoadStationsAndInventory.Station> nearbyStations = findStationsInRadius(stRtree, lat, lon, radius);
        List<LoadStationsAndInventory.Station> filteredStations = filterStationsByDataAvailability(nearbyStations, startYear, endYear);

        sortedStations = sortStationsByDistance(filteredStations);

        if (sortedStations.size() > limit) {
            sortedStations = sortedStations.subList(0, limit);
        }

        logger.info(nearbyStations.size() + " Stationen innerhalb von " + radius + " km um Koordinaten (" + lat + ", " + lon + "):");
        logger.info(filteredStations.size() + " Stationen innerhalb von " + radius + " km um Koordinaten (" + lat + ", " + lon + "), " +
                "                           die Daten im Zeitraum von " + startYear + " und " + endYear + "baufweisen");

        for (LoadStationsAndInventory.Station station : nearbyStations) {
            System.out.println("Unfiltered: " + station);
        }

        for (LoadStationsAndInventory.Station station : filteredStations) {
            System.out.println("Filtered: " + station);
        }

        return saveStationsToJson(sortedStations);

    }


    private static STRtree buildRTree(List<LoadStationsAndInventory.Station> stations) {

        STRtree rTree = new STRtree();
        for (LoadStationsAndInventory.Station station : stations) {
            rTree.insert(new Envelope(station.longitude(), station.longitude(),
                    station.latitude(), station.latitude()), station);
        }
        return rTree;
    }

    private static List<LoadStationsAndInventory.Station> findStationsInRadius(STRtree rTree, double lat, double lon, double radius) {

        double degreeMargin = radius / 111.32;
        Envelope searchEnvelope = new Envelope(lon - degreeMargin, lon + degreeMargin, lat - degreeMargin, lat + degreeMargin);

        @SuppressWarnings("unchecked")
        List<LoadStationsAndInventory.Station> nearbyStations = rTree.query(searchEnvelope);

        return nearbyStations.stream()
                .filter(station -> {
                    double distance = haversine(lat, lon, station.latitude(), station.longitude());
                    return distance <= radius;
                })
                .map(station -> {
                    double distance = haversine(lat, lon, station.latitude(), station.longitude());
                    return new LoadStationsAndInventory.Station(
                            station.id(),
                            station.latitude(),
                            station.longitude(),
                            station.name(),
                            distance
                    );
                })
                .collect(Collectors.toList());
    }
    private static List<LoadStationsAndInventory.Station> filterStationsByDataAvailability(List<LoadStationsAndInventory.Station> stations, int startYear, int endYear) {

        return stations.stream()
                .filter(station -> {
                    BitSet years = inventoryData.get(station.id());
                    return years != null && years.get(startYear) && years.get(endYear);
                })
                .collect(Collectors.toList());
    }
        private static List<LoadStationsAndInventory.Station> sortStationsByDistance(List<LoadStationsAndInventory.Station> stations) {

        return stations.stream()
                .sorted(Comparator.comparingDouble(LoadStationsAndInventory.Station::distance))
                .collect(Collectors.toList());

    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {

        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static String saveStationsToJson(List<LoadStationsAndInventory.Station> stations) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(new File("station_data.json"), stations);

            return objectMapper.writeValueAsString(stations);

        } catch (Exception e) {
            System.err.println("Fehler beim Erstellen der Json der Stationenliste: " + e.getMessage());
            return "{}";
        }
    }
}

