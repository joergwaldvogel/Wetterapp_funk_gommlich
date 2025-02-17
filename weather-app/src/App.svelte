<script>
  import { onMount } from "svelte";
  import L from "leaflet";
  import Chart from "chart.js/auto";

  let map;
  let lat = 37.7749; // Standardwert für Kalifornien
  let lon = -122.4194;
  let radius = 50;
  let stations = [];
  let selectedStation = null;
  let weatherData = null;
  let chart;
  let showDetails = false;

  onMount(() => {
      map = L.map("map").setView([lat, lon], 5);
      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", { maxZoom: 19 }).addTo(map);
  });

  async function fetchStations() {
      try {
          const response = await fetch(`http://localhost:8080/api/get_stations?lat=${lat}&lon=${lon}&radius=${radius}`);
          if (!response.ok) throw new Error("Fehler beim Abrufen der Wetterstationen");
          stations = await response.json();

          // Marker zur Karte hinzufügen
          stations.forEach(station => {
              L.marker([station.latitude, station.longitude])
                  .addTo(map)
                  .bindPopup(`Station: ${station.name} (${station.id})`);
          });
      } catch (error) {
          console.error("Fehler beim Abrufen der Stationen:", error);
      }
  }

  async function fetchWeatherData(stationId) {
      try {
          const response = await fetch(`http://localhost:8080/api/get_weather_data?stationId=${stationId}&startYear=2020&endYear=2024`);
          if (!response.ok) throw new Error("Fehler beim Abrufen der Wetterdaten");
          weatherData = await response.json();
          selectedStation = stationId;
          showDetails = true;

          updateChart();
      } catch (error) {
          console.error("Fehler beim Abrufen der Wetterdaten:", error);
      }
  }

  function updateChart() {
      if (!weatherData) return;
      if (chart) chart.destroy();

      const ctx = document.getElementById("weatherChart").getContext("2d");
      chart = new Chart(ctx, {
          type: "bar",
          data: {
              labels: weatherData.years,
              datasets: [
                  {
                      label: "Min Temp",
                      data: weatherData.minTemps,
                      backgroundColor: "rgba(54, 162, 235, 0.5)"
                  },
                  {
                      label: "Max Temp",
                      data: weatherData.maxTemps,
                      backgroundColor: "rgba(255, 99, 132, 0.5)"
                  }
              ]
          }
      });
  }
</script>

<main class="container-fluid">
    <div id="map-container">
        <div id="map"></div>
        <div class="input-box">
            <h5>Einstellungen</h5>
            <form on:submit|preventDefault={fetchStations}>
                <div class="mb-3">
                    <label class="form-label">Latitude</label>
                    <input type="number" step="0.01" bind:value={lat} class="form-control">
                </div>
                <div class="mb-3">
                    <label class="form-label">Longitude</label>
                    <input type="number" step="0.01" bind:value={lon} class="form-control">
                </div>
                <div class="mb-3">
                    <label class="form-label">Reichweite der Suche (km)</label>
                    <input type="number" bind:value={radius} class="form-control">
                </div>
                <button type="submit" class="btn btn-primary">Suchen</button>
            </form>
        </div>
    </div>

    {#if stations.length > 0}
        <h5>Gefundene Stationen</h5>
        <ul class="list-group">
            {#each stations as station}
                <li class="list-group-item">
                    <div class="d-flex justify-content-between align-items-center">
                        <span>Station: {station.name} ({station.id})</span>
                        <button class="btn btn-sm btn-link" on:click={() => fetchWeatherData(station.id)}>Details anzeigen</button>
                    </div>
                </li>
            {/each}
        </ul>
    {/if}

    {#if showDetails && weatherData}
        <h2>Wetterdaten für {selectedStation}</h2>

        <!-- Temperatur-Diagramm -->
        <canvas id="weatherChart"></canvas>

        <!-- Tabelle mit detaillierten Wetterdaten -->
        <table class="table table-striped mt-3">
            <thead>
                <tr>
                    <th>Jahr</th>
                    <th>Mittelwert Min</th>
                    <th>Mittelwert Max</th>
                </tr>
            </thead>
            <tbody>
                {#each weatherData.years as year, index}
                    <tr>
                        <td>{year}</td>
                        <td>{weatherData.minTemps[index]}</td>
                        <td>{weatherData.maxTemps[index]}</td>
                    </tr>
                {/each}
            </tbody>
        </table>
    {/if}
</main>

<style>
    #map-container {
        position: fixed;
        width: 100%;
        height: 80vh;
        overflow: hidden;
    }
    #map {
        height: 100%;
        width: 100%;
    }
    .input-box {
        position: absolute;
        top: 20px;
        left: 60px;
        background-color: rgba(255, 255, 255, 0.9);
        padding: 20px;
        border-radius: 8px;
        z-index: 1000;
    }
</style>
