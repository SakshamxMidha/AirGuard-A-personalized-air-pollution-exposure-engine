package com.airguard.service;

import com.airguard.config.AppProperties;
import com.airguard.model.Dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AqiService {

    private final RestTemplate restTemplate;
    private final AppProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Cacheable(value = "aqi", key = "#latitude + ',' + #longitude")
    public AqiResponse fetchAqi(double latitude, double longitude) {
        try {
            return fetchFromOpenMeteo(latitude, longitude);
        } catch (Exception e) {
            log.warn("Open-Meteo AQI failed, trying WAQI fallback: {}", e.getMessage());
            try {
                return fetchFromWaqi(latitude, longitude);
            } catch (Exception e2) {
                log.warn("WAQI fallback failed, using mock data: {}", e2.getMessage());
                return buildMockAqi(latitude, longitude);
            }
        }
    }

    private AqiResponse fetchFromOpenMeteo(double lat, double lon) throws Exception {
        String url = String.format(
            "%s/air-quality?latitude=%.4f&longitude=%.4f" +
            "&hourly=us_aqi,pm2_5,pm10,ozone,nitrogen_dioxide,sulphur_dioxide,carbon_monoxide" +
            "&forecast_days=1&timezone=auto",
            props.getApi().getOpenMeteo().getAqiUrl(), lat, lon
        );

        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = objectMapper.readTree(response);
        JsonNode hourly = root.get("hourly");

        int currentHour = java.time.LocalTime.now().getHour();
        JsonNode times = hourly.get("time");
        JsonNode aqiArr = hourly.get("us_aqi");
        JsonNode pm25Arr = hourly.get("pm2_5");
        JsonNode pm10Arr = hourly.get("pm10");
        JsonNode ozoneArr = hourly.get("ozone");
        JsonNode no2Arr = hourly.get("nitrogen_dioxide");
        JsonNode so2Arr = hourly.get("sulphur_dioxide");
        JsonNode coArr = hourly.get("carbon_monoxide");

        double aqi = getDouble(aqiArr, currentHour, 0);
        double pm25 = getDouble(pm25Arr, currentHour, 0);
        double pm10 = getDouble(pm10Arr, currentHour, 0);
        double ozone = getDouble(ozoneArr, currentHour, 0);
        double no2 = getDouble(no2Arr, currentHour, 0);
        double so2 = getDouble(so2Arr, currentHour, 0);
        double co = getDouble(coArr, currentHour, 0);

        // Build 24h forecast
        List<HourlyForecast> forecast = new ArrayList<>();
        for (int i = 0; i < Math.min(24, times.size()); i++) {
            double forecastAqi = getDouble(aqiArr, i, 0);
            forecast.add(HourlyForecast.builder()
                .time(times.get(i).asText())
                .aqi(forecastAqi)
                .category(aqiCategory(forecastAqi))
                .build());
        }

        return AqiResponse.builder()
            .latitude(lat)
            .longitude(lon)
            .usAqi(aqi)
            .pm25(pm25)
            .pm10(pm10)
            .ozone(ozone)
            .no2(no2)
            .so2(so2)
            .carbonMonoxide(co)
            .category(aqiCategory(aqi))
            .color(aqiColor(aqi))
            .hourlyForecast(forecast)
            .source("Open-Meteo")
            .build();
    }

    private AqiResponse fetchFromWaqi(double lat, double lon) throws Exception {
        String token = props.getApi().getWaqi().getToken();
        String url = String.format(
            "%s/feed/geo:%.4f;%.4f/?token=%s",
            props.getApi().getWaqi().getUrl(), lat, lon, token
        );

        String response = restTemplate.getForObject(url, String.class);
        JsonNode root = objectMapper.readTree(response);

        if (!"ok".equals(root.get("status").asText())) {
            throw new RuntimeException("WAQI returned non-ok status");
        }

        JsonNode data = root.get("data");
        double aqi = data.get("aqi").asDouble();
        JsonNode iaqi = data.get("iaqi");

        double pm25 = iaqi.has("pm25") ? iaqi.get("pm25").get("v").asDouble() : 0;
        double pm10 = iaqi.has("pm10") ? iaqi.get("pm10").get("v").asDouble() : 0;
        double ozone = iaqi.has("o3") ? iaqi.get("o3").get("v").asDouble() : 0;
        double no2 = iaqi.has("no2") ? iaqi.get("no2").get("v").asDouble() : 0;
        String city = data.has("city") ? data.get("city").get("name").asText() : "";

        return AqiResponse.builder()
            .latitude(lat)
            .longitude(lon)
            .cityName(city)
            .usAqi(aqi)
            .pm25(pm25)
            .pm10(pm10)
            .ozone(ozone)
            .no2(no2)
            .category(aqiCategory(aqi))
            .color(aqiColor(aqi))
            .hourlyForecast(List.of())
            .source("WAQI")
            .build();
    }

    private AqiResponse buildMockAqi(double lat, double lon) {
        // Deterministic pseudo-random based on coords so same location gives same value
        double seed = Math.abs(lat * 13.7 + lon * 7.3) % 200;
        double aqi = 20 + seed;
        double pm25 = aqi * 0.18;
        double pm10 = aqi * 0.28;

        return AqiResponse.builder()
            .latitude(lat)
            .longitude(lon)
            .usAqi(aqi)
            .pm25(pm25)
            .pm10(pm10)
            .ozone(45)
            .no2(12)
            .category(aqiCategory(aqi))
            .color(aqiColor(aqi))
            .hourlyForecast(List.of())
            .source("Estimated")
            .build();
    }

    @Cacheable(value = "geocoding", key = "#query")
    public List<GeoSearchResult> searchCity(String query) {
        try {
            String url = String.format(
                "%s/search?name=%s&count=5&language=en&format=json",
                props.getApi().getOpenMeteo().getGeocodingUrl(),
                java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
            );
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode results = root.get("results");

            List<GeoSearchResult> list = new ArrayList<>();
            if (results != null) {
                for (JsonNode r : results) {
                    list.add(GeoSearchResult.builder()
                        .name(r.get("name").asText())
                        .country(r.has("country") ? r.get("country").asText() : "")
                        .admin1(r.has("admin1") ? r.get("admin1").asText() : "")
                        .latitude(r.get("latitude").asDouble())
                        .longitude(r.get("longitude").asDouble())
                        .build());
                }
            }
            return list;
        } catch (Exception e) {
            log.warn("Geocoding failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Cacheable(value = "reverse-geocoding", key = "#lat + ',' + #lon")
    public String reverseGeocode(double lat, double lon) {
        try {
            String url = String.format(
                "%s/reverse?lat=%.6f&lon=%.6f&format=json",
                props.getApi().getNominatim().getUrl(), lat, lon
            );
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode address = root.get("address");
            if (address != null) {
                if (address.has("city")) return address.get("city").asText();
                if (address.has("town")) return address.get("town").asText();
                if (address.has("village")) return address.get("village").asText();
                if (address.has("county")) return address.get("county").asText();
            }
            return root.has("display_name") ? root.get("display_name").asText().split(",")[0] : "Unknown";
        } catch (Exception e) {
            log.debug("Reverse geocoding failed: {}", e.getMessage());
            return "Unknown Location";
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private double getDouble(JsonNode arr, int index, double fallback) {
        if (arr == null || arr.size() <= index) return fallback;
        JsonNode node = arr.get(index);
        return (node == null || node.isNull()) ? fallback : node.asDouble();
    }

    public static String aqiCategory(double aqi) {
        if (aqi <= 50) return "Good";
        if (aqi <= 100) return "Moderate";
        if (aqi <= 150) return "Unhealthy for Sensitive Groups";
        if (aqi <= 200) return "Unhealthy";
        if (aqi <= 300) return "Very Unhealthy";
        return "Hazardous";
    }

    public static String aqiColor(double aqi) {
        if (aqi <= 50) return "#00e400";
        if (aqi <= 100) return "#ffff00";
        if (aqi <= 150) return "#ff7e00";
        if (aqi <= 200) return "#ff0000";
        if (aqi <= 300) return "#8f3f97";
        return "#7e0023";
    }
}
