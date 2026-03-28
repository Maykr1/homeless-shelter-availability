package com.project.homeless_shelter_availability_api.controller;

import com.project.homeless_shelter_availability_api.dto.ShelterQuery;
import com.project.homeless_shelter_availability_api.dto.ShelterResponse;
import com.project.homeless_shelter_availability_api.service.ShelterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/shelters")
public class ShelterController {

    private final ShelterService shelterService;

    public ShelterController(ShelterService shelterService) {
        this.shelterService = shelterService;
    }

    @GetMapping
    public ResponseEntity<List<ShelterResponse>> getShelters(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusMiles,
            @RequestParam(required = false) List<String> category,
            @RequestParam(defaultValue = "false") boolean bedsAvailableOnly
    ) {
        ShelterQuery shelterQuery = new ShelterQuery(query, lat, lng, radiusMiles, category, bedsAvailableOnly);
        return ResponseEntity.ok(shelterService.searchShelters(shelterQuery));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ShelterResponse> getShelterBySlug(
            @PathVariable String slug,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        return ResponseEntity.ok(shelterService.getShelterBySlug(slug, lat, lng));
    }
}
