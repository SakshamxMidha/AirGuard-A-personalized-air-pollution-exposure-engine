package com.airguard.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private String cityName;

    @Column(nullable = false)
    private double aqi;

    private double pm25;
    private double pm10;
    private double ozone;
    private double no2;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private HealthProfile healthProfile;

    @Column(nullable = false)
    private double durationHours;

    @Column(nullable = false)
    private double exposureScore;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private int xpEarned;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(updatable = false, nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum ActivityType {
        RUNNING(2.5, "Running"),
        HIKING(2.2, "Hiking"),
        CYCLING(2.0, "Cycling"),
        WALKING(1.5, "Walking"),
        YOGA(1.3, "Yoga"),
        SITTING(1.0, "Sitting");

        public final double multiplier;
        public final String label;

        ActivityType(double multiplier, String label) {
            this.multiplier = multiplier;
            this.label = label;
        }
    }

    public enum HealthProfile {
        ASTHMATIC(2.0, "Asthmatic"),
        CHILD(1.6, "Child"),
        ELDERLY(1.5, "Elderly"),
        NORMAL(1.0, "Normal Adult");

        public final double multiplier;
        public final String label;

        HealthProfile(double multiplier, String label) {
            this.multiplier = multiplier;
            this.label = label;
        }
    }

    public enum RiskLevel {
        LOW, MODERATE, HIGH, VERY_HIGH;

        public static RiskLevel fromScore(double score) {
            if (score <= 100) return LOW;
            if (score <= 300) return MODERATE;
            if (score <= 600) return HIGH;
            return VERY_HIGH;
        }

        public int xpReward() {
            return switch (this) {
                case LOW -> 50;
                case MODERATE -> 30;
                case HIGH -> 15;
                case VERY_HIGH -> 5;
            };
        }
    }
}
