package com.project.homeless_shelter_availability_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.homeless_shelter_availability_api.entity.Shelter;

@Repository
public interface HomelessShelterRepository extends JpaRepository<Shelter, Integer>  {
    
}
