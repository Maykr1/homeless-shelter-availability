package com.project.homeless_shelter_availability_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "SHELTER")
@Getter @Setter @Builder // TODO: Remove @Builder, it is dangerous
@AllArgsConstructor
@NoArgsConstructor
public class Shelter {
    @Id @GeneratedValue
    private Integer id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "ADDRESS")
    private String address;
    
    @Column(name = "STATE")
    private String state;

    @Column(name = "CITY")
    private String city;

    @Column(name = "ZIPCODE")
    private String zipCode;
    
    @Column(name = "TOTAL_BEDS")
    private String total_beds;

    @Column(name = "TOTAL_HOUSING")
    private String total_housing;
}
