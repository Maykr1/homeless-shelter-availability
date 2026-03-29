package com.project.homeless_shelter_availability_api.service;

import com.project.homeless_shelter_availability_api.model.Shelter;
import org.springframework.lang.NonNull;

import java.util.List;

public interface ShelterService {

    @NonNull List<Shelter> getAllShelters();

    @NonNull List<Shelter> getSheltersByState(@NonNull String state);

    @NonNull Shelter getShelterById(@NonNull Long id);

    @NonNull Shelter createShelter(@NonNull Shelter shelter);

    @NonNull Shelter updateShelter(@NonNull Long id, @NonNull Shelter shelter);

    void deleteShelter(@NonNull Long id);
}
