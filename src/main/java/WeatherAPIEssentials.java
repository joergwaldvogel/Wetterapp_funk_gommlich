public class WeatherAPIEssentials extends fetchingWeatherData {

    public static void main(String[] args) {
        fetchAndProcessWeatherDataByYear("ACW00011604", 1949, 1951);

        //TODO ZU beachtende Funktionen:
        //  -> Wetter Daten müssen Frontend per JSOn übermittelt werden, visualisierung dann vielleicht per Chart.js
        //      - Jede Station als seperate JSON ausgeben? zusammen? i dunno
        //      - wetterdaten pro jahr und pro meterologischer Jahreszeit (Je Jahreszeit ein Datenpunkt pro Jahr)
        //          *Das als Feld im Frontend beachten
        //      - Die anzahl der zu anzeigenden Stationen soll konfigurierbar sein (max aber 10)
        //  -> Vergleich der Stations ID mit der ghcnd-stations.txt Datei im index, datei runterladen und formatieren???
        //  -> Um wetterdaten zu ermitteln, umkreis berechnen, welche stationen da liegen (Haversine-Formel mit Bounding Box als Vorfilter für effizienz)

    }
}
