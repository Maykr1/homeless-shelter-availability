package com.project.homeless_shelter_availability_api.repository;

import com.project.homeless_shelter_availability_api.model.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    Optional<Shelter> findBySlug(String slug);
}
