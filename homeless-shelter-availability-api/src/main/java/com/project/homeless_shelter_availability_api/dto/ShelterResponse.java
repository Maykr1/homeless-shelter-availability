package com.project.homeless_shelter_availability_api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShelterResponse {
    Long id;
    String slug;
    String name;
    String address;
    String city;
    String state;
    String zip;
    String phone;
    String website;
    CoordinatesResponse coordinates;
    String hours;
    String category;
    List<String> services;
    List<String> eligibility;
    Integer availableBeds;
    Integer totalBeds;
    String availabilityStatus;
    Instant lastUpdated;
    String description;
    Double distanceMiles;

    @JsonProperty("bedsAvailable")
    public Integer bedsAvailable() {
        return availableBeds;
    }
}
