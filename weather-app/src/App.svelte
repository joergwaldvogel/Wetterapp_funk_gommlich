<script>
  import { onMount } from "svelte";
  import { tick } from "svelte";
  import "leaflet/dist/leaflet.css";
  import L from "leaflet";
  import Chart from "chart.js/auto";


  let stations = [];
  let selectedStation = null;
  let selectedStationName = null;
  let weatherData = null;
  let seasonalweatherData = null;
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
  let showLoading = false;

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
   });

function updateLimitStations() {
    if (limit > 10) {
        limit = 10;  // Setzt den Wert auf 10, falls er höher ist
    }
}

function updateLimitRadius() {
    if (radius > 100) {
        radius = 100;  // Setzt den Wert auf 100, falls er höher ist
    }
}

function handleRadiusChange() {
    updateLimitRadius();
    updateCircle();
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
        const response = await fetch(`http://localhost:8080/api/get_stations?lat=${lat}&lon=${lon}&radius=${radius}&limit=${limit}&startYear=${startYear}&endYear=${endYear}`);
        if (!response.ok) throw new Error("Failed to fetch stations");
        stations = await response.json();
        selectedStation = null;
        weatherData = null;
        seasonalweatherData = null;
        console.log("Stations received:", stations);
        showStationMarkers();

    } catch (error) {
        console.error("Error fetching stations:", error);
    }
}

// Funktion, um Marker und roten Kreis für Stationen anzuzeigen
function showStationMarkers() {
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
        marker.bindPopup(`<b>${station.name}</b><br>(${latitude}, ${longitude})`);


        marker.on('click', () => {
            fetchWeatherData(station.id); // Lade die Wetterdaten für die Station
            fetchSeasonalWeatherData(station.id);
            map.setView([latitude, longitude], 10);
        });

        markers.push(marker);
    });
  filterMarkers();
}

async function fetchWeatherData(stationId) {
    showLoading = false;
    const timeout = setTimeout(() => {
        showLoading = true; // Ladehinweis nach 1 sec
    }, 1000);

    try {
        const response = await fetch(`http://localhost:8080/api/get_weather_data?stationId=${stationId}&startYear=${startYear}&endYear=${endYear}`);
        if (!response.ok) throw new Error("Failed to fetch weather data");
        weatherData = await response.json();
        const station = stations.find(s => s.id === stationId);
        selectedStationName = station ? station.name : "Unbekannte Station";  // Name speichern oder Fallback
        selectedStation = stationId;
        console.log("Weather data received:", weatherData);

        await tick();

        updateChart();
    } catch (error) {
        console.error("Error fetching weather data:", error);
    } finally {
        clearTimeout(timeout);
        showLoading = false;
    }
}

async function fetchSeasonalWeatherData(stationId) {
    seasonalweatherData = null;
    try {
        const response = await fetch(`http://localhost:8080/api/get_seasonal_weather_data?stationId=${stationId}&startYear=${startYear}&endYear=${endYear}`);
        if (!response.ok) throw new Error("Failed to fetch weather data");
        seasonalweatherData = await response.json();
        selectedStation = stationId;
        console.log("Seasonal Weather data received:", seasonalweatherData);
        console.log("Weather data received:", JSON.stringify(seasonalweatherData, null, 2));

         await tick();

         updateChart();
    } catch (error) {
        console.error("Error fetching weather data:", error);
    }
}

const getSortedSeasons = () => {
const seasonOrder = ['Winter', 'Frühling', 'Sommer', 'Herbst'];
console.log("Log 4");
  return Object.keys(seasonalweatherData.jahreszeiten)
    .map(season => {
      const seasonYears = season.split("_");
      const seasonName = seasonYears[0];  // Saisonname (z.B. Frühling, Sommer, Herbst, Winter)
      const year = parseInt(seasonYears[seasonYears.length - 1]);  // Jahr extrahieren

      // Nur Jahre im Bereich zwischen startYear und endYear berücksichtigen
      if (year < startYear || year > endYear) {
        return null;  // Rückgabe von null für Jahre außerhalb des Bereichs
      }

      return {
        season,
        seasonName,
        year,
        minTemp: seasonalweatherData.jahreszeiten[season].avgMin,
        maxTemp: seasonalweatherData.jahreszeiten[season].avgMax
      };
    })
    .filter(item => item !== null) // Nullwerte herausfiltern
    .sort((a, b) => {
      // Zuerst nach Jahr sortieren
      if (a.year !== b.year) {
        return a.year - b.year;
      }

      // Wenn Jahre gleich sind, nach Saison innerhalb des Jahres sortieren (Frühling, Sommer, Herbst, Winter)
      return seasonOrder.indexOf(a.seasonName) - seasonOrder.indexOf(b.seasonName);
    });
};

