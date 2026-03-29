package com.project.homeless_shelter_availability_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.homeless_shelter_availability_api.model.Shelter;
import com.project.homeless_shelter_availability_api.service.ShelterService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Objects;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(ShelterController.class)
class ShelterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShelterService shelterService;

    @Autowired
    private ObjectMapper objectMapper;

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

    private static <T> T nonNull(T value) {
        return Objects.requireNonNull(value);
    }

    private static MediaType jsonMediaType() {
        return nonNull(MediaType.APPLICATION_JSON);
    }

    private String writeJson(Object value) throws Exception {
        return nonNull(objectMapper.writeValueAsString(value));
    }

    @Test
    void getAllShelters_returnsList() throws Exception {
        when(shelterService.getAllShelters()).thenReturn(List.of(nonNull(shelter)));

        mockMvc.perform(get("/api/shelters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Hope House"))
                .andExpect(jsonPath("$[0].city").value("Springfield"));
    }

    @Test
    void getAllShelters_returnsEmptyList() throws Exception {
        when(shelterService.getAllShelters()).thenReturn(List.of());

        mockMvc.perform(get("/api/shelters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getShelterById_found_returnsShelter() throws Exception {
        when(shelterService.getShelterById(nonNull(1L))).thenReturn(nonNull(shelter));

        mockMvc.perform(get("/api/shelters/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Hope House"))
                .andExpect(jsonPath("$.availableBeds").value(10));
    }

    @Test
    void getShelterById_notFound_returns404() throws Exception {
        when(shelterService.getShelterById(nonNull(99L))).thenThrow(new EntityNotFoundException("Shelter not found with id: 99"));

        mockMvc.perform(get("/api/shelters/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createShelter_returnsCreated() throws Exception {
        Shelter input = Shelter.builder()
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

        when(shelterService.createShelter(nonNull(input))).thenReturn(nonNull(shelter));

        mockMvc.perform(post("/api/shelters")
                        .contentType(jsonMediaType())
                        .content(writeJson(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Hope House"));
    }

    @Test
    void updateShelter_found_returnsUpdated() throws Exception {
        Shelter updated = Shelter.builder()
                .id(1L)
                .name("Hope House Updated")
                .address("123 Main St")
                .city("Springfield")
                .state("IL")
                .zipCode("62701")
                .phoneNumber("555-9999")
                .email("updated@example.com")
                .totalBeds(60)
                .availableBeds(5)
                .description("Updated description.")
                .build();

        when(shelterService.updateShelter(nonNull(1L), nonNull(updated))).thenReturn(nonNull(updated));

        mockMvc.perform(put("/api/shelters/1")
                        .contentType(jsonMediaType())
                        .content(writeJson(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hope House Updated"))
                .andExpect(jsonPath("$.availableBeds").value(5));
    }

    @Test
    void updateShelter_notFound_returns404() throws Exception {
        when(shelterService.updateShelter(nonNull(99L), nonNull(shelter)))
                .thenThrow(new EntityNotFoundException("Shelter not found with id: 99"));

        mockMvc.perform(put("/api/shelters/99")
                        .contentType(jsonMediaType())
                        .content(writeJson(shelter)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShelter_found_returnsNoContent() throws Exception {
        doNothing().when(shelterService).deleteShelter(nonNull(1L));

        mockMvc.perform(delete("/api/shelters/1"))
                .andExpect(status().isNoContent());

        verify(shelterService, times(1)).deleteShelter(nonNull(1L));
    }

    @Test
    void deleteShelter_notFound_returns404() throws Exception {
        doThrow(new EntityNotFoundException("Shelter not found with id: 99"))
                .when(shelterService).deleteShelter(nonNull(99L));

        mockMvc.perform(delete("/api/shelters/99"))
                .andExpect(status().isNotFound());
    }
}
