package com.project.homeless_shelter_availability_api.service;

import com.project.homeless_shelter_availability_api.dto.ShelterQuery;
import com.project.homeless_shelter_availability_api.dto.ShelterResponse;

import java.util.List;

public interface ShelterService {

    List<ShelterResponse> searchShelters(ShelterQuery query);

    ShelterResponse getShelterBySlug(String slug, Double lat, Double lng);
}
