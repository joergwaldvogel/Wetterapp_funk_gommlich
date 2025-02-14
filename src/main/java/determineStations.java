import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.index.kdtree.KdTree;
import org.locationtech.jts.index.kdtree.KdNode;
import org.locationtech.jts.geom.Envelope;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class determineStations extends loadStations {

    public static String stationSearch(double lat, double lon, double radius) { //vordefinierte Koordinaten zum mtesten

        List<Station> stations = getStations();

        //KD-Tree erstellen
        KdTree kdTree = new KdTree();
        for (Station station : stations) {
            kdTree.insert(new Coordinate(station.latitude(), station.longitude()), station);
        }

        List<Station> nearbyStations = findStationsInRadius(kdTree, lat, lon, radius);
        List<Station> sortedStations = sortStationsByDistance(nearbyStations, lat, lon);

        if (sortedStations.size() > 10) {
            sortedStations = sortedStations.subList(0, 10);
        }

        System.out.println(nearbyStations.size() + " Stationen innerhalb von " + radius + " km um (" + lat + ", " + lon + "):");
        for (Station station : nearbyStations) {
            System.out.println(station);
        }

        return saveStationsToJson(sortedStations);

    }

    //KD-Tree radiussuche und manueller Distanzberechnung (da nur rechteck berrechnet werden kann)
    private static List<Station> findStationsInRadius(KdTree kdTree, double lat, double lon, double radius) {
        double radiusInDegrees = radius / 111.32; // Grobe Umrechnung von km in Grad

        Envelope searchEnvelope = new Envelope(
                lon - radiusInDegrees, lon + radiusInDegrees,
                lat - radiusInDegrees, lat + radiusInDegrees
        );

        List<KdNode> nearbyNodes = kdTree.query(searchEnvelope);

        return nearbyNodes.stream()
                .map(node -> (Station) node.getData())
                .filter(station -> haversine(lat, lon, station.latitude(), station.longitude()) <= radius)
                .collect(Collectors.toList());
    }

    private static List<Station> sortStationsByDistance(List<Station> stations, double lat, double lon) {
        return stations.stream()
                .sorted(Comparator.comparingDouble(station -> haversine(lat, lon, station.latitude(), station.longitude())))
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

    private static String saveStationsToJson(List<Station> stations) {
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

 /*   public static void main(String[] args) {
        double searchLatitude = 17.1167;
        double searchLongitude = -61.7833 ;
        double searchRadius = 6000.0;
        stationSearch(searchLatitude, searchLongitude, searchRadius);
    }*/
}

