package com.project.homeless_shelter_availability_api.service.impl;

import com.project.homeless_shelter_availability_api.dto.ShelterQuery;
import com.project.homeless_shelter_availability_api.dto.ShelterResponse;
import com.project.homeless_shelter_availability_api.mapper.ShelterMapper;
import com.project.homeless_shelter_availability_api.model.Shelter;
import com.project.homeless_shelter_availability_api.repository.ShelterRepository;
import com.project.homeless_shelter_availability_api.service.ShelterService;
import com.project.homeless_shelter_availability_api.util.GeoUtils;
import com.project.homeless_shelter_availability_api.util.TextNormalizer;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class ShelterServiceImpl implements ShelterService {

    private final ShelterRepository shelterRepository;
    private final ShelterMapper shelterMapper;

    public ShelterServiceImpl(ShelterRepository shelterRepository, ShelterMapper shelterMapper) {
        this.shelterRepository = shelterRepository;
        this.shelterMapper = shelterMapper;
    }

    @Override
    public List<ShelterResponse> searchShelters(ShelterQuery query) {
        String normalizedQuery = TextNormalizer.normalize(query.query());
        Set<String> categories = normalizeCategories(query.category());
        boolean hasOrigin = query.lat() != null && query.lng() != null;

        return shelterRepository.findAll().stream()
                .map(shelter -> new ShelterDistance(shelter, hasOrigin
                        ? GeoUtils.distanceMiles(query.lat(), query.lng(), shelter.getLatitude(), shelter.getLongitude())
                        : null))
                .filter(candidate -> matchesQuery(candidate.shelter(), normalizedQuery))
                .filter(candidate -> matchesCategories(candidate.shelter(), categories))
                .filter(candidate -> !query.bedsAvailableOnly()
                        || candidate.shelter().getAvailableBeds() != null && candidate.shelter().getAvailableBeds() > 0)
                .filter(candidate -> matchesRadius(candidate.distanceMiles(), query.radiusMiles()))
                .sorted(buildComparator(hasOrigin))
                .map(candidate -> shelterMapper.toResponse(candidate.shelter(), candidate.distanceMiles()))
                .toList();
    }

    @Override
    public ShelterResponse getShelterBySlug(String slug, Double lat, Double lng) {
        Shelter shelter = shelterRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Shelter not found with slug: " + slug));
        Double distanceMiles = lat != null && lng != null
                ? GeoUtils.distanceMiles(lat, lng, shelter.getLatitude(), shelter.getLongitude())
                : null;
        return shelterMapper.toResponse(shelter, distanceMiles);
    }

    private boolean matchesQuery(Shelter shelter, String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return true;
        }
        return Stream.of(
                        shelter.getName(),
                        shelter.getAddress(),
                        shelter.getCity(),
                        shelter.getState(),
                        shelter.getZip(),
                        shelter.getDescription()
                )
                .map(TextNormalizer::normalize)
                .anyMatch(value -> value.contains(normalizedQuery));
    }

    private boolean matchesCategories(Shelter shelter, Set<String> categories) {
        if (categories.isEmpty()) {
            return true;
        }
        return categories.contains(TextNormalizer.normalize(shelter.getCategory()));
    }

    private boolean matchesRadius(Double distanceMiles, Double radiusMiles) {
        if (radiusMiles == null) {
            return true;
        }
        return distanceMiles == null || distanceMiles <= radiusMiles;
    }

    private Set<String> normalizeCategories(List<String> category) {
        if (category == null) {
            return Set.of();
        }
        return category.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(TextNormalizer::normalize)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private Comparator<ShelterDistance> buildComparator(boolean hasOrigin) {
        Comparator<ShelterDistance> comparator = Comparator
                .comparingInt((ShelterDistance candidate) -> availabilityRank(candidate.shelter()))
                .thenComparing(candidate -> candidate.shelter().getName(), String.CASE_INSENSITIVE_ORDER);

        if (!hasOrigin) {
            return comparator;
        }

        return Comparator
                .comparing((ShelterDistance candidate) -> candidate.distanceMiles() == null)
                .thenComparing(candidate -> candidate.distanceMiles() == null ? Double.MAX_VALUE : candidate.distanceMiles())
                .thenComparing(comparator);
    }

    private int availabilityRank(Shelter shelter) {
        return switch (shelterMapper.resolveAvailabilityStatus(shelter.getAvailableBeds())) {
            case "available" -> 0;
            case "limited" -> 1;
            case "unknown" -> 2;
            default -> 3;
        };
    }

    private record ShelterDistance(Shelter shelter, Double distanceMiles) {
    }
}
