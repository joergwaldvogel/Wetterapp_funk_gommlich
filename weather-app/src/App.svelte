<script>
    import { onMount } from "svelte";
    let latitude = 52.52;
    let longitude = 13.405;
    let radius = 50;
    let stations = [];
    let selectedStation = null;
    let weatherData = null;

    async function fetchStations() {
      const response = await fetch(`http://localhost:8080/stations?lat=${latitude}&lon=${longitude}&radius=${radius}`);
      stations = await response.json();
    }

    async function fetchWeatherData(stationId) {
      selectedStation = stationId;
      const response = await fetch(`http://localhost:8080/weather?station=${stationId}`);
      weatherData = await response.json();
    }
</script>

<main class="container">
    <h1>Weather Station Finder</h1>

    <div class="input-container">
        <input type="number" bind:value={latitude} placeholder="Latitude" />
        <input type="number" bind:value={longitude} placeholder="Longitude" />
        <input type="number" bind:value={radius} placeholder="Radius (km)" />
        <button on:click={fetchStations}>Find Stations</button>
    </div>

    {#if stations.length > 0}
    <h2>Found Stations</h2>
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

    {#if weatherData}
    <div class="weather-card">
        <h2>Weather Data for {selectedStation}</h2>
        <pre>{JSON.stringify(weatherData, null, 2)}</pre>
    </div>
    {/if}
</main>

<style>
    .container {
      max-width: 600px;
      margin: auto;
      padding: 20px;
      font-family: Arial, sans-serif;
    }

    .input-container {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    input {
      padding: 8px;
      font-size: 1rem;
    }

    button {
      padding: 10px;
      background-color: #007bff;
      color: white;
      border: none;
      cursor: pointer;
    }

    .weather-card {
      background: #f8f9fa;
      padding: 15px;
      margin-top: 20px;
      border-radius: 8px;
    }
</style>
