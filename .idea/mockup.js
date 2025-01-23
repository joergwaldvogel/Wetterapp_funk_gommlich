 // Initialize the map
    const map = L.map('map').setView([37.7749, -122.4194], 5); // Default to California
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19
    }).addTo(map);

    // Example station marker
    L.marker([37.7749, -122.4194]).addTo(map)
        .bindPopup('Station: Kalifornien (123.234.233)')
        .openPopup();

    // Toggle station details
    function toggleDetails(button) {
        const details = button.parentElement.nextElementSibling;
        if (details.style.display === 'none') {
            details.style.display = 'block';
            const ctx = details.querySelector('canvas').getContext('2d');
            new Chart(ctx, {
                type: 'bar',
                data: {
                    labels: ['2020', '2021'],
                    datasets: [
                        {
                            label: 'Min Temp',
                            data: [-5, -3],
                            backgroundColor: 'rgba(54, 162, 235, 0.5)'
                        },
                        {
                            label: 'Max Temp',
                            data: [15, 18],
                            backgroundColor: 'rgba(255, 99, 132, 0.5)'
                        }
                    ]
                }
            });
        } else {
            details.style.display = 'none';
        }
    }