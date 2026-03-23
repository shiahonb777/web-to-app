// Vite Vanilla JS Weather App - Built version

// Mock weather data
const weatherData = {
    'New York': { temp: 23, icon: '☀️', desc: 'Sunny', humidity: 45, wind: 12, feels: 25 },
    'Los Angeles': { temp: 26, icon: '⛅', desc: 'Cloudy', humidity: 65, wind: 8, feels: 28 },
    'Chicago': { temp: 31, icon: '🌤️', desc: 'Partly Cloudy', humidity: 78, wind: 6, feels: 35 },
    'Houston': { temp: 30, icon: '🌧️', desc: 'Showers', humidity: 82, wind: 15, feels: 33 },
    'Phoenix': { temp: 24, icon: '☀️', desc: 'Sunny', humidity: 55, wind: 10, feels: 26 },
    'London': { temp: 22, icon: '🌫️', desc: 'Overcast', humidity: 70, wind: 5, feels: 23 },
    'Paris': { temp: 27, icon: '⛅', desc: 'Cloudy', humidity: 60, wind: 9, feels: 29 },
    'Tokyo': { temp: 20, icon: '🌤️', desc: 'Partly Cloudy', humidity: 40, wind: 14, feels: 21 },
    'Sydney': { temp: 25, icon: '☀️', desc: 'Sunny', humidity: 50, wind: 11, feels: 27 },
    'Dubai': { temp: 28, icon: '🌫️', desc: 'Hazy', humidity: 75, wind: 4, feels: 31 }
};

// DOM elements
const cityInput = document.getElementById('cityInput');
const searchBtn = document.getElementById('searchBtn');
const cityName = document.getElementById('cityName');
const temperature = document.getElementById('temperature');
const weatherIcon = document.getElementById('weatherIcon');
const description = document.getElementById('description');
const humidity = document.getElementById('humidity');
const wind = document.getElementById('wind');
const feelsLike = document.getElementById('feelsLike');

// Update weather display
function updateWeather(city) {
    const data = weatherData[city];
    
    if (data) {
        cityName.textContent = city;
        temperature.textContent = `${data.temp}°`;
        weatherIcon.textContent = data.icon;
        description.textContent = data.desc;
        humidity.textContent = `${data.humidity}%`;
        wind.textContent = `${data.wind} km/h`;
        feelsLike.textContent = `${data.feels}°`;
        
        // Add animation effect
        const display = document.querySelector('.weather-display');
        display.style.animation = 'none';
        display.offsetHeight; // Trigger reflow
        display.style.animation = 'fadeIn 0.5s ease-out';
    } else {
        // Generate random weather data
        const icons = ['☀️', '⛅', '🌤️', '🌧️', '🌫️', '⛈️'];
        const descs = ['Sunny', 'Cloudy', 'Partly Cloudy', 'Showers', 'Overcast', 'Thunderstorm'];
        const randomIndex = Math.floor(Math.random() * icons.length);
        const randomTemp = Math.floor(Math.random() * 20) + 15;
        
        cityName.textContent = city;
        temperature.textContent = `${randomTemp}°`;
        weatherIcon.textContent = icons[randomIndex];
        description.textContent = descs[randomIndex];
        humidity.textContent = `${Math.floor(Math.random() * 50) + 30}%`;
        wind.textContent = `${Math.floor(Math.random() * 20) + 5} km/h`;
        feelsLike.textContent = `${randomTemp + Math.floor(Math.random() * 5) - 2}°`;
    }
}

// Event listeners
searchBtn.addEventListener('click', () => {
    const city = cityInput.value.trim();
    if (city) {
        updateWeather(city);
    }
});

cityInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        const city = cityInput.value.trim();
        if (city) {
            updateWeather(city);
        }
    }
});

// Initialize
console.log('🌤️ Vite Weather App loaded');
