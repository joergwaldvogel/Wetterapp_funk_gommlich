package dhbw.de;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "dhbw.de")
public class WeatherAPIApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeatherAPIApplication.class, args);
    }
}