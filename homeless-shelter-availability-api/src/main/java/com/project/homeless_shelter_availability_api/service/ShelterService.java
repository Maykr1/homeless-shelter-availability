package com.project.homeless_shelter_availability_api.service;

import com.project.homeless_shelter_availability_api.model.Shelter;

import java.util.List;

public interface ShelterService {

    List<Shelter> getAllShelters();

    Shelter getShelterById(Long id);

    Shelter createShelter(Shelter shelter);

    Shelter updateShelter(Long id, Shelter shelter);

    void deleteShelter(Long id);
}
