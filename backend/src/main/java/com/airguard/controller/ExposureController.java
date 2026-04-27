package com.airguard.controller;

import com.airguard.model.Dto.*;
import com.airguard.model.User;
import com.airguard.repository.UserRepository;
import com.airguard.service.ExposureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exposure")
@RequiredArgsConstructor
public class ExposureController {

    private final ExposureService exposureService;
    private final UserRepository userRepository;

    @PostMapping("/calculate")
    public ResponseEntity<ExposureResponse> calculate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ExposureRequest req) {
        User user = getUser(userDetails);
        ExposureResponse resp = exposureService.calculateExposure(user, req);
        userRepository.save(user);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> dashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(exposureService.getDashboard(user));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ActivitySummary>> history(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(exposureService.getHistory(user));
    }

    private User getUser(UserDetails ud) {
        return userRepository.findByUsername(ud.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
