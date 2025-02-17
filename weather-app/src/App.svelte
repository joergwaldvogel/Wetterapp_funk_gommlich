<script>
  import { onMount } from "svelte";

  let stations = [];
  let selectedStation = null;
  let weatherData = null;

  let lat = -8.0;
  let lon = -36.5;
  let radius = 50;
  let startYear = 1949;
  let endYear = 1951;


  // Fetch stations from backend
  async function fetchStations() {
      try {
          const response = await fetch(`http://localhost:8080/api/get_stations?lat=${lat}&lon=${lon}&radius=${radius}`);
          if (!response.ok) throw new Error("Failed to fetch stations");
          stations = await response.json();
          selectedStation = null;  // Reset selected station
          weatherData = null;      // Clear previous weather data
          console.log("Stations received:", stations);
      } catch (error) {
          console.error("Error fetching stations:", error);
      }
  }

  // Fetch weather data for a specific station
  async function fetchWeatherData(stationId) {
      try {
          const response = await fetch(`http://localhost:8080/api/get_weather_data?stationId=${stationId}&startYear=${startYear}&endYear=${endYear}`);
          if (!response.ok) throw new Error("Failed to fetch weather data");
          weatherData = await response.json();
          selectedStation = stationId;  // Store selected station
          console.log("Weather data received:", weatherData);
      } catch (error) {
          console.error("Error fetching weather data:", error);
      }
  }
</script>

<main>
    <h1>Weather Station Finder</h1>

     <!-- Search Controls -->
        <div class="search-controls">
            <label>Latitude: <input type="number" bind:value={lat} /></label>
            <label>Longitude: <input type="number" bind:value={lon} /></label>
            <label>Radius (km): <input type="number" bind:value={radius} /></label>
        </div>

        <!-- Year Selection -->
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
                        {station.id} - ({station.latitude}, {station.longitude})
                    </button>

                    <!-- Show weather data directly under the selected station -->
                    {#if selectedStation === station.id && weatherData}
                        <pre>{JSON.stringify(weatherData, null, 2)}</pre>
                    {/if}
                </li>
            {/each}
        </ul>
    {/if}
</main>

<style>
   main {
        font-family: Arial, sans-serif;
        max-width: 600px;
        margin: auto;
    }
    .search-controls, .year-controls {
        display: flex;
        gap: 10px;
        margin-bottom: 10px;
    }
    .year-controls label {
        display: flex;
        flex-direction: column;
        align-items: center;
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
    pre {
        background: #222;
        color: #fff;
        padding: 10px;
        border-radius: 5px;
        margin-top: 5px;
        overflow-x: auto;
    }
</style>
