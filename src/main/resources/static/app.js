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
    let data = await res.json();

    const cur = data.current ?? data;
    const loc = data.location ?? { name: city };

    const temp = cur.temp_c ?? cur.temperature;
    const condition = cur.condition?.text ?? cur.condition ?? "Clear";
    const icon = cur.condition?.icon;

    // UI
    locationDisplay.textContent = loc.name;
    bigTemp.textContent = fmtTemp(temp);
    bigCond.textContent = condition;

    if (icon) {
        bigIcon.src = icon.startsWith("http") ? icon : "https:" + icon;
    }

    metaHum.textContent = cur.humidity ?? "--";
    metaWind.textContent = (cur.wind_kph ?? cur.wind ?? "--") + " km/h";

    defaultCityEl.textContent = `${loc.name} — ${fmtTemp(temp)} | ${condition}`;

    updateBackground(condition);

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

    updateSunSlider(days[0]?.astro);

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
function updateSunSlider(astro) {
    if (!astro) return;

    const now = new Date();
    const today = now.toDateString();

    let sunrise = new Date(`${today} ${astro.sunrise}`);
    let sunset = new Date(`${today} ${astro.sunset}`);

    if (isNaN(sunrise)) sunrise = now;
    if (isNaN(sunset)) sunset = now;

    let pct = ((now - sunrise) / (sunset - sunrise)) * 100;
    pct = Math.max(0, Math.min(100, pct));

    sunProgress.value = pct;
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
