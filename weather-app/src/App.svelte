<script>
  import { onMount } from "svelte";
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
  let radius = 100000;
  let startYear = 1949;
  let endYear = 1959;

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
       fetchStations();
   });

  // Fetch stations from backend
  async function fetchStations() {
      try {
          const response = await fetch(`http://localhost:8080/api/get_stations?lat=${lat}&lon=${lon}&radius=${radius}`);
          if (!response.ok) throw new Error("Failed to fetch stations");
          stations = await response.json();
          selectedStation = null;  // Reset selected station
          weatherData = null;      // Clear previous weather data
          console.log("Stations received:", stations);
          showStationMarkers();
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

    // Erstelle einen neuen roten Kreis um die Suchposition
    searchCircle = L.circle([lat, lon], {
        color: 'red',
        fillColor: 'red',
        fillOpacity: 0.1,
        radius: radius * 1000 // km → m Umrechnung
    }).addTo(map);

    // Definiere ein kleineres Icon
    const smallIcon = L.icon({
        iconUrl: "https://unpkg.com/leaflet@1.7.1/dist/images/marker-icon.png", // Standard Leaflet-Icon
        iconSize: [16, 26], // Kleinere Größe (Breite, Höhe)
        iconAnchor: [8, 26], // Ankerpunkt unten mittig
        popupAnchor: [1, -24] // Position des Popups relativ zum Icon
    });

    // Füge Marker für die ersten 10 Stationen hinzu
    stations.slice(0, 10).forEach(station => {
        const { latitude, longitude } = station;
        const marker = L.marker([latitude, longitude], { icon: smallIcon }).addTo(map);
        marker.bindPopup(`<b>${station.id}</b><br>(${latitude}, ${longitude})`);
    });
}

  // Fetch weather data for a specific station
  async function fetchWeatherData(stationId) {
      try {
          const response = await fetch(`http://localhost:8080/api/get_weather_data?stationId=${stationId}&startYear=${startYear}&endYear=${endYear}`);
          if (!response.ok) throw new Error("Failed to fetch weather data");
          weatherData = await response.json();
          selectedStation = stationId;  // Store selected station
          console.log("Weather data received:", weatherData);

          updateChart();  // Diagramm aktualisieren
      } catch (error) {
          console.error("Error fetching weather data:", error);
      }
  }
  function updateChart() {
      if (!weatherData || !weatherData.jahreswerte || !chartCanvas) return;

      const years = Object.keys(weatherData.jahreswerte);
      const tminData = years.map(year => weatherData.jahreswerte[year].tmin || null);
      const tmaxData = years.map(year => weatherData.jahreswerte[year].zmax || null);

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
            <label>Latitude: <input type="number" bind:value={lat} /></label>
            <label>Longitude: <input type="number" bind:value={lon} /></label>
            <label>Radius (km): <input type="number" bind:value={radius} /></label>
        </div>

        <div class="year-controls">
            <label>Start Year: <input type="number" bind:value={startYear} min="1900" max="2100" /></label>
            <label>End Year: <input type="number" bind:value={endYear} min="1900" max="2100" /></label>
        </div>

        <button on:click={fetchStations}>Search Stations</button>

    <!-- Station List -->
    {#if stations.length > 0}
        <h2>Available Stations:</h2>
        <ul>
            {#each stations as station}
                <li>
                    <button on:click={() => fetchWeatherData(station.id)}>
                        {station.id}  ({station.latitude}, {station.longitude})
                    </button>

                    <!-- Show weather data directly under the selected station -->
                   {#if selectedStation === station.id && weatherData}
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
                                                   {#each Object.keys(weatherData.jahreswerte) as year}
                                                       <tr>
                                                           <td>{year}</td>
                                                           <td>
                                                               {#if weatherData.jahreswerte[year].tmin === "NaN"}
                                                                   <span class="missing-data">Data not available</span>
                                                               {:else}
                                                                   {weatherData.jahreswerte[year].tmin}°C
                                                               {/if}
                                                           </td>
                                                           <td>
                                                               {#if weatherData.jahreswerte[year].zmax === "NaN"}
                                                                   <span class="missing-data">Data not available</span>
                                                               {:else}
                                                                   {weatherData.jahreswerte[year].zmax}°C
                                                               {/if}
                                                           </td>
                                                       </tr>
                                                   {/each}
                                               </tbody>
                                           </table>
                                       </div>
                                   {/if}
                               </li>
                           {/each}
                       </ul>
                   {/if}
        <div class="chart-container">
            <canvas bind:this={chartCanvas}></canvas>
        </div>
    </div>
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

    pre {
        background: #222;
        color: #fff;
        padding: 10px;
        border-radius: 5px;
        margin-top: 5px;
        overflow-x: auto;
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

    h3 {
        color:black;
    }

    .weather-data {
        margin-top: 1px;
        display: flex;
        justify-content: left; /* Links ausrichten */
        align-items: center; /* Vertikal zentrieren */
    }

    .weather-data table {
        width: 100%;
        border-collapse: collapse;
        border: 2px solid #A49E9E;
    }

    .weather-data th, .weather-data td {
        padding: 8px;
        text-align: left;
        border-bottom: 1px solid #ddd;
        color: black;
    }

    .missing-data {
        color: red;
        font-style: italic;
    }
</style>
