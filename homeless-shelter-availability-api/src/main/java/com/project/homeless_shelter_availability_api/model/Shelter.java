package com.project.homeless_shelter_availability_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder @Data
@Entity
@Table(
        name = "shelters",
        indexes = {
                @Index(name = "idx_shelters_state", columnList = "state"),
                @Index(name = "idx_shelters_city_state", columnList = "city,state"),
                @Index(name = "idx_shelters_zip_code", columnList = "zip_code"),
                @Index(name = "idx_shelters_available_beds", columnList = "available_beds")
        }
)
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

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String email;

    @Column(name = "total_beds", nullable = false)
    private Integer totalBeds;

    @Column(name = "available_beds", nullable = false)
    private Integer availableBeds;

    private String description;
}
