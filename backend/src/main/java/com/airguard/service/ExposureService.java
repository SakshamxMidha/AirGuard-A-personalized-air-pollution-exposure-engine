package com.airguard.service;

import com.airguard.model.Activity;
import com.airguard.model.Activity.*;
import com.airguard.model.Dto.*;
import com.airguard.model.User;
import com.airguard.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExposureService {

    private final ActivityRepository activityRepository;
    private final AqiService aqiService;

    @Transactional
    public ExposureResponse calculateExposure(User user, ExposureRequest req) {
        // Fetch live AQI
        AqiResponse aqiData = aqiService.fetchAqi(req.getLatitude(), req.getLongitude());

        // Reverse geocode
        String city = aqiData.getCityName() != null && !aqiData.getCityName().isEmpty()
            ? aqiData.getCityName()
            : aqiService.reverseGeocode(req.getLatitude(), req.getLongitude());

        // Calculate exposure score
        double actMult = req.getActivityType().multiplier;
        double vulMult = req.getHealthProfile().multiplier;
        double exposureScore = aqiData.getUsAqi() * actMult * req.getDurationHours() * vulMult;

        // ML-inspired risk classification using multi-factor scoring
        RiskLevel riskLevel = classifyRisk(aqiData.getUsAqi(), exposureScore, req.getHealthProfile(), req.getActivityType());

        int xp = riskLevel.xpReward();
        List<String> recs = buildRecommendations(aqiData.getUsAqi(), riskLevel, req.getActivityType(), req.getHealthProfile());

        // Persist
        Activity activity = Activity.builder()
            .user(user)
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .cityName(city)
            .aqi(aqiData.getUsAqi())
            .pm25(aqiData.getPm25())
            .pm10(aqiData.getPm10())
            .ozone(aqiData.getOzone())
            .no2(aqiData.getNo2())
            .activityType(req.getActivityType())
            .healthProfile(req.getHealthProfile())
            .durationHours(req.getDurationHours())
            .exposureScore(exposureScore)
            .riskLevel(riskLevel)
            .xpEarned(xp)
            .recommendations(String.join("|", recs))
            .build();

        activityRepository.save(activity);

        // Update user XP and level
        user.setXp(user.getXp() + xp);
        user.setLevel(computeLevel(user.getXp()));
        user.setLastActive(LocalDateTime.now());
        updateStreak(user);

        return ExposureResponse.builder()
            .id(activity.getId())
            .aqi(aqiData.getUsAqi())
            .pm25(aqiData.getPm25())
            .pm10(aqiData.getPm10())
            .exposureScore(exposureScore)
            .riskLevel(riskLevel)
            .activityMultiplier(actMult)
            .vulnerabilityMultiplier(vulMult)
            .durationHours(req.getDurationHours())
            .xpEarned(xp)
            .cityName(city)
            .recommendations(recs)
            .primaryAdvice(recs.isEmpty() ? "" : recs.get(0))
            .timestamp(LocalDateTime.now())
            .build();
    }

    public DashboardStats getDashboard(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime week = now.minusDays(7);
        LocalDateTime month = now.minusDays(30);

        long total = activityRepository.countByUser(user);
        Double avg7 = activityRepository.avgAqiSince(user, week);
        Double avg30 = activityRepository.avgAqiSince(user, month);
        long lowCount = activityRepository.countLowRiskByUser(user);

        List<Activity> recent = activityRepository.findByUserOrderByCreatedAtDesc(
            user, PageRequest.of(0, 10)
        );

        // 7-day trend
        List<WeeklyAqi> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).toLocalDate().atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);
            List<Activity> dayActivities = activityRepository.findByUserAndDateRange(user, dayStart, dayEnd);
            double dayAvg = dayActivities.stream().mapToDouble(Activity::getAqi).average().orElse(0);
            trend.add(WeeklyAqi.builder()
                .date(dayStart.toLocalDate().toString())
                .avgAqi(Math.round(dayAvg * 10.0) / 10.0)
                .category(AqiService.aqiCategory(dayAvg))
                .build());
        }

        // Risk distribution
        Map<String, Long> dist = recent.stream()
            .collect(Collectors.groupingBy(a -> a.getRiskLevel().name(), Collectors.counting()));

        // Achievements
        List<AchievementDto> achievements = computeAchievements(user, total, lowCount);

        List<ActivitySummary> summaries = recent.stream().map(a -> ActivitySummary.builder()
            .id(a.getId())
            .cityName(a.getCityName())
            .aqi(a.getAqi())
            .exposureScore(Math.round(a.getExposureScore() * 10.0) / 10.0)
            .riskLevel(a.getRiskLevel())
            .activityType(a.getActivityType())
            .durationHours(a.getDurationHours())
            .xpEarned(a.getXpEarned())
            .createdAt(a.getCreatedAt())
            .build()).toList();

        return DashboardStats.builder()
            .totalActivities((int) total)
            .avgAqi7Days(avg7 != null ? Math.round(avg7 * 10.0) / 10.0 : 0)
            .avgAqi30Days(avg30 != null ? Math.round(avg30 * 10.0) / 10.0 : 0)
            .totalXp(user.getXp())
            .currentLevel(user.getLevel())
            .streakDays(user.getStreakDays())
            .lowRiskCount(lowCount)
            .rank(rankFromLevel(user.getLevel()))
            .weeklyAqiTrend(trend)
            .recentActivities(summaries)
            .achievements(achievements)
            .riskDistribution(dist)
            .build();
    }

    public List<ActivitySummary> getHistory(User user) {
        return activityRepository.findByUserOrderByCreatedAtDesc(user).stream()
            .map(a -> ActivitySummary.builder()
                .id(a.getId())
                .cityName(a.getCityName())
                .aqi(a.getAqi())
                .exposureScore(a.getExposureScore())
                .riskLevel(a.getRiskLevel())
                .activityType(a.getActivityType())
                .durationHours(a.getDurationHours())
                .xpEarned(a.getXpEarned())
                .createdAt(a.getCreatedAt())
                .build())
            .toList();
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    private RiskLevel classifyRisk(double aqi, double score, HealthProfile profile, ActivityType activity) {
        // Multi-factor classification: base from score, then adjust for AQI and vulnerability
        RiskLevel base = RiskLevel.fromScore(score);

        // Bump up if AQI is high regardless of score
        if (aqi > 200 && base.ordinal() < RiskLevel.VERY_HIGH.ordinal()) {
            return RiskLevel.HIGH;
        }
        // Sensitive groups bump one level
        if ((profile == HealthProfile.ASTHMATIC || profile == HealthProfile.CHILD)
                && aqi > 100 && base == RiskLevel.LOW) {
            return RiskLevel.MODERATE;
        }
        // Vigorous exercise in moderate AQI bumps
        if ((activity == ActivityType.RUNNING || activity == ActivityType.CYCLING)
                && aqi > 100 && base == RiskLevel.LOW) {
            return RiskLevel.MODERATE;
        }
        return base;
    }

    private List<String> buildRecommendations(double aqi, RiskLevel risk, ActivityType activity, HealthProfile profile) {
        List<String> recs = new ArrayList<>();
        boolean sensitive = profile == HealthProfile.ASTHMATIC || profile == HealthProfile.CHILD;

        switch (risk) {
            case LOW -> {
                recs.add("Air quality is good — enjoy your outdoor activity!");
                recs.add("Great conditions for " + activity.label.toLowerCase() + " today.");
                if (sensitive) recs.add("Keep rescue inhaler accessible as a precaution.");
            }
            case MODERATE -> {
                recs.add("Acceptable air quality, but unusually sensitive people should consider reducing prolonged exertion.");
                if (activity == ActivityType.RUNNING || activity == ActivityType.CYCLING)
                    recs.add("Consider reducing intensity or shortening your session.");
                if (sensitive) recs.add("Monitor symptoms closely; take medication as prescribed.");
                recs.add("Stay hydrated and take breaks if you feel respiratory discomfort.");
            }
            case HIGH -> {
                recs.add("Air quality is unhealthy — consider moving activity indoors.");
                if (sensitive) recs.add("Highly sensitive individuals should avoid all outdoor exertion.");
                recs.add("Wear an N95 mask if you must go outdoors.");
                recs.add("Close windows and use air purifier indoors.");
                if (activity == ActivityType.RUNNING || activity == ActivityType.CYCLING)
                    recs.add("Reschedule vigorous outdoor activities to early morning or after rain.");
            }
            case VERY_HIGH -> {
                recs.add("⚠️ Hazardous air quality — stay indoors as much as possible.");
                recs.add("Avoid all outdoor physical activity until air quality improves.");
                recs.add("Use N95/KN95 mask if you must venture outside.");
                recs.add("Keep windows and doors closed; run air purifier on high.");
                if (sensitive) recs.add("Consider evacuating to cleaner air environment if conditions persist.");
            }
        }
        if (aqi > 100) recs.add("Check air quality again in 1–2 hours as conditions may change.");
        return recs;
    }

    private int computeLevel(int xp) {
        // Level = floor(sqrt(xp / 50)) + 1, capped at 100
        return Math.min(100, (int) Math.floor(Math.sqrt(xp / 50.0)) + 1);
    }

    private String rankFromLevel(int level) {
        if (level < 5) return "Air Rookie";
        if (level < 10) return "Breath Watcher";
        if (level < 20) return "Clean Air Scout";
        if (level < 35) return "Pollution Tracker";
        if (level < 50) return "Air Quality Analyst";
        if (level < 70) return "Environmental Guardian";
        return "Air Master";
    }

    private void updateStreak(User user) {
        LocalDateTime last = user.getLastActive();
        if (last == null) {
            user.setStreakDays(1);
            return;
        }
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
            last.toLocalDate(), LocalDateTime.now().toLocalDate()
        );
        if (daysBetween == 1) user.setStreakDays(user.getStreakDays() + 1);
        else if (daysBetween > 1) user.setStreakDays(1);
    }

    private List<AchievementDto> computeAchievements(User user, long total, long lowCount) {
        return List.of(
            AchievementDto.builder()
                .id("first_check")
                .title("First Check")
                .description("Log your first air quality check")
                .icon("🌱")
                .unlocked(total >= 1)
                .progress((int) Math.min(total, 1))
                .target(1)
                .build(),
            AchievementDto.builder()
                .id("ten_checks")
                .title("Regular Tracker")
                .description("Log 10 air quality checks")
                .icon("📊")
                .unlocked(total >= 10)
                .progress((int) Math.min(total, 10))
                .target(10)
                .build(),
            AchievementDto.builder()
                .id("clean_air_hero")
                .title("Clean Air Hero")
                .description("Log 5 activities in Low risk conditions")
                .icon("🌿")
                .unlocked(lowCount >= 5)
                .progress((int) Math.min(lowCount, 5))
                .target(5)
                .build(),
            AchievementDto.builder()
                .id("streak_7")
                .title("Week Warrior")
                .description("Maintain a 7-day streak")
                .icon("🔥")
                .unlocked(user.getStreakDays() >= 7)
                .progress(Math.min(user.getStreakDays(), 7))
                .target(7)
                .build(),
            AchievementDto.builder()
                .id("level_5")
                .title("Rising Guardian")
                .description("Reach Level 5")
                .icon("⭐")
                .unlocked(user.getLevel() >= 5)
                .progress(Math.min(user.getLevel(), 5))
                .target(5)
                .build(),
            AchievementDto.builder()
                .id("fifty_checks")
                .title("Dedicated Monitor")
                .description("Log 50 air quality checks")
                .icon("🏆")
                .unlocked(total >= 50)
                .progress((int) Math.min(total, 50))
                .target(50)
                .build()
        );
    }
}
