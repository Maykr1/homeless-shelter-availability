package com.project.homeless_shelter_availability_api.dto;

import java.util.List;

public record ShelterQuery(
        String query,
        Double lat,
        Double lng,
        Double radiusMiles,
        List<String> category,
        boolean bedsAvailableOnly
) {
}
