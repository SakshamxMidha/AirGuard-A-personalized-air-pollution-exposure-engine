package com.airguard.repository;

import com.airguard.model.Activity;
import com.airguard.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByUserOrderByCreatedAtDesc(User user);

    List<Activity> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUser(User user);

    @Query("SELECT COALESCE(SUM(a.xpEarned), 0) FROM Activity a WHERE a.user = :user")
    int sumXpByUser(@Param("user") User user);

    @Query("SELECT COALESCE(AVG(a.aqi), 0) FROM Activity a WHERE a.user = :user AND a.createdAt >= :since")
    Double avgAqiSince(@Param("user") User user, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.user = :user AND a.riskLevel = 'LOW'")
    long countLowRiskByUser(@Param("user") User user);

    @Query("SELECT COUNT(DISTINCT DATE(a.createdAt)) FROM Activity a WHERE a.user = :user AND a.createdAt >= :since")
    long countDistinctDaysSince(@Param("user") User user, @Param("since") LocalDateTime since);

    @Query("SELECT a FROM Activity a WHERE a.user = :user AND a.createdAt >= :from AND a.createdAt < :to")
    List<Activity> findByUserAndDateRange(@Param("user") User user,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}
