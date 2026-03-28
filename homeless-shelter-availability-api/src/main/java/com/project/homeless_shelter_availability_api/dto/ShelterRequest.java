package com.project.homeless_shelter_availability_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@AllArgsConstructor 
@NoArgsConstructor
public class ShelterRequest {
    private String name;
    private String address;
    private String state;
    private String city;
    private String zipCode;
    private String total_beds;
    private String total_housing;
}
