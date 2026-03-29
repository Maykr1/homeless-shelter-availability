package com.project.homeless_shelter_availability_api.repository;

import com.project.homeless_shelter_availability_api.model.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    List<Shelter> findAllByStateIgnoreCaseOrderByCityAscNameAsc(String state);
}
