package com.project.homeless_shelter_availability_api.service.impl;

import com.project.homeless_shelter_availability_api.model.Shelter;
import com.project.homeless_shelter_availability_api.repository.ShelterRepository;
import com.project.homeless_shelter_availability_api.service.ShelterService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShelterServiceImpl implements ShelterService {

    private final ShelterRepository shelterRepository;

    @Override
    public List<Shelter> getAllShelters() {
        return shelterRepository.findAll();
    }

    @Override
    public Shelter getShelterById(Long id) {
        return shelterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Shelter not found with id: " + id));
    }

    @Override
    public Shelter createShelter(Shelter shelter) {
        return shelterRepository.save(shelter);
    }

    @Override
    public Shelter updateShelter(Long id, Shelter shelter) {
        Shelter existing = getShelterById(id);
        existing.setName(shelter.getName());
        existing.setAddress(shelter.getAddress());
        existing.setCity(shelter.getCity());
        existing.setState(shelter.getState());
        existing.setZipCode(shelter.getZipCode());
        existing.setPhoneNumber(shelter.getPhoneNumber());
        existing.setEmail(shelter.getEmail());
        existing.setTotalBeds(shelter.getTotalBeds());
        existing.setAvailableBeds(shelter.getAvailableBeds());
        existing.setDescription(shelter.getDescription());
        return shelterRepository.save(existing);
    }

    @Override
    public void deleteShelter(Long id) {
        if (!shelterRepository.existsById(id)) {
            throw new EntityNotFoundException("Shelter not found with id: " + id);
        }
        shelterRepository.deleteById(id);
    }
}
