package com.project.homeless_shelter_availability_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.homeless_shelter_availability_api.dto.ShelterRequest;
import com.project.homeless_shelter_availability_api.dto.ShelterResponse;
import com.project.homeless_shelter_availability_api.entity.Shelter;
import com.project.homeless_shelter_availability_api.service.HomelessShelterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomelessShelterController {
    private final HomelessShelterService homelessShelterService;

    @GetMapping("")
    public ResponseEntity<List<Shelter>> getAllShelters() {
        Long start = System.currentTimeMillis();

        log.info("Getting all shelthers...");
        List<Shelter> shelters = homelessShelterService.getAllShelters();

        log.info("[{} ms] - Finished retrieving all shelters.", System.currentTimeMillis() - start);
        return new ResponseEntity<>(shelters, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shelter> getShelterById(@PathVariable Integer id) {
        Long start = System.currentTimeMillis();

        log.info("Getting all shelthers...");
        Shelter shelter = homelessShelterService.getShelterById(id);

        log.info("[{} ms] - Finished retrieving all shelters.", System.currentTimeMillis() - start);
        return new ResponseEntity<>(shelter, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<ShelterResponse> createShelter(@RequestBody ShelterRequest shelterRequest) {
        Long start = System.currentTimeMillis();

        log.info("Creating shelter...");
        ShelterResponse shelterResponse = homelessShelterService.createShelter(shelterRequest);

        log.info("[{} ms] - Finished retrieving all shelters.", System.currentTimeMillis() - start);
        return new ResponseEntity<>(shelterResponse, HttpStatus.OK);
    }
}
