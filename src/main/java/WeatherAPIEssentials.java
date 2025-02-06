

import java.io.IOException;

public class WeatherAPIEssentials extends weather_test_data {

    public static void main(String[] args) {
        try {
            fetchWeatherData("ACW00011604", 2000, 2020);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
