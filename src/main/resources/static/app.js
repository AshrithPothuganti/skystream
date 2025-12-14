// SkyStream FINAL Frontend Loader

// API
const API_CURRENT = "/api/weather/current?city=";
const API_FORECAST = "/api/weather/forecast?city=";

// Elements
const cityInput = document.getElementById("cityInput");
const searchBtn = document.getElementById("searchBtn");

const bigTemp = document.getElementById("bigTemp");
const bigCond = document.getElementById("bigCond");
const bigIcon = document.getElementById("bigIcon");
const locationDisplay = document.getElementById("locationDisplay");

const metaHum = document.getElementById("metaHum");
const metaWind = document.getElementById("metaWind");

const hourlySlider = document.getElementById("hourlySlider");
const weekList = document.getElementById("weekList");

const sunriseTime = document.getElementById("sunriseTime");
const sunsetTime = document.getElementById("sunsetTime");
const sunProgress = document.getElementById("sunProgress");

const defaultCityEl = document.getElementById("defaultCity");
const unitBtn = document.getElementById("unitBtn");
const saveLocBtn = document.getElementById("saveLocBtn");

let currentUnit = "C";
let lastCity = "Hyderabad";

function toF(c) { return (c * 9) / 5 + 32; }
function fmtTemp(c) {
    if (c == null) return "--";
    return currentUnit === "C" ? Math.round(c) + "°" : Math.round(toF(c)) + "°";
}

// -------------------------
// BACKGROUND ENGINE
// -------------------------
function updateBackground(condition) {
    if (!condition) condition = "clear";
    const c = condition.toLowerCase();

    let bg = "default.jpg";

    if (c.includes("sun") || c.includes("clear")) bg = "sunny.jpg";
    else if (c.includes("cloud")) bg = "cloudy.jpg";
    else if (c.includes("rain")) bg = "rain.jpg";
    else if (c.includes("snow")) bg = "snow.jpg";
    else if (c.includes("fog") || c.includes("mist")) bg = "fog.jpg";

    document.body.style.backgroundImage = `url('/background/${bg}')`;
}

// -------------------------
// LOAD CURRENT WEATHER
// -------------------------
async function loadWeather(city) {
    lastCity = city;

    const res = await fetch(API_CURRENT + encodeURIComponent(city));
    const data = await res.json();

    // Unified DTO fields
    locationDisplay.textContent = data.city ?? city;
    bigTemp.textContent = fmtTemp(data.temperature);
    bigCond.textContent = data.condition ?? "---";

    if (data.icon) {
        bigIcon.src = data.icon.startsWith("https")
            ? data.icon
            : "https:" + data.icon;
    }

    metaHum.textContent = data.humidity ?? "--";
    metaWind.textContent = (data.wind ?? "--") + " km/h";

    defaultCityEl.textContent =
        `${data.city} — ${fmtTemp(data.temperature)} | ${data.condition}`;

    sunriseTime.textContent = data.sunrise ?? "--";
    sunsetTime.textContent = data.sunset ?? "--";

    if (data.aqi != null) {
        renderAirQuality(data.aqi);
    }

    updateBackground(data.condition);
}


// -------------------------
// LOAD FORECAST (10-day)
// -------------------------
async function loadForecast(city) {
    const res = await fetch(API_FORECAST + encodeURIComponent(city));
    const data = await res.json();

    

    const days = data?.forecast?.forecastday ?? [];

    // HOURLY
    hourlySlider.innerHTML = "";
    const hours = days[0]?.hour ?? [];
    hours.forEach(h => {
        const icon = h.condition?.icon;
        const t = (h.time || "00:00").split(" ")[1];

        hourlySlider.innerHTML += `
            <div class="hour-card">
                <div>${t}</div>
                <img src="${icon ? "https:" + icon : ""}" width="42">
                <div>${fmtTemp(h.temp_c)}</div>
            </div>
        `;
    });

    // WEEKLY
    weekList.innerHTML = "";
    days.forEach(d => {
        weekList.innerHTML += `
            <div class="week-card">
                <div>${d.date}</div>
                <div class="muted">${d.day.condition.text}</div>
                <div>${fmtTemp(d.day.maxtemp_c)} / ${fmtTemp(d.day.mintemp_c)}</div>
            </div>
        `;
    });

    // SUN
    sunriseTime.textContent = days[0]?.astro?.sunrise ?? "--";
    sunsetTime.textContent = days[0]?.astro?.sunset ?? "--";

    const localTime = data.location?.localtime;
updateSunSlider(days[0]?.astro, localTime);

    

const tips = generateLifestyleTips({
    uv: data.current.uv,
    humidity: data.current.humidity,
    wind_kph: data.current.wind_kph,
    temp_c: data.current.temp_c,
    vis_km: data.current.vis_km
});

renderLifestyleTips(tips);


}

