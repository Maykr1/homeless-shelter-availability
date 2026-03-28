package com.project.homeless_shelter_availability_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.homeless_shelter_availability_api.dto.CoordinatesResponse;
import com.project.homeless_shelter_availability_api.dto.ShelterResponse;
import com.project.homeless_shelter_availability_api.service.ShelterService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShelterController.class)
class ShelterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShelterService shelterService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getShelters_returnsFilteredResults() throws Exception {
        ShelterResponse response = ShelterResponse.builder()
                .id(1L)
                .slug("hope-house-indianapolis")
                .name("Hope House Indianapolis")
                .address("123 Main St")
                .city("Indianapolis")
                .state("IN")
                .zip("46204")
                .phone("555-1234")
                .website("https://example.org")
                .coordinates(CoordinatesResponse.builder().lat(39.7684).lng(-86.1581).build())
                .hours("Open 24 hours")
                .category("general")
                .services(List.of("Emergency shelter"))
                .eligibility(List.of("Adults"))
                .availableBeds(8)
                .totalBeds(30)
                .availabilityStatus("available")
                .lastUpdated(Instant.parse("2026-03-28T12:00:00Z"))
                .description("A welcoming shelter")
                .distanceMiles(1.2)
                .build();

        when(shelterService.searchShelters(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/shelters")
                        .queryParam("query", "46204")
                        .queryParam("lat", "39.7684")
                        .queryParam("lng", "-86.1581")
                        .queryParam("radiusMiles", "10")
                        .queryParam("category", "general")
                        .queryParam("bedsAvailableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("hope-house-indianapolis"))
                .andExpect(jsonPath("$[0].bedsAvailable").value(8))
                .andExpect(jsonPath("$[0].availabilityStatus").value("available"))
                .andExpect(jsonPath("$[0].coordinates.lat").value(39.7684));

        verify(shelterService).searchShelters(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getShelterBySlug_returnsShelter() throws Exception {
        ShelterResponse response = ShelterResponse.builder()
                .id(1L)
                .slug("hope-house-indianapolis")
                .name("Hope House Indianapolis")
                .address("123 Main St")
                .city("Indianapolis")
                .state("IN")
                .zip("46204")
                .phone("555-1234")
                .category("general")
                .services(List.of())
                .eligibility(List.of())
                .availabilityStatus("unknown")
                .build();

        when(shelterService.getShelterBySlug("hope-house-indianapolis", 39.7684, -86.1581)).thenReturn(response);

        mockMvc.perform(get("/api/shelters/hope-house-indianapolis")
                        .queryParam("lat", "39.7684")
                        .queryParam("lng", "-86.1581"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("hope-house-indianapolis"))
                .andExpect(jsonPath("$.city").value("Indianapolis"))
                .andExpect(jsonPath("$.availabilityStatus").value("unknown"));
    }

    @Test
    void getShelterBySlug_notFound_returns404() throws Exception {
        when(shelterService.getShelterBySlug("missing", null, null))
                .thenThrow(new EntityNotFoundException("Shelter not found with slug: missing"));

        mockMvc.perform(get("/api/shelters/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Shelter not found"));
    }
}
