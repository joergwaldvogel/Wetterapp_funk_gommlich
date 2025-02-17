<script>
  import { onMount } from "svelte";

  let stations = [];
  let selectedStation = null;
  let weatherData = null;

  // Coordinates for searching stations
  let lat = -8.0;
  let lon = -36.5;
  let radius = 50;

  // Fetch stations from backend
  async function fetchStations() {
      try {
          const response = await fetch(`http://localhost:8080/api/get_stations?lat=${lat}&lon=${lon}&radius=${radius}`);
          if (!response.ok) throw new Error("Failed to fetch stations");
          stations = await response.json();
          console.log("Stations received:", stations);
      } catch (error) {
          console.error("Error fetching stations:", error);
      }
  }

  // Fetch weather data for a specific station
  async function fetchWeatherData(stationId) {
      try {
          const startYear = 1949;
          const endYear = 1951;
          const response = await fetch(`http://localhost:8080/api/get_weather_data?stationId=${stationId}&startYear=${startYear}&endYear=${endYear}`);
          if (!response.ok) throw new Error("Failed to fetch weather data");
          weatherData = await response.json();
          selectedStation = stationId;
          console.log("Weather data received:", weatherData);
      } catch (error) {
          console.error("Error fetching weather data:", error);
      }

      fetch("http://localhost:8080/api/test")
        .then(response => response.text())
        .then(data => console.log("Response from backend:", data))
        .catch(error => console.error("Fetch error:", error));
  }
</script>

<!-- UI Section -->
<main>
    <h1>Weather Station Finder</h1>

    <!-- Search Controls -->
    <label>Latitude: <input type="number" bind:value={lat} /></label>
    <label>Longitude: <input type="number" bind:value={lon} /></label>
    <label>Radius (km): <input type="number" bind:value={radius} /></label>
    <button on:click={fetchStations}>Search Stations</button>

    <!-- Station List -->
    {#if stations.length > 0}
        <h2>Available Stations:</h2>
        <ul>
            {#each stations as station}
                <li>
                    <button on:click={() => fetchWeatherData(station.id)}>
                        {station.id} - ({station.latitude}, {station.longitude})
                    </button>
                </li>
            {/each}
        </ul>
    {/if}

    <!-- Weather Data -->
    {#if weatherData}
        <h2>Weather Data for {selectedStation}</h2>
        <pre>{JSON.stringify(weatherData, null, 2)}</pre>
    {/if}
</main>

<style>
  main {
      font-family: Arial, sans-serif;
      max-width: 600px;
      margin: auto;
  }
  ul {
      list-style: none;
      padding: 0;
  }
  li {
      margin: 5px 0;
  }
  button {
      cursor: pointer;
      padding: 5px 10px;
      border: none;
      background-color: #007bff;
      color: white;
      border-radius: 5px;
  }
</style>
