<script>
  import { onMount } from "svelte";
  import { tick } from "svelte";
  import "leaflet/dist/leaflet.css";
  import L from "leaflet";
  import Chart from "chart.js/auto";

  let stations = [];
  let selectedStation = null;
  let weatherData = null;
  let map;
  let searchCircle = null;
  let lat = 52.52;
  let lon = 13.405;
  let radius = 50;
  let limit = 10;
  let markers = [];
  let startYear = 1949;
  let endYear = 1959;
  let originMarker = null;

  let chartCanvas;
  let myChart = null;

   onMount(() => {
       if (!map) {
           map = L.map("map", {
               center: [lat, lon],
               zoom: 5
           });

           L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
               maxZoom: 19
           }).addTo(map);
       }
       //fetchStations(); //Gibt es einen bestimmten grund weshalb das hier ist? Das sorgt nämlich dafür das bei jedem neuladen der Seite
   });                    // Der code für die stationssuche komplett neu ausgeführt wird ohne das der User den knopf drückt

function updateLimit() {
    if (limit > 10) {
        limit = 10;  // Setzt den Wert auf 10, falls er höher ist
    }
}



   function filterMarkers() {
       markers.forEach(marker => {
           const position = marker.getLatLng();
           const distance = haversine(lat, lon, position.lat, position.lng);
           if (distance > radius * 1000) {
               map.removeLayer(marker);
           }
       });
       markers = markers.filter(marker => map.hasLayer(marker)); // Liste bereinigen
   }

function updateCircle() {
    if (searchCircle) {
        map.removeLayer(searchCircle);
    }
    if (originMarker) { map.removeLayer(originMarker); }

    // Entferne alle Marker
    markers.forEach(marker => map.removeLayer(marker));
    markers = []; // Leere die Marker-Liste

    searchCircle = createGeodesicCircle(lat, lon, radius * 1000);

    // Ursprungspunkt setzen
     originMarker = L.marker([lat, lon], {
        icon: L.icon({
        iconUrl: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png",
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34]
        })
    }).addTo(map);
    originMarker.bindPopup("Current Location");
}

function zoomToCoordinates() {
    map.setView([lat, lon], 10); // Zoomt auf die gesetzten Koordinaten
}

function createGeodesicCircle(lat, lon, radius, steps = 64) {
        let coords = [];
        let earthRadius = 6371000; // Erdradius in Metern

        for (let i = 0; i < steps; i++) {
            let angle = (i / steps) * 2 * Math.PI;
            let dx = radius * Math.cos(angle);
            let dy = radius * Math.sin(angle);

            let newLat = lat + (dy / earthRadius) * (180 / Math.PI);
            let newLon = lon + (dx / (earthRadius * Math.cos(lat * Math.PI / 180))) * (180 / Math.PI);

            coords.push([newLat, newLon]);
        }
        return L.polygon(coords, { color: 'red', fillColor: 'red', fillOpacity: 0.1 }).addTo(map);
}

// Fetch stations from backend
async function fetchStations() {
    try {
        const response = await fetch(`http://localhost:8080/api/get_stations?lat=${lat}&lon=${lon}&radius=${radius}&limit=${limit}`);
        if (!response.ok) throw new Error("Failed to fetch stations");
        stations = await response.json();
        selectedStation = null;  // Reset selected station
        weatherData = null;      // Clear previous weather data
        console.log("Stations received:", stations);
        showStationMarkers();
        if (myChart) {
            myChart.destroy();
        }

    } catch (error) {
        console.error("Error fetching stations:", error);
    }
}

// Funktion, um Marker und roten Kreis für Stationen anzuzeigen
function showStationMarkers() {
     // Entferne alten Kreis, falls vorhanden
    if (searchCircle) {
        map.removeLayer(searchCircle);
    }
    searchCircle = createGeodesicCircle(lat, lon, radius * 1000);

     // Definiere ein kleineres Icon
    const smallIcon = L.icon({
        iconUrl: "https://unpkg.com/leaflet@1.7.1/dist/images/marker-icon.png", // Standard Leaflet-Icon
        iconSize: [16, 26], // Kleinere Größe (Breite, Höhe)
        iconAnchor: [8, 26], // Ankerpunkt unten mittig
        popupAnchor: [1, -24] // Position des Popups relativ zum Icon
    });

    // Entferne alte Marker
    markers.forEach(marker => map.removeLayer(marker));
    markers = [];

    // Füge Marker für die ersten 10 Stationen hinzu
    stations.slice(0, 10).forEach(station => {
        const { latitude, longitude } = station;
        const marker = L.marker([latitude, longitude], { icon: smallIcon }).addTo(map);
        marker.bindPopup(`<b>${station.id}</b><br>(${latitude}, ${longitude})`);

        marker.on('click', () => {
            fetchWeatherData(station.id); // Lade die Wetterdaten für die Station
            map.setView([latitude, longitude], 10);
        });

        markers.push(marker);
    });
  filterMarkers();
}

  // Fetch weather data for a specific station
  async function fetchWeatherData(stationId) {
      try {
          const response = await fetch(`http://localhost:8080/api/get_weather_data?stationId=${stationId}&startYear=${startYear}&endYear=${endYear}`);
          if (!response.ok) throw new Error("Failed to fetch weather data");
          weatherData = await response.json();
          selectedStation = stationId;  // Store selected station
          console.log("Weather data received:", weatherData);

          await tick();

          updateChart();  // Diagramm aktualisieren
      } catch (error) {
          console.error("Error fetching weather data:", error);
      }
  }
  function updateChart() {
      if (!weatherData || !weatherData.jahreswerte || !chartCanvas) return;

        const years = Object.keys(weatherData.jahreswerte);
            const tminData = years.map(year => {
                return weatherData.jahreswerte[year].tmin !== "NaN" ? parseFloat(weatherData.jahreswerte[year].tmin).toFixed(2) : null;
            });
            const tmaxData = years.map(year => {
                return weatherData.jahreswerte[year].zmax !== "NaN" ? parseFloat(weatherData.jahreswerte[year].zmax).toFixed(2) : null;
            });

      if (myChart) {
          myChart.destroy();
      }

      myChart = new Chart(chartCanvas, {
          type: "line",
          data: {
              labels: years,
              datasets: [
                  {
                      label: "Min. Temperatur (°C)",
                      data: tminData,
                      borderColor: "blue",
                      backgroundColor: "rgba(0, 0, 255, 0.2)",
                      fill: true,
                      tension: 0.3
                  },
                  {
                      label: "Max. Temperatur (°C)",
                      data: tmaxData,
                      borderColor: "red",
                      backgroundColor: "rgba(255, 0, 0, 0.2)",
                      fill: true,
                      tension: 0.3
                  }
              ]
          },
          options: {
              responsive: true,
              maintainAspectRatio: false,
              scales: {
                  x: {
                      title: {
                          display: true,
                          text: "Jahr"
                      }
                  },
                  y: {
                      title: {
                          display: true,
                          text: "Temperatur (°C)"
                      }
                  }
              }
          }
      });
  }
