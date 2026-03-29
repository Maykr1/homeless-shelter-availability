package com.project.homeless_shelter_availability_api.service;

import com.project.homeless_shelter_availability_api.model.Shelter;
import com.project.homeless_shelter_availability_api.repository.ShelterRepository;
import com.project.homeless_shelter_availability_api.service.impl.ShelterServiceImpl;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShelterServiceTest {

    @Mock
    private ShelterRepository shelterRepository;

    @InjectMocks
    private ShelterServiceImpl shelterServiceImpl;

    private Shelter shelter;

    @BeforeEach
    void setUp() {
        shelter = Shelter.builder()
            .id(1L)
            .name("Hope House")
            .address("123 Main St")
            .city("Springfield")
            .state("IL")
            .zipCode("62701")
            .phoneNumber("555-1234")
            .email("hope@example.com")
            .totalBeds(50)
            .availableBeds(10)
            .description("A welcoming shelter for all.")
            .build();
    }

    @Test
    void getAllShelters_returnsList() {
        when(shelterRepository.findAll()).thenReturn(List.of(shelter));

        List<Shelter> result = shelterServiceImpl.getAllShelters();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Hope House", result.get(0).getName());
        verify(shelterRepository, times(1)).findAll();
    }

    @Test
    void getShelterById_found_returnsShelter() {
        when(shelterRepository.findById(1L)).thenReturn(Optional.of(shelter));

        Shelter result = shelterServiceImpl.getShelterById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Hope House", result.getName());
        verify(shelterRepository, times(1)).findById(1L);
    }

    @Test
    void getShelterById_notFound_throwsException() {
        when(shelterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> shelterServiceImpl.getShelterById(99L));
        verify(shelterRepository, times(1)).findById(99L);
    }

    @Test
    void createShelter_returnsCreated() {
        when(shelterRepository.save(any(Shelter.class))).thenReturn(shelter);

        Shelter result = shelterServiceImpl.createShelter(shelter);

        assertNotNull(result);
        assertEquals("Hope House", result.getName());
        verify(shelterRepository, times(1)).save(shelter);
    }

    @Test
    void updateShelter_found_returnsUpdated() {
        Shelter updatedInfo = Shelter.builder()
                .name("Hope House Updated")
                .availableBeds(5)
                .build();

        when(shelterRepository.findById(1L)).thenReturn(Optional.of(shelter));
        // Returns the same object passed into save()
        when(shelterRepository.save(any(Shelter.class))).thenAnswer(i -> i.getArgument(0));

        Shelter result = shelterServiceImpl.updateShelter(1L, updatedInfo);

        assertNotNull(result);
        assertEquals("Hope House Updated", result.getName());
        assertEquals(5, result.getAvailableBeds());
        
        verify(shelterRepository, times(1)).findById(1L);
        verify(shelterRepository, times(1)).save(any(Shelter.class));
    }

    @Test
    void updateShelter_notFound_throwsException() {
        when(shelterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> shelterServiceImpl.updateShelter(99L, shelter));
        
        verify(shelterRepository, times(1)).findById(99L);
        verify(shelterRepository, never()).save(any(Shelter.class));
    }

    @Test
    void deleteShelter_found_deletes() {
        when(shelterRepository.existsById(1L)).thenReturn(true);
        doNothing().when(shelterRepository).deleteById(1L);

        assertDoesNotThrow(() -> shelterServiceImpl.deleteShelter(1L));

        verify(shelterRepository, times(1)).existsById(1L);
        verify(shelterRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteShelter_notFound_throwsException() {
        when(shelterRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> shelterServiceImpl.deleteShelter(99L));
        
        verify(shelterRepository, times(1)).existsById(99L);
        verify(shelterRepository, never()).deleteById(anyLong());
    }
}
