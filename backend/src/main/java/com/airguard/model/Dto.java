package com.airguard.model;

import jakarta.validation.constraints.*;
import lombok.*;
import com.airguard.model.Activity.ActivityType;
import com.airguard.model.Activity.HealthProfile;
import com.airguard.model.Activity.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Dto {

    // ─── Auth ─────────────────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank @Size(min = 3, max = 30) private String username;
        @NotBlank @Email private String email;
        @NotBlank @Size(min = 8, max = 64) private String password;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }

    @Data @Builder
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private UserProfile user;
    }

    @Data @Builder
    public static class RefreshRequest {
        private String refreshToken;
    }

    // ─── User ─────────────────────────────────────────────────────────────────

    @Data @Builder
    public static class UserProfile {
        private Long id;
        private String username;
        private String email;
        private int xp;
        private int level;
        private int streakDays;
        private String rank;
        private int xpToNextLevel;
        private LocalDateTime createdAt;
    }

    // ─── AQI ──────────────────────────────────────────────────────────────────

    @Data @Builder
    public static class AqiResponse {
        private double latitude;
        private double longitude;
        private String cityName;
        private double usAqi;
        private double pm25;
        private double pm10;
        private double ozone;
        private double no2;
        private double so2;
        private double carbonMonoxide;
        private String category;
        private String color;
        private List<HourlyForecast> hourlyForecast;
        private String source;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class HourlyForecast {
        private String time;
        private double aqi;
        private String category;
    }

    @Data @Builder
    public static class GeoSearchResult {
        private String name;
        private String country;
        private String admin1;
        private double latitude;
        private double longitude;
    }

    // ─── Exposure ─────────────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ExposureRequest {
        @NotNull private Double latitude;
        @NotNull private Double longitude;
        @NotNull private ActivityType activityType;
        @NotNull private HealthProfile healthProfile;
        @DecimalMin("0.1") @DecimalMax("24.0") private double durationHours = 1.0;
    }

    @Data @Builder
    public static class ExposureResponse {
        private Long id;
        private double aqi;
        private double pm25;
        private double pm10;
        private double exposureScore;
        private RiskLevel riskLevel;
        private double activityMultiplier;
        private double vulnerabilityMultiplier;
        private double durationHours;
        private int xpEarned;
        private String cityName;
        private List<String> recommendations;
        private String primaryAdvice;
        private LocalDateTime timestamp;
    }

    @Data @Builder
    public static class DashboardStats {
        private int totalActivities;
        private double avgAqi7Days;
        private double avgAqi30Days;
        private double totalExposureScore;
        private int totalXp;
        private int currentLevel;
        private int streakDays;
        private long lowRiskCount;
        private String rank;
        private List<WeeklyAqi> weeklyAqiTrend;
        private List<ActivitySummary> recentActivities;
        private List<AchievementDto> achievements;
        private Map<String, Long> riskDistribution;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WeeklyAqi {
        private String date;
        private double avgAqi;
        private String category;
    }

    @Data @Builder
    public static class ActivitySummary {
        private Long id;
        private String cityName;
        private double aqi;
        private double exposureScore;
        private RiskLevel riskLevel;
        private ActivityType activityType;
        private double durationHours;
        private int xpEarned;
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AchievementDto {
        private String id;
        private String title;
        private String description;
        private String icon;
        private boolean unlocked;
        private int progress;
        private int target;
    }

    @Data @Builder
    public static class ApiError {
        private int status;
        private String error;
        private String message;
        private LocalDateTime timestamp;
    }
}
