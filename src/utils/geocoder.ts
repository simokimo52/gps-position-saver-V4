/**
 * Offline-friendly reverse geocoding using OpenStreetMap Nominatim
 */
export async function reverseGeocode(
  lat: number,
  lng: number
): Promise<string | null> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);

    const response = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`,
      {
        headers: {
          'Accept-Language': 'en',
          'User-Agent': 'GpsPositionSaver/1.0',
        },
        signal: controller.signal,
      }
    );

    clearTimeout(timeoutId);

    if (!response.ok) return null;
    const data = await response.json();

    if (data && data.display_name) {
      // Create a nice human-readable address: road, neighbourhood/suburb, city, state/country
      const a = data.address || {};
      const parts = [
        a.road || a.pedestrian || a.street,
        a.suburb || a.neighbourhood || a.quarter,
        a.city || a.town || a.village || a.county,
        a.country,
      ].filter(Boolean);

      if (parts.length > 0) {
        return parts.join(', ');
      }
      return data.display_name;
    }
    return null;
  } catch (error) {
    // Return null silently for offline mode or network errors
    return null;
  }
}
