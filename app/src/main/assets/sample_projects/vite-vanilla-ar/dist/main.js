// Vite Vanilla JS Weather App - Built version (Arabic)

// Mock weather data for Arabic cities
const weatherData = {
    'دبي': { temp: 28, icon: '☀️', desc: 'مشمس', humidity: 45, wind: 12, feels: 30 },
    'الرياض': { temp: 32, icon: '☀️', desc: 'حار', humidity: 35, wind: 8, feels: 35 },
    'القاهرة': { temp: 26, icon: '⛅', desc: 'غائم جزئياً', humidity: 55, wind: 10, feels: 28 },
    'بيروت': { temp: 24, icon: '🌤️', desc: 'صافي', humidity: 60, wind: 6, feels: 26 },
    'عمان': { temp: 22, icon: '⛅', desc: 'غائم', humidity: 50, wind: 9, feels: 24 },
    'الدوحة': { temp: 30, icon: '☀️', desc: 'مشمس', humidity: 65, wind: 15, feels: 33 },
    'الكويت': { temp: 31, icon: '🌫️', desc: 'ضبابي', humidity: 70, wind: 5, feels: 34 },
    'مسقط': { temp: 27, icon: '🌤️', desc: 'صافي جزئياً', humidity: 75, wind: 14, feels: 29 },
    'أبوظبي': { temp: 29, icon: '☀️', desc: 'مشمس', humidity: 40, wind: 11, feels: 31 },
    'البحرين': { temp: 28, icon: '🌫️', desc: 'رطب', humidity: 80, wind: 4, feels: 32 }
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
        wind.textContent = `${data.wind} كم/س`;
        feelsLike.textContent = `${data.feels}°`;
        
        // Add animation effect
        const display = document.querySelector('.weather-display');
        display.style.animation = 'none';
        display.offsetHeight; // Trigger reflow
        display.style.animation = 'fadeIn 0.5s ease-out';
    } else {
        // Generate random weather data
        const icons = ['☀️', '⛅', '🌤️', '🌧️', '🌫️', '⛈️'];
        const descs = ['مشمس', 'غائم', 'صافي جزئياً', 'ممطر', 'ضبابي', 'عاصف'];
        const randomIndex = Math.floor(Math.random() * icons.length);
        const randomTemp = Math.floor(Math.random() * 20) + 20;
        
        cityName.textContent = city;
        temperature.textContent = `${randomTemp}°`;
        weatherIcon.textContent = icons[randomIndex];
        description.textContent = descs[randomIndex];
        humidity.textContent = `${Math.floor(Math.random() * 50) + 30}%`;
        wind.textContent = `${Math.floor(Math.random() * 20) + 5} كم/س`;
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
console.log('🌤️ تطبيق الطقس تم تحميله');
