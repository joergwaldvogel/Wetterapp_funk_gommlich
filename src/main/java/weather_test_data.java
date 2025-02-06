import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class weather_test_data {

    private static final String NOAA_URL = "https://www1.ncdc.noaa.gov/pub/data/ghcn/daily/all/";

    public static void fetchWeatherData(String stationId, int startYear, int endYear) throws IOException {
        String fileUrl = NOAA_URL + stationId + ".dly";
        System.out.println("Lade Daten von: " + fileUrl);

        URL url = new URL(fileUrl);
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

        Map<Integer, Double> minTemps = new HashMap<>();
        Map<Integer, Double> maxTemps = new HashMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            String yearStr = line.substring(11, 15);
            int year = Integer.parseInt(yearStr);

            if (year < startYear || year > endYear) continue;

            String type = line.substring(17, 21);
            if (!type.equals("TMIN") && !type.equals("TMAX")) continue;

            double tempSum = 0;
            int count = 0;

            for (int i = 0; i < 31; i++) {
                String valueStr = line.substring(21 + (i * 8), 26).trim();
                if (!valueStr.equals("-9999")) {
                    double temp = Integer.parseInt(valueStr) / 10.0;
                    tempSum += temp;
                    count++;
                }
            }

            if (count > 0) {
                double avgTemp = tempSum / count;
                if (type.equals("TMIN")) {
                    minTemps.put(year, avgTemp);
                } else if (type.equals("TMAX")) {
                    maxTemps.put(year, avgTemp);
                }
            }
        }
        reader.close();

        writeToFile(stationId, minTemps, maxTemps);
    }

    private static void writeToFile(String stationId, Map<Integer, Double> minTemps, Map<Integer, Double> maxTemps) throws IOException {
        File file = new File(stationId + "_temperature_data.txt");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        writer.write("Jahr\tTMIN\tTMAX\n");
        for (int year : minTemps.keySet()) {
            writer.write(year + "\t" + minTemps.get(year) + "\t" + maxTemps.get(year) + "\n");
        }

        writer.close();
        System.out.println("Daten gespeichert in: " + file.getAbsolutePath());
    }
    public static void main(String[] args) {
        try {
            fetchWeatherData("ACW00011604", 2000, 2020);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
