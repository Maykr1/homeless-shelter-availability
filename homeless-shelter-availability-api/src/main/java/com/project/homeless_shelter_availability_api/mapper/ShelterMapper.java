package com.project.homeless_shelter_availability_api.mapper;

import com.project.homeless_shelter_availability_api.dto.CoordinatesResponse;
import com.project.homeless_shelter_availability_api.dto.ShelterResponse;
import com.project.homeless_shelter_availability_api.model.Shelter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShelterMapper {

    public ShelterResponse toResponse(Shelter shelter, Double distanceMiles) {
        return ShelterResponse.builder()
                .id(shelter.getId())
                .slug(shelter.getSlug())
                .name(shelter.getName())
                .address(shelter.getAddress())
                .city(shelter.getCity())
                .state(shelter.getState())
                .zip(shelter.getZip())
                .phone(shelter.getPhone())
                .website(shelter.getWebsite())
                .coordinates(coordinatesOf(shelter))
                .hours(shelter.getHours())
                .category(shelter.getCategory())
                .services(safeList(shelter.getServices()))
                .eligibility(safeList(shelter.getEligibility()))
                .availableBeds(shelter.getAvailableBeds())
                .totalBeds(shelter.getTotalBeds())
                .availabilityStatus(resolveAvailabilityStatus(shelter.getAvailableBeds()))
                .lastUpdated(shelter.getLastSourceUpdatedAt())
                .description(shelter.getDescription())
                .distanceMiles(distanceMiles)
                .build();
    }

    public String resolveAvailabilityStatus(Integer availableBeds) {
        if (availableBeds == null) {
            return "unknown";
        }
        if (availableBeds <= 0) {
            return "full";
        }
        if (availableBeds <= 5) {
            return "limited";
        }
        return "available";
    }

    private CoordinatesResponse coordinatesOf(Shelter shelter) {
        if (shelter.getLatitude() == null || shelter.getLongitude() == null) {
            return null;
        }
        return CoordinatesResponse.builder()
                .lat(shelter.getLatitude())
                .lng(shelter.getLongitude())
                .build();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