// -------------------------
// SUN PROGRESS
// -------------------------
function updateSunSlider(astro, localTimeStr) {
    if (!astro) return;

    const now = new Date(localTimeStr.replace(" ", "T"));
    const today = now.toDateString();

    const sunrise = new Date(`${today} ${astro.sunrise}`);
    const sunset = new Date(`${today} ${astro.sunset}`);
    const nextSunrise = new Date(sunrise.getTime() + 24 * 60 * 60 * 1000);

    if (isNaN(sunrise) || isNaN(sunset)) return;

    const sunIcon = document.getElementById("sunIcon");
    const moonIcon = document.getElementById("moonIcon");
    const sunFill = document.getElementById("sunFill");
    const moonFill = document.getElementById("moonFill");

    // ☀️ DAYTIME
    if (now >= sunrise && now <= sunset) {
        let pct = ((now - sunrise) / (sunset - sunrise)) * 100;
        pct = Math.max(0, Math.min(100, pct));

        sunFill.style.width = pct + "%";
        moonFill.style.display = "none";

        sunIcon.style.left = pct + "%";
        sunIcon.style.display = "block";
        moonIcon.style.display = "none";
    }

    // 🌙 NIGHTTIME
    else {
        let nightStart = sunset;
        let nightEnd = now < sunrise ? sunrise : nextSunrise;

        let pct = ((now - nightStart) / (nightEnd - nightStart)) * 100;
        pct = Math.max(0, Math.min(100, pct));

        moonFill.style.display = "block";
        moonFill.style.width = pct + "%";

        moonIcon.style.left = pct + "%";
        moonIcon.style.display = "block";

        sunIcon.style.display = "none";
        sunFill.style.width = "0%";
    }
}


// -------------------------
// SEARCH BUTTON
// -------------------------
searchBtn.onclick = () => {
    const c = cityInput.value.trim();
    if (!c) return;
    loadWeather(c);
    loadForecast(c);
};

// -------------------------
// UNIT TOGGLE
// -------------------------
unitBtn.onclick = () => {
    currentUnit = currentUnit === "C" ? "F" : "C";
    unitBtn.textContent = `°${currentUnit}`;
    loadWeather(lastCity);
    loadForecast(lastCity);
};

// -------------------------
// FAVORITES
// -------------------------
saveLocBtn.onclick = () => {
    let fav = JSON.parse(localStorage.getItem("favorites") || "[]");
    if (!fav.includes(lastCity)) fav.push(lastCity);
    localStorage.setItem("favorites", JSON.stringify(fav));
    alert(lastCity + " added to favorites!");
};

// -------------------------
window.onload = () => {
    loadWeather("Hyderabad");
    loadForecast("Hyderabad");
};


function renderAirQuality(aqi) {
    let category = "";
    if (aqi <= 50) category = "Good";
    else if (aqi <= 100) category = "Moderate";
    else if (aqi <= 150) category = "Unhealthy for sensitive groups";
    else if (aqi <= 200) category = "Unhealthy";
    else if (aqi <= 300) category = "Very Unhealthy";
    else category = "Hazardous";

    document.getElementById("aqiValue").innerText = `AQI: ${aqi}`;
    document.getElementById("aqiCategory").innerText = category;

    const percent = Math.min(aqi / 5, 100);  
    document.getElementById("aqiPointer").style.left = percent + "%";
}

