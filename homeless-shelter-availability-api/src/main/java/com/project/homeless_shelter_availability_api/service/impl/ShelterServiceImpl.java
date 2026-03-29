package com.project.homeless_shelter_availability_api.service.impl;

import com.project.homeless_shelter_availability_api.model.Shelter;
import com.project.homeless_shelter_availability_api.repository.ShelterRepository;
import com.project.homeless_shelter_availability_api.service.ShelterService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ShelterServiceImpl implements ShelterService {

    private final ShelterRepository shelterRepository;

    private static @NonNull List<Shelter> nonNullShelters(List<Shelter> shelters) {
        return Objects.requireNonNull(shelters, "repository returned null shelter list");
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "shelterList", key = "'all'")
    public @NonNull List<Shelter> getAllShelters() {
        return nonNullShelters(shelterRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "shelterListByState", key = "#state.trim().toUpperCase()")
    public @NonNull List<Shelter> getSheltersByState(@NonNull String state) {
        String normalizedState = Objects.requireNonNull(state, "state must not be null").trim();
        return nonNullShelters(shelterRepository.findAllByStateIgnoreCaseOrderByCityAscNameAsc(normalizedState));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "shelterById", key = "#id")
    public @NonNull Shelter getShelterById(@NonNull Long id) {
        Long shelterId = Objects.requireNonNull(id, "id must not be null");
        Shelter shelter = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new EntityNotFoundException("Shelter not found with id: " + shelterId));
        return Objects.requireNonNull(shelter, "repository returned null shelter");
    }

    @Override
    @CacheEvict(value = {"shelterList", "shelterListByState", "shelterById"}, allEntries = true)
    public @NonNull Shelter createShelter(@NonNull Shelter shelter) {
        Shelter nonNullShelter = Objects.requireNonNull(shelter, "shelter must not be null");
        return shelterRepository.save(nonNullShelter);
    }

    @Override
    @CacheEvict(value = {"shelterList", "shelterListByState", "shelterById"}, allEntries = true)
    public @NonNull Shelter updateShelter(@NonNull Long id, @NonNull Shelter shelter) {
        Shelter nonNullShelter = Objects.requireNonNull(shelter, "shelter must not be null");
        Shelter existing = getShelterById(id);
        existing.setName(nonNullShelter.getName());
        existing.setAddress(nonNullShelter.getAddress());
        existing.setCity(nonNullShelter.getCity());
        existing.setState(nonNullShelter.getState());
        existing.setZipCode(nonNullShelter.getZipCode());
        existing.setPhoneNumber(nonNullShelter.getPhoneNumber());
        existing.setEmail(nonNullShelter.getEmail());
        existing.setTotalBeds(nonNullShelter.getTotalBeds());
        existing.setAvailableBeds(nonNullShelter.getAvailableBeds());
        existing.setDescription(nonNullShelter.getDescription());
        return shelterRepository.save(existing);
    }

    @Override
    @CacheEvict(value = {"shelterList", "shelterListByState", "shelterById"}, allEntries = true)
    public void deleteShelter(@NonNull Long id) {
        Long shelterId = Objects.requireNonNull(id, "id must not be null");
        if (!shelterRepository.existsById(shelterId)) {
            throw new EntityNotFoundException("Shelter not found with id: " + shelterId);
        }
        shelterRepository.deleteById(shelterId);
    }
}
