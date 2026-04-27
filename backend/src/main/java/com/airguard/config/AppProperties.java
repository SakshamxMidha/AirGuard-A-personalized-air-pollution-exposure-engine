package com.airguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "airguard")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Api api = new Api();
    private Cache cache = new Cache();

    @Data
    public static class Jwt {
        private String secret = "default-secret-change-in-production-256bit-min-length";
        private long accessTokenExpiry = 3600000L;
        private long refreshTokenExpiry = 2592000000L;
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";
    }

    @Data
    public static class Api {
        private OpenMeteo openMeteo = new OpenMeteo();
        private Nominatim nominatim = new Nominatim();
        private Waqi waqi = new Waqi();

        @Data
        public static class OpenMeteo {
            private String baseUrl = "https://api.open-meteo.com/v1";
            private String aqiUrl = "https://air-quality-api.open-meteo.com/v1";
            private String geocodingUrl = "https://geocoding-api.open-meteo.com/v1";
        }

        @Data
        public static class Nominatim {
            private String url = "https://nominatim.openstreetmap.org";
        }

        @Data
        public static class Waqi {
            private String url = "https://api.waqi.info";
            private String token = "demo";
        }
    }

    @Data
    public static class Cache {
        private int aqiTtl = 900;
    }
}
