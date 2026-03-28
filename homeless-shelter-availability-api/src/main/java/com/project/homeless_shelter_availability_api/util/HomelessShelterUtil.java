package com.project.homeless_shelter_availability_api.util;

import com.project.homeless_shelter_availability_api.dto.ShelterRequest;
import com.project.homeless_shelter_availability_api.entity.Shelter;

public class HomelessShelterUtil {

    //TODO: Change this to a wrapper
    public static Shelter toEntity(ShelterRequest shelterRequest) {
        return Shelter.builder()
            .name(shelterRequest.getName())
            .address(shelterRequest.getAddress())
            .state(shelterRequest.getState())
            .city(shelterRequest.getCity())
            .zipCode(shelterRequest.getZipCode())
            .total_beds(shelterRequest.getTotal_beds())
            .total_housing(shelterRequest.getTotal_housing())
            .build();
    }
}