</script>

<main>
    <div id="map"></div>
    <div class="overlay">
        <h1>Weather Station Finder</h1>

        <div class="search-controls">
            <label>Latitude: <input type="number" bind:value={lat} on:change={updateCircle} /></label>
            <label>Longitude: <input type="number" bind:value={lon} on:change={updateCircle} /></label>
            <label>Radius (km): <input type="number" bind:value={radius} on:change={updateCircle} /></label>
            <label>Maximale Anzeige an Stationen: <input type="number" bind:value={limit} min="1" max="10" on:change={updateLimit} /></label>
        </div>

        <div class="year-controls">
            <label>Start Year: <input type="number" bind:value={startYear} min="1900" max="2100" /></label>
            <label>End Year: <input type="number" bind:value={endYear} min="1900" max="2100" /></label>
        </div>

        <button on:click={() => { fetchStations(); map.setView([lat, lon], 10); }}>
            Search Stations
        </button>

    <!-- Station List -->
    {#if stations.length > 0}
        <h2>Available Stations:</h2>
        <ul>
            {#each stations as station}
                <li>
                    <button on:click={() => fetchWeatherData(station.id)}>
                        {station.id}  ({station.latitude}, {station.longitude})
                    </button>
                     </li>
            {/each}
        </ul>
    {/if}
    </div>
    {#if weatherData}
        <div class="overlay_right">
            <div class="chart-container">
                <canvas bind:this={chartCanvas}></canvas>
            </div>

            <div class="weather-data">
                <table>
                    <thead>
                        <tr>
                            <th>Year</th>
                            <th>Minimum Temperature</th>
                            <th>Maximum Temperature</th>
                        </tr>
                    </thead>
                    <tbody>
                        {#each Object.keys(weatherData.jahreswerte).filter(year =>
                            weatherData.jahreswerte[year].tmin !== "NaN" && weatherData.jahreswerte[year].zmax !== "NaN"
                        ) as year}
                            <tr>
                                <td>{year}</td>
                                <td>{parseFloat(weatherData.jahreswerte[year].tmin).toFixed(2)}°C</td>
                                <td>{parseFloat(weatherData.jahreswerte[year].zmax).toFixed(2)}°C</td>
                            </tr>
                        {/each}
                    </tbody>
                </table>
            </div>
        </div>
    {/if}
</main>

<style>
   main {
        font-family: Arial, sans-serif;
    }

    .search-controls, .year-controls {
        gap: 10px;
        margin-bottom: 10px;
    }

    .year-controls label {
        flex-direction: column;
        align-items: center;
    }

    .chart-container {
        width: 100%;
        max-width: 600px;
        height: 400px;
        margin: 20px auto;
    }

    ul {
        list-style: none;
        padding: 0;
    }

    li {
        margin: 5px 0;
        color: #918F8F;
    }

    button {
        cursor: pointer;
        padding: 5px 10px;
        border: none;
        background-color: #615F5F;
        color: white;
        border-radius: 5px;
    }

    #map {
        position: absolute;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        z-index: 0;
    }

    .overlay {
        position: absolute;
        top: 5%;
        left: 15%;
        transform: translateX(-50%);
        background: rgba(255, 255, 255, 0.9);
        padding: 15px;
        border-radius: 8px;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        z-index: 1000;
        max-width: 400px;
        text-align: center;
    }

    .overlay_right {
        position: absolute;
        top: 5%;
        left: 88%;
        transform: translateX(-50%);
        background: rgba(255, 255, 255, 0.9);
        padding: 15px;
        border-radius: 8px;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        z-index: 1000;
        text-align: center;
    }

    .weather-data {
        max-height: 300px;
        overflow-y: auto;
        margin-top: 10px;
        justify-content: left;
        align-items: center;
    }

    .weather-data table {
        width: 100%;
        border-collapse: collapse;
        border: 2px solid #A49E9E;
    }

    .weather-data th, .weather-data td {
        padding: 10px;
        text-align: left;
        border-bottom: 1px solid #ddd;
        color: black;

    }

    .weather-data th {
        position: sticky;
        top: 0;
        background-color: #f0f0f0;
        z-index: 2;
    }

</style>
