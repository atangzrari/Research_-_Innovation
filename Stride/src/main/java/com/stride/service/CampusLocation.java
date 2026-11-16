package com.stride.service;

/** The optional location factor (RQ17): is a reported position within the campus radius? */
public final class CampusLocation {

    private static final double EARTH_RADIUS_M = 6_371_000;

    private final boolean enabled;
    private final double latitude;
    private final double longitude;
    private final double radiusMetres;

    public CampusLocation(Double latitude, Double longitude, double radiusMetres) {
        this.enabled = latitude != null && longitude != null && radiusMetres > 0;
        this.latitude = latitude == null ? 0 : latitude;
        this.longitude = longitude == null ? 0 : longitude;
        this.radiusMetres = radiusMetres;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Great-circle distance in metres (haversine formula). */
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a));
    }

    /** Classifies a reported position; anything missing or unreadable counts as "not used". */
    public ConfidenceScorer.Location classify(String lat, String lon, String accuracy) {
        if (!enabled || lat == null || lon == null || lat.isBlank() || lon.isBlank()) {
            return ConfidenceScorer.Location.NOT_USED;
        }
        try {
            double la = Double.parseDouble(lat);
            double lo = Double.parseDouble(lon);
            double acc = accuracy == null || accuracy.isBlank() ? 0 : Double.parseDouble(accuracy);
            if (Math.abs(la) > 90 || Math.abs(lo) > 180 || acc > 2000) {
                return ConfidenceScorer.Location.NOT_USED;
            }
            return distance(latitude, longitude, la, lo) <= radiusMetres + acc
                    ? ConfidenceScorer.Location.INSIDE : ConfidenceScorer.Location.OUTSIDE;
        } catch (NumberFormatException e) {
            return ConfidenceScorer.Location.NOT_USED;
        }
    }
}
