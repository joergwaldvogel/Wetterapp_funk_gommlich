package dhbw.de;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class DetermineStations extends loadStations {

    public static String stationSearch(double lat, double lon, double radius) { //vordefinierte Koordinaten zum mtesten

        List<loadStations.Station> stations = loadStations.getStations();
        STRtree stRtree = buildRTree(stations);

        List<loadStations.Station> nearbyStations = findStationsInRadius(stRtree, lat, lon, radius);
        List<loadStations.Station> sortedStations = sortStationsByDistance(nearbyStations);

        if (sortedStations.size() > 10) {
            sortedStations = sortedStations.subList(0, 10);
        }

        System.out.println(nearbyStations.size() + " Stationen innerhalb von " + radius + " km um (" + lat + ", " + lon + "):");
        for (loadStations.Station station : nearbyStations) {
            System.out.println(station);
        }

        return saveStationsToJson(sortedStations);

    }


    //  R-Tree erstellen
    private static STRtree buildRTree(List<loadStations.Station> stations) {
        STRtree rTree = new STRtree();
        for (loadStations.Station station : stations) {
            rTree.insert(new Envelope(station.longitude(), station.longitude(),
                    station.latitude(), station.latitude()), station);
        }
        return rTree;
    }

    // R-Tree durchsuchen
    private static List<loadStations.Station> findStationsInRadius(STRtree rTree, double lat, double lon, double radius) {
        double degreeMargin = radius / 111.32; // Convert km to degrees
        Envelope searchEnvelope = new Envelope(lon - degreeMargin, lon + degreeMargin, lat - degreeMargin, lat + degreeMargin);

        @SuppressWarnings("unchecked")
        List<loadStations.Station> nearbyStations = rTree.query(searchEnvelope);

        // Haversine filtering
        return nearbyStations.stream()
                .filter(station -> {
                    double distance = haversine(lat, lon, station.latitude(), station.longitude());
                    return distance <= radius;
                })
                .map(station -> {
                    double distance = haversine(lat, lon, station.latitude(), station.longitude());
                    return new loadStations.Station(
                            station.id(),
                            station.latitude(),
                            station.longitude(),
                            station.name(),
                            distance
                    );
                })
                .collect(Collectors.toList());
    }
        private static List<loadStations.Station> sortStationsByDistance(List<loadStations.Station> stations) {
        return stations.stream()
                .sorted(Comparator.comparingDouble(loadStations.Station::distance))
                .collect(Collectors.toList());

    }


    //Haversine-Formel
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Erdradius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static String saveStationsToJson(List<loadStations.Station> stations) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(new File("station_data.json"), stations);
            return objectMapper.writeValueAsString(stations);
        } catch (Exception e) {
            System.err.println("Fehler beim Erstellen der JSON-Daten: " + e.getMessage());
            return "[]"; // Leeres JSON-Array als Fallback
        }
    }

    public static void main(String[] args) {
        double searchLatitude = 52.52;
        double searchLongitude = 13.405;
        double searchRadius = 100.0;
        stationSearch(searchLatitude, searchLongitude, searchRadius);

    }
}

