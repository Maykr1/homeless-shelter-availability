package com.project.homeless_shelter_availability_api.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.project.homeless_shelter_availability_api.dto.ShelterRequest;
import com.project.homeless_shelter_availability_api.dto.ShelterResponse;
import com.project.homeless_shelter_availability_api.entity.Shelter;
import com.project.homeless_shelter_availability_api.repository.HomelessShelterRepository;
import com.project.homeless_shelter_availability_api.service.HomelessShelterService;
import com.project.homeless_shelter_availability_api.util.HomelessShelterUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Service
@RequiredArgsConstructor
public class HomelessShelterServiceImpl implements HomelessShelterService {
    private final HomelessShelterRepository homelessShelterRepository;

    @Override
    public List<Shelter> getAllShelters() {
        return homelessShelterRepository.findAll();
    }

    @Override
    public Shelter getShelterById(Integer id) {
        Optional<Shelter> optionalShelter =  homelessShelterRepository.findById(id);

        if (!optionalShelter.isPresent()) {
            log.warn("No shelter found at id: {}", id);
        }

        Shelter shelter = optionalShelter.get();

        log.info("Found shelter at id: {} name: {}", id, shelter.getName());
        return shelter;
    }

    @Override
    public ShelterResponse createShelter(ShelterRequest shelterRequest) {
        Shelter shelter = HomelessShelterUtil.toEntity(shelterRequest);
        
        homelessShelterRepository.save(shelter);

        ShelterResponse shelterResponse = ShelterResponse.builder()
            .name(shelter.getName())
            .message("Successfully created Shelter")
            .build();

        return shelterResponse;
    }
}
