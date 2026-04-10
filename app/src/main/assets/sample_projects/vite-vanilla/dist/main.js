// Vite Vanilla JS 天气应用 - 已构建版本

// 模拟天气数据
const weatherData = {
    '北京': { temp: 23, icon: '☀️', desc: '晴朗', humidity: 45, wind: 12, feels: 25 },
    '上海': { temp: 26, icon: '⛅', desc: '多云', humidity: 65, wind: 8, feels: 28 },
    '广州': { temp: 31, icon: '🌤️', desc: '晴间多云', humidity: 78, wind: 6, feels: 35 },
    '深圳': { temp: 30, icon: '🌧️', desc: '阵雨', humidity: 82, wind: 15, feels: 33 },
    '杭州': { temp: 24, icon: '☀️', desc: '晴朗', humidity: 55, wind: 10, feels: 26 },
    '成都': { temp: 22, icon: '🌫️', desc: '阴天', humidity: 70, wind: 5, feels: 23 },
    '武汉': { temp: 27, icon: '⛅', desc: '多云', humidity: 60, wind: 9, feels: 29 },
    '西安': { temp: 20, icon: '🌤️', desc: '晴间多云', humidity: 40, wind: 14, feels: 21 },
    '南京': { temp: 25, icon: '☀️', desc: '晴朗', humidity: 50, wind: 11, feels: 27 },
    '重庆': { temp: 28, icon: '🌫️', desc: '阴天', humidity: 75, wind: 4, feels: 31 }
};

// DOM 元素
const cityInput = document.getElementById('cityInput');
const searchBtn = document.getElementById('searchBtn');
const cityName = document.getElementById('cityName');
const temperature = document.getElementById('temperature');
const weatherIcon = document.getElementById('weatherIcon');
const description = document.getElementById('description');
const humidity = document.getElementById('humidity');
const wind = document.getElementById('wind');
const feelsLike = document.getElementById('feelsLike');

// 更新天气显示
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
        
        // 添加动画效果
        const display = document.querySelector('.weather-display');
        display.style.animation = 'none';
        display.offsetHeight; // 触发重绘
        display.style.animation = 'fadeIn 0.5s ease-out';
    } else {
        // 随机生成天气数据
        const icons = ['☀️', '⛅', '🌤️', '🌧️', '🌫️', '⛈️'];
        const descs = ['晴朗', '多云', '晴间多云', '阵雨', '阴天', '雷阵雨'];
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

// 事件监听
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

// 初始化
console.log('🌤️ Vite 天气应用已加载');
