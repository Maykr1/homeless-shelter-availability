package com.project.homeless_shelter_availability_api.service;

import java.util.List;

import com.project.homeless_shelter_availability_api.dto.ShelterRequest;
import com.project.homeless_shelter_availability_api.dto.ShelterResponse;
import com.project.homeless_shelter_availability_api.entity.Shelter;

public interface HomelessShelterService {
    public List<Shelter> getAllShelters();
    public Shelter getShelterById(Integer id);
    public ShelterResponse createShelter(ShelterRequest shelterRequest);
}
