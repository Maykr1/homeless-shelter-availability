package com.project.homeless_shelter_availability_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@Entity
@Table(name = "shelters")
@NoArgsConstructor
@AllArgsConstructor
public class Shelter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(name = "zip_code")
    private String zip;

    @Column(name = "phone_number")
    private String phone;

    private String website;

    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(columnDefinition = "text")
    private String hours;

    @Builder.Default
    @Column(nullable = false)
    private String category = "general";

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> services = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> eligibility = new ArrayList<>();

    @Column(name = "available_beds")
    private Integer availableBeds;

    @Column(name = "total_beds")
    private Integer totalBeds;

    @Column(name = "last_source_updated_at")
    private Instant lastSourceUpdatedAt;

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "source_external_id")
    private String sourceExternalId;

    @Column(name = "normalized_name")
    private String normalizedName;

    @Column(name = "normalized_address")
    private String normalizedAddress;
}
