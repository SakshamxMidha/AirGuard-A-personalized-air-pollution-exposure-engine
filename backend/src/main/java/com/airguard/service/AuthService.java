package com.airguard.service;

import com.airguard.model.Dto.*;
import com.airguard.model.User;
import com.airguard.repository.UserRepository;
import com.airguard.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authManager;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
            .username(req.getUsername())
            .email(req.getEmail())
            .password(passwordEncoder.encode(req.getPassword()))
            .xp(0)
            .level(1)
            .streakDays(0)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword())
        );
        User user = userRepository.findByUsername(req.getUsername())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            String username = jwtUtils.extractUsername(refreshToken);
            if (username == null || jwtUtils.isTokenExpired(refreshToken)) {
                throw new IllegalArgumentException("Invalid refresh token");
            }
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return buildAuthResponse(user);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
    }

    public UserProfile getProfile(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toProfile(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
            .accessToken(jwtUtils.generateAccessToken(user.getUsername()))
            .refreshToken(jwtUtils.generateRefreshToken(user.getUsername()))
            .user(toProfile(user))
            .build();
    }

    public static UserProfile toProfile(User user) {
        int xpForNextLevel = xpForLevel(user.getLevel() + 1);
        int xpForCurrentLevel = xpForLevel(user.getLevel());
        return UserProfile.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .xp(user.getXp())
            .level(user.getLevel())
            .streakDays(user.getStreakDays())
            .rank(rankFromLevel(user.getLevel()))
            .xpToNextLevel(xpForNextLevel - Math.min(user.getXp(), xpForNextLevel))
            .createdAt(user.getCreatedAt())
            .build();
    }

    private static int xpForLevel(int level) {
        return 50 * (level - 1) * (level - 1);
    }

    private static String rankFromLevel(int level) {
        if (level < 5) return "Air Rookie";
        if (level < 10) return "Breath Watcher";
        if (level < 20) return "Clean Air Scout";
        if (level < 35) return "Pollution Tracker";
        if (level < 50) return "Air Quality Analyst";
        if (level < 70) return "Environmental Guardian";
        return "Air Master";
    }
}
