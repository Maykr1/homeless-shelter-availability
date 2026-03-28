package com.project.homeless_shelter_availability_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder @Data
@Entity @Table(name = "shelters") 
@NoArgsConstructor
@AllArgsConstructor
public class Shelter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String zipCode;

    private String phoneNumber;

    private String email;

    @Column(nullable = false)
    private Integer totalBeds;

    @Column(nullable = false)
    private Integer availableBeds;

    private String description;
}
