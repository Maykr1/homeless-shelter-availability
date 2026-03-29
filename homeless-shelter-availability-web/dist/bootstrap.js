const bundlePath = "/assets/index-J7a6VFdS.js";
const defaultRadiusMiles = "10";
const geolocationOptions = {
  enableHighAccuracy: true,
  timeout: 5000,
  maximumAge: 300000,
};

function hasSearchLocation(searchParams) {
  return Boolean(
    searchParams.get("lat") ||
      searchParams.get("lng") ||
      searchParams.get("query") ||
      searchParams.get("state"),
  );
}

function shouldSeedCurrentLocation(currentUrl) {
  return currentUrl.pathname === "/find-help" && !hasSearchLocation(currentUrl.searchParams);
}

function shouldUseCurrentLocationForLink(targetUrl) {
  return targetUrl.origin === window.location.origin && shouldSeedCurrentLocation(targetUrl);
}

function getCurrentLocation() {
  return new Promise((resolve) => {
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      resolve(null);
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const latitude = Number(position?.coords?.latitude);
        const longitude = Number(position?.coords?.longitude);

        resolve(
          Number.isFinite(latitude) && Number.isFinite(longitude)
            ? { latitude, longitude }
            : null,
        );
      },
      () => resolve(null),
      geolocationOptions,
    );
  });
}

async function bootstrap() {
  const currentUrl = new URL(window.location.href);

  document.addEventListener(
    "click",
    async (event) => {
      if (
        event.defaultPrevented ||
        event.button !== 0 ||
        event.metaKey ||
        event.ctrlKey ||
        event.shiftKey ||
        event.altKey
      ) {
        return;
      }

      const target = event.target;
      const anchor =
        target instanceof Element ? target.closest('a[href]') : null;

      if (!anchor) {
        return;
      }

      const targetUrl = new URL(anchor.href, window.location.href);
      if (!shouldUseCurrentLocationForLink(targetUrl)) {
        return;
      }

      event.preventDefault();
      const currentLocation = await getCurrentLocation();

      if (currentLocation) {
        targetUrl.searchParams.set("lat", String(currentLocation.latitude));
        targetUrl.searchParams.set("lng", String(currentLocation.longitude));

        if (!targetUrl.searchParams.get("radius")) {
          targetUrl.searchParams.set("radius", defaultRadiusMiles);
        }
      }

      window.location.assign(targetUrl.toString());
    },
    true,
  );

  if (shouldSeedCurrentLocation(currentUrl)) {
    const currentLocation = await getCurrentLocation();

    if (currentLocation) {
      currentUrl.searchParams.set("lat", String(currentLocation.latitude));
      currentUrl.searchParams.set("lng", String(currentLocation.longitude));

      if (!currentUrl.searchParams.get("radius")) {
        currentUrl.searchParams.set("radius", defaultRadiusMiles);
      }

      window.history.replaceState(window.history.state, "", currentUrl);
    }
  }

  await import(bundlePath);
}

bootstrap().catch((error) => {
  console.error("Failed to bootstrap the frontend.", error);
});
