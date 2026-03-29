package com.project.homeless_shelter_availability_api.controller;

import com.project.homeless_shelter_availability_api.model.Shelter;
import com.project.homeless_shelter_availability_api.repository.ShelterRepository;
import com.project.homeless_shelter_availability_api.support.PostgresContainerSupport;
import com.project.homeless_shelter_availability_api.util.TextNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
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

    @BeforeEach
    void setUp() {
        shelterRepository.deleteAll();

        shelterRepository.saveAll(Objects.requireNonNull(List.of(
                Shelter.builder()
                        .slug("harbor-house-indianapolis")
                        .name("Harbor House Indianapolis")
                        .address("245 Meridian Street")
                        .city("Indianapolis")
                        .state("IN")
                        .zip("46204")
                        .phone("317-555-0001")
                        .website("https://harbor.example.org")
                        .latitude(39.7684)
                        .longitude(-86.1581)
                        .hours("Open 24 hours")
                        .category("general")
                        .services(List.of("Emergency shelter", "Case management"))
                        .eligibility(List.of("Adults"))
                        .availableBeds(18)
                        .totalBeds(60)
                        .lastSourceUpdatedAt(Instant.parse("2026-03-28T13:12:00Z"))
                        .description("Central downtown shelter with overnight beds.")
                        .normalizedName(TextNormalizer.normalize("Harbor House Indianapolis"))
                        .normalizedAddress(TextNormalizer.normalize("245 Meridian Street"))
                        .build(),
                Shelter.builder()
                        .slug("horizon-center-pittsburgh")
                        .name("Horizon Center Pittsburgh")
                        .address("601 Smithfield Street")
                        .city("Pittsburgh")
                        .state("PA")
                        .zip("15222")
                        .phone("412-555-0002")
                        .latitude(40.4406)
                        .longitude(-79.9959)
                        .hours("Open 24 hours")
                        .category("general")
                        .services(List.of("Meals"))
                        .eligibility(List.of("Adults"))
                        .availableBeds(0)
                        .totalBeds(40)
                        .lastSourceUpdatedAt(Instant.parse("2026-03-28T13:45:00Z"))
                        .description("Downtown service hub.")
                        .normalizedName(TextNormalizer.normalize("Horizon Center Pittsburgh"))
                        .normalizedAddress(TextNormalizer.normalize("601 Smithfield Street"))
                        .build(),
                Shelter.builder()
                        .slug("valor-house-columbus")
                        .name("Valor House Columbus")
                        .address("111 Veteran Way")
                        .city("Columbus")
                        .state("OH")
                        .zip("43065")
                        .phone("614-555-0003")
                        .latitude(40.0992)
                        .longitude(-83.1141)
                        .hours("Intake until 10 PM")
                        .category("veteran")
                        .services(List.of())
                        .eligibility(List.of("Veterans"))
                        .availableBeds(null)
                        .totalBeds(25)
                        .lastSourceUpdatedAt(Instant.parse("2026-03-28T14:00:00Z"))
                        .description("Veteran focused shelter.")
                        .normalizedName(TextNormalizer.normalize("Valor House Columbus"))
                        .normalizedAddress(TextNormalizer.normalize("111 Veteran Way"))
                        .build()
        )));
    }

    @Test
    void search_filtersByRadiusAndBedsAvailable() throws Exception {
        mockMvc.perform(get("/api/shelters")
                        .queryParam("lat", "39.7684")
                        .queryParam("lng", "-86.1581")
                        .queryParam("radiusMiles", "10")
                        .queryParam("bedsAvailableOnly", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("harbor-house-indianapolis"))
                .andExpect(jsonPath("$[0].distanceMiles").isNumber())
                .andExpect(jsonPath("$[0].bedsAvailable").value(18))
                .andExpect(jsonPath("$[0].availabilityStatus").value("available"));
    }

    @Test
    void search_includesUnknownAvailabilityWhenNotFiltering() throws Exception {
        mockMvc.perform(get("/api/shelters")
                        .queryParam("query", "43065")
                        .queryParam("category", "veteran")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("valor-house-columbus"))
                .andExpect(jsonPath("$[0].availabilityStatus").value("unknown"))
                .andExpect(jsonPath("$[0].eligibility[0]").value("Veterans"));
    }

    @Test
    void getShelterBySlug_returnsDtoShape() throws Exception {
        mockMvc.perform(get("/api/shelters/harbor-house-indianapolis")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("harbor-house-indianapolis"))
                .andExpect(jsonPath("$.name").value("Harbor House Indianapolis"))
                .andExpect(jsonPath("$.coordinates.lat").value(39.7684))
                .andExpect(jsonPath("$.services[0]").value("Emergency shelter"))
                .andExpect(jsonPath("$.lastUpdated").value("2026-03-28T13:12:00Z"));
    }
}
