package com.project.homeless_shelter_availability_api.dto;

import lombok.Builder;
import lombok.Value;

@Value @Builder
public class ShelterResponse {
    private String name;
    private String message;
}
