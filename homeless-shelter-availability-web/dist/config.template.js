window.__APP_CONFIG__ = {
  GOOGLE_MAPS_API_KEY: "${GOOGLE_MAPS_API_KEY}",
  API_BASE_URL: "${API_BASE_URL}",
};

try {
  const shelterDatasetVersionKey = "shelter-dataset-version";
  const shelterDatasetVersion = "shelters-14000-v4";

  if (window.localStorage.getItem(shelterDatasetVersionKey) !== shelterDatasetVersion) {
    window.localStorage.removeItem("shelter-list-cache-v1");
    window.localStorage.removeItem("city-coordinate-cache-v1");
    window.localStorage.setItem(shelterDatasetVersionKey, shelterDatasetVersion);
  }
} catch (error) {
  // Ignore storage access failures in hardened/private browser contexts.
}



