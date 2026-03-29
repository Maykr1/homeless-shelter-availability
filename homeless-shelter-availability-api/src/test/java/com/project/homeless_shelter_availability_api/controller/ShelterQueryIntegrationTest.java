package com.project.homeless_shelter_availability_api.controller;

import com.project.homeless_shelter_availability_api.model.Shelter;
import com.project.homeless_shelter_availability_api.repository.ShelterRepository;
import com.project.homeless_shelter_availability_api.support.PostgresContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShelterQueryIntegrationTest extends PostgresContainerSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShelterRepository shelterRepository;

    private Shelter shelter;

    @BeforeEach
    void setUp() {
        shelterRepository.deleteAll();
        Shelter input = Objects.requireNonNull(Shelter.builder()
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
                .build());
        shelter = shelterRepository.save(input);
    }

    @Test
    void getAllShelters_readsPersistedShelters() throws Exception {
        mockMvc.perform(get("/api/shelters")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(shelter.getId()))
                .andExpect(jsonPath("$[0].name").value("Hope House"))
                .andExpect(jsonPath("$[0].zipCode").value("62701"));
    }

    @Test
    void getShelterById_readsPersistedShelter() throws Exception {
        mockMvc.perform(get("/api/shelters/{id}", shelter.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shelter.getId()))
                .andExpect(jsonPath("$.name").value("Hope House"))
                .andExpect(jsonPath("$.availableBeds").value(10));
    }

    @Test
    void getShelterById_returnsNotFoundWhenMissing() throws Exception {
        mockMvc.perform(get("/api/shelters/{id}", shelter.getId() + 1000)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
