package com.airguard.controller;

import com.airguard.model.Dto.*;
import com.airguard.service.AqiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/aqi")
@RequiredArgsConstructor
public class AqiController {

    private final AqiService aqiService;

    @GetMapping("/current")
    public ResponseEntity<AqiResponse> current(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        AqiResponse resp = aqiService.fetchAqi(latitude, longitude);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/public/current")
    public ResponseEntity<AqiResponse> currentPublic(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        return ResponseEntity.ok(aqiService.fetchAqi(latitude, longitude));
    }

    @GetMapping("/search")
    public ResponseEntity<List<GeoSearchResult>> search(@RequestParam String q) {
        return ResponseEntity.ok(aqiService.searchCity(q));
    }

    @GetMapping("/reverse")
    public ResponseEntity<String> reverse(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        return ResponseEntity.ok(aqiService.reverseGeocode(latitude, longitude));
    }
}