async function updateChart() {
    console.log("Updating Chart");
    if (!weatherData || !weatherData.jahreswerte || !chartCanvas || !seasonalweatherData) return;
    console.log("Log 1");

    const years = Object.keys(weatherData.jahreswerte);
    const tminData = years.map(year => weatherData.jahreswerte[year].avgMin !== "NaN" ? parseFloat(weatherData.jahreswerte[year].avgMin) : null);
    const tmaxData = years.map(year => weatherData.jahreswerte[year].avgMax !== "NaN" ? parseFloat(weatherData.jahreswerte[year].avgMax) : null);

    let seasonData = {
        Winter: { min: Array(years.length).fill(null), max: Array(years.length).fill(null) },
        Frühling: { min: Array(years.length).fill(null), max: Array(years.length).fill(null) },
        Sommer: { min: Array(years.length).fill(null), max: Array(years.length).fill(null) },
        Herbst: { min: Array(years.length).fill(null), max: Array(years.length).fill(null) }
    };

    if (seasonalweatherData && seasonalweatherData.jahreszeiten) {
    console.log("Log 2");
        const sortedSeasons = getSortedSeasons();
        console.log("Log 3");
        sortedSeasons.forEach(({ seasonName, year, minTemp, maxTemp }) => {
            const index = years.indexOf(year.toString());
            if (index !== -1) {
                seasonData[seasonName].min[index] = parseFloat(minTemp);
                seasonData[seasonName].max[index] = parseFloat(maxTemp);
            }
        });
    }

    if (myChart) {
        myChart.destroy();
    }

    myChart = new Chart(chartCanvas, {
        type: "line",
        data: {
            labels: years,
            datasets: [
                {
                    label: "Jährliche Min. Temperatur (°C)",
                    data: tminData,
                    borderColor: "blue",
                    backgroundColor: "transparent",
                    fill: true,
                    tension: 0.3
                },
                {
                    label: "Jährliche Max. Temperatur (°C)",
                    data: tmaxData,
                    borderColor: "red",
                    backgroundColor: "transparent",
                    fill: true,
                    tension: 0.3
                },
                {
                    label: "Winter Min. Temperatur (°C)",
                    data: seasonData.Winter.min,
                    borderColor: "darkblue",
                    backgroundColor: "transparent",
                    fill: false,
                    tension: 0.3
                },
                {
                    label: "Winter Max. Temperatur (°C)",
                    data: seasonData.Winter.max,
                    borderColor: "lightblue",
                    backgroundColor: "transparent",
                    fill: false,
                    tension: 0.3
                },
                {
                    label: "Frühling Min. Temperatur (°C)",
                    data: seasonData.Frühling.min,
                    borderColor: "green",
                    backgroundColor: "transparent",
                    fill: false,
                    tension: 0.3
                },
                {
                    label: "Frühling Max. Temperatur (°C)",
                    data: seasonData.Frühling.max,
                    borderColor: "lightgreen",
                    backgroundColor: "transparent",
                    fill: false,
                    tension: 0.3
                },
                {
                    label: "Sommer Min. Temperatur (°C)",
                    data: seasonData.Sommer.min,
                    borderColor: "orange",
                    backgroundColor: "transparent",
                    fill: false,
                    tension: 0.3
                },
                {
                    label: "Sommer Max. Temperatur (°C)",
                    data: seasonData.Sommer.max,
                    borderColor: "yellow",
                    backgroundColor: "transparent",
                    fill: false,
                    tension: 0.3
                },
                {
                    label: "Herbst Min. Temperatur (°C)",
                    data: seasonData.Herbst.min,
                    borderColor: "brown",
                    backgroundColor: "transparent",
                    fill: false,
                    tension: 0.3
                },
                {
                    label: "Herbst Max. Temperatur (°C)",
                    data: seasonData.Herbst.max,
                    borderColor: "goldenrod",
                    backgroundColor: "transparent",
                    fill: false,
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
            <label>Radius (km): <input type="number" bind:value={radius} min="1" max="100" on:change={handleRadiusChange} /></label>
            <label>Max Display at Stations: <input type="number" bind:value={limit} min="1" max="10" on:change={updateLimitStations} /></label>
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
                        <button on:click={() => {
                            fetchWeatherData(station.id);
                            fetchSeasonalWeatherData(station.id);
                        }}>
                         {station.name}
                        </button>
                         </li>
                {/each}
            </ul>
        {/if}
    </div>

    {#if showLoading}
      <div class="loading-modal">
          <div class="loading-content">
              <p>Lade Daten...</p>
              <progress></progress>
          </div>
      </div>
    {/if}

    {#if weatherData && seasonalweatherData}
        <div class="overlay_right">
            <h1>{selectedStationName}</h1>
            <div class="chart-container">
                    <canvas bind:this={chartCanvas}></canvas>
            </div>

            <div class="weather-tables">
                <!-- Beide Tabellen nebeneinander -->
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
                                weatherData.jahreswerte[year].avgMin !== "NaN" && weatherData.jahreswerte[year].avgMax !== "NaN"
                            ) as year}
                                <tr>
                                    <td>{year}</td>
                                    <td>{parseFloat(weatherData.jahreswerte[year].avgMin).toFixed(1)}°C</td>
                                    <td>{parseFloat(weatherData.jahreswerte[year].avgMax).toFixed(1)}°C</td>
                                </tr>
                            {/each}
                        </tbody>
                    </table>
                </div>

                <div class="seasonal-weather-data">
                    <table>
                        <thead>
                            <tr>
                                <th>Year</th>
                                <th>Minimum Temperature</th>
                                <th>Maximum Temperature</th>
                            </tr>
                        </thead>
                        <tbody>
                            {#each getSortedSeasons() as { season, minTemp, maxTemp }}
                              {#if minTemp !== "NaN" && maxTemp !== "NaN"}
                                <tr>
                                  <td>{season}</td>
                                  <td>{minTemp.toFixed(1)}°C</td>
                                  <td>{maxTemp.toFixed(1)}°C</td>
                                </tr>
                              {/if}
                            {/each}
                        </tbody>
                    </table>
                </div>
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

    .year-controls {
        display: flex;
    }

    .year-controls label {
        flex-direction: column;
        align-items: center;
    }

    .chart-container {
        flex: 1;
        min-width: 300px;
        min-width: 90%;
        max-width: 90%;
        height: 400px;
        margin-bottom: 15px;
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
        background-color: #757474;
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
        top: 2%;
        left: 15%;
        transform: translateX(-50%);
        background: rgba(255, 255, 255, 0.9);
        padding: 0.5%;
        border-radius: 8px;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        z-index: 1000;
        max-width: 400px;
        text-align: center;
    }

    .overlay_right {
        position: absolute;
        top: 2%;
        left: 79%;
        transform: translateX(-50%);
        background: rgba(255, 255, 255, 0.9);
        padding: 1%;
        border-radius: 8px;
        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        z-index: 1000;
        text-align: center;
        max-width: 90%;
        overflow: auto;
    }

    .weather-tables {
        display: flex;
        justify-content: space-between;
        gap: 0.1%;  /* Abstand zwischen den beiden Tabellen */
        margin-top: 20px;
    }

    .weather-data,.seasonal-weather-data {
        max-height: 220px;
        overflow-y: auto;
        width: 46%;
        justify-content: left;
        align-items: center;
    }

    .weather-data table, .seasonal-weather-data table {
        width: 100%;
        border-collapse: collapse;
        border: 2px solid #A49E9E;
    }

    .weather-data th, .weather-data td, .seasonal-weather-data th, .seasonal-weather-data td {
        padding: 10px;
        text-align: left;
        border-bottom: 1px solid #ddd;
        color: black;

    }

    .weather-data th , .seasonal-weather-data th {
        position: sticky;
        top: 0;
        background-color: #f0f0f0;
        z-index: 2;
    }

    .loading-modal {
        position: fixed;
        top: 0;
        left: 0;
        width: 100vw;
        height: 100vh;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .loading-content {
        background: white;
        padding: 20px;
        border-radius: 8px;
        text-align: center;
        box-shadow: 0px 0px 10px rgba(0, 0, 0, 0.2);
    }

    progress {
        width: 100%;
        height: 10px;
    }
</style>
