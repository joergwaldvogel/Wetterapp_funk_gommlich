package dhbw.de;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.strtree.STRtree;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dhbw.de.FetchingWeatherData.fetchAndProcessWeatherDataBySeasons;
import static dhbw.de.FetchingWeatherData.fetchAndProcessWeatherDataByYear;
import static dhbw.de.WeatherAPIRESTController.logger;



@Service
public class DetermineStationsInRadius extends LoadStationsFromNOAA {

    static List<LoadStationsFromNOAA.Station> sortedStations;

    public static String stationSearch(double lat, double lon, double radius, int limit) {

        List<LoadStationsFromNOAA.Station> stations = LoadStationsFromNOAA.getNOAAStations();
        STRtree stRtree = buildRTree(stations);

        List<LoadStationsFromNOAA.Station> nearbyStations = findStationsInRadius(stRtree, lat, lon, radius);
        sortedStations = sortStationsByDistance(nearbyStations);

        if (sortedStations.size() > limit) {
            sortedStations = sortedStations.subList(0, limit);
        }

        logger.info(nearbyStations.size() + " Stationen innerhalb von " + radius + " km um Koordinaten (" + lat + ", " + lon + "):");
        for (LoadStationsFromNOAA.Station station : nearbyStations) {
            System.out.println(station);
        }

        return saveStationsToJson(sortedStations);

    }


    //  R-Tree erstellen
    private static STRtree buildRTree(List<LoadStationsFromNOAA.Station> stations) {
        STRtree rTree = new STRtree();
        for (LoadStationsFromNOAA.Station station : stations) {
            rTree.insert(new Envelope(station.longitude(), station.longitude(),
                    station.latitude(), station.latitude()), station);
        }
        return rTree;
    }

    // R-Tree durchsuchen
    private static List<LoadStationsFromNOAA.Station> findStationsInRadius(STRtree rTree, double lat, double lon, double radius) {
        double degreeMargin = radius / 111.32; // Convert km to degrees
        Envelope searchEnvelope = new Envelope(lon - degreeMargin, lon + degreeMargin, lat - degreeMargin, lat + degreeMargin);

        @SuppressWarnings("unchecked")
        List<LoadStationsFromNOAA.Station> nearbyStations = rTree.query(searchEnvelope);

        // Haversine filtering
        return nearbyStations.stream()
                .filter(station -> {
                    double distance = haversine(lat, lon, station.latitude(), station.longitude());
                    return distance <= radius;
                })
                .map(station -> {
                    double distance = haversine(lat, lon, station.latitude(), station.longitude());
                    return new LoadStationsFromNOAA.Station(
                            station.id(),
                            station.latitude(),
                            station.longitude(),
                            station.name(),
                            distance
                    );
                })
                .collect(Collectors.toList());
    }
        private static List<LoadStationsFromNOAA.Station> sortStationsByDistance(List<LoadStationsFromNOAA.Station> stations) {
        return stations.stream()
                .sorted(Comparator.comparingDouble(LoadStationsFromNOAA.Station::distance))
                .collect(Collectors.toList());

    }

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

    private static String saveStationsToJson(List<LoadStationsFromNOAA.Station> stations) {
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
/*
    public static void main(String[] args) {
        double searchLatitude = 52.52;
        double searchLongitude = 13.405;
        double searchRadius = 50.0;
        int limit = 10;
        stationSearch(searchLatitude, searchLongitude, searchRadius, limit);

        String stationId = "GME00127850"; // beispieldaten zum direkt testen
        int startYear = 1949;
        int endYear = 2000;

        String DataByYear = fetchAndProcessWeatherDataByYear(stationId, startYear, endYear);
        String DataBySeason = fetchAndProcessWeatherDataBySeasons(stationId, startYear, endYear);

        Map<String, Object> WeatherDataResponse = new HashMap<>();
        WeatherDataResponse.put("DataByYear", DataByYear);
        WeatherDataResponse.put("DataBySeason", DataBySeason);

        System.out.println(WeatherDataResponse);


    }
 */
}

