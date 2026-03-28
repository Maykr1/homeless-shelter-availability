package com.project.homeless_shelter_availability_api.util;

public final class GeoUtils {

    private static final double EARTH_RADIUS_MILES = 3958.7613d;

    private GeoUtils() {
    }

    public static Double distanceMiles(Double originLat, Double originLng, Double targetLat, Double targetLng) {
        if (originLat == null || originLng == null || targetLat == null || targetLng == null) {
            return null;
        }

        double latDistance = Math.toRadians(targetLat - originLat);
        double lngDistance = Math.toRadians(targetLng - originLng);
        double a = Math.pow(Math.sin(latDistance / 2), 2)
                + Math.cos(Math.toRadians(originLat))
                * Math.cos(Math.toRadians(targetLat))
                * Math.pow(Math.sin(lngDistance / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_MILES * c;
    }
}