function generateLifestyleTips(weather) {

    return [
        {
            icon: "/icons/humidity.png",
            title: "Humidity",
            value: weather.humidity + "%",
            tip: weather.humidity < 40 ? "Use moisturisers" : "Humidity is normal"
        },
        {
            icon: "/icons/uv.png",
            title: "UV Index",
            value: weather.uv,
            tip:
                weather.uv <= 2 ? "Very low UV. Safe outdoors." :
                weather.uv <= 5 ? "Moderate UV. Use sunscreen." :
                "High UV! Avoid long sun exposure."
        },
        {
            icon: "/icons/car.png",
            title: "Long Drive",
            value: weather.wind_kph + " km/h wind",
            tip: weather.wind_kph < 10 ? "Great day for long Drive the car" : "Dusty winds. Drive safly take breakes"
        },
        {
            icon: "/icons/workout.png",
            title: "Workout",
            value: fmtTemp(weather.temp_c),
            tip: weather.temp_c < 18 ? "Better for indoor workouts" : "Good for outdoor workouts"
        },
        {
            icon: "/icons/visibility.png",
            title: "Visibility",
            value: weather.vis_km + " km",
            tip: weather.vis_km < 5 ? "Traffic may be slow" : "Good traffic conditions"
        },
        {
            icon: "/icons/mosquito.png",
            title: "Mosquito Level",
            value: weather.temp_c + "°C",
            tip: weather.temp_c < 20 ? "Few mosquitoes" : "Mosquito activity possible"
        }
    ];
}

function renderLifestyleTips(tips) {
    let container = document.getElementById("tipsContainer");
    container.innerHTML = "";

    tips.forEach(t => {
        container.innerHTML += `
            <div class="tip-card">
                <img src="${t.icon}" alt="">
                <div class="tip-title">${t.title}</div>
                <div class="tip-value">${t.value}</div>
                <div class="tip-detail">${t.tip}</div>
            </div>
        `;
    });
}


function highlightMatch(text, query) {
    const regex = new RegExp(`(${query})`, "i");
    return text.replace(regex, `<span class="highlight">$1</span>`);
}

const autoBox = document.getElementById("autocompleteBox");

cityInput.addEventListener("input", async () => {
    let q = cityInput.value.trim();

    if (q.length < 2) {
        autoBox.style.display = "none";
        return;
    }

   const res = await fetch(`/api/weather/search?q=${q}`);
const data = await res.json();

renderAutoComplete(data.results || [], q);

});

let currentIndex = -1;

function renderAutoComplete(list, query) {
    if (!list || list.length === 0) {
        autoBox.style.display = "none";
        return;
    }

    autoBox.innerHTML = "";
    currentIndex = -1;

    list.forEach(city => {
        const div = document.createElement("div");
        div.className = "autocomplete-item";

        div.innerHTML = `
            ${highlightMatch(city.name, query)}
            <small>${city.region}, ${city.country}</small>
        `;

        div.onclick = () => {
            selectCity(city.name);
        };

        autoBox.appendChild(div);
    });

    autoBox.style.display = "block";
}

function selectCity(name) {
    cityInput.value = name;
    autoBox.style.display = "none";
    loadWeather(name);
    loadForecast(name);
}

cityInput.addEventListener("keydown", (e) => {
    const items = autoBox.querySelectorAll(".autocomplete-item");

    if (e.key === "ArrowDown") {
        currentIndex = (currentIndex + 1) % items.length;
    }
    if (e.key === "ArrowUp") {
        currentIndex = (currentIndex - 1 + items.length) % items.length;
    }
    if (e.key === "Enter") {
        if (currentIndex >= 0 && items[currentIndex]) {
            items[currentIndex].click();
            return;
        }
    }

    items.forEach((item, index) => {
        item.classList.toggle("active", index === currentIndex);
    });
});

document.addEventListener("click", (e) => {
    if (!e.target.closest(".search-wrapper")) {
        autoBox.style.display = "none";
    }
});
