import { CoordinateFormat, Place, UnitSystem } from '../types';

export function toDms(coordinate: number, isLatitude: boolean): string {
  const absolute = Math.abs(coordinate);
  const degrees = Math.floor(absolute);
  const minutesDouble = (absolute - degrees) * 60.0;
  const minutes = Math.floor(minutesDouble);
  const seconds = (minutesDouble - minutes) * 60.0;
  const direction = isLatitude
    ? coordinate >= 0 ? 'N' : 'S'
    : coordinate >= 0 ? 'E' : 'W';

  return `${degrees}° ${String(minutes).padStart(2, '0')}' ${seconds.toFixed(1).padStart(4, '0')}" ${direction}`;
}

export function toDdm(coordinate: number, isLatitude: boolean): string {
  const absolute = Math.abs(coordinate);
  const degrees = Math.floor(absolute);
  const minutes = (absolute - degrees) * 60.0;
  const direction = isLatitude
    ? coordinate >= 0 ? 'N' : 'S'
    : coordinate >= 0 ? 'E' : 'W';

  return `${degrees}° ${minutes.toFixed(3).padStart(6, '0')}' ${direction}`;
}

export function toUtm(lat: number, lng: number): string {
  const zone = Math.min(60, Math.max(1, Math.floor((lng + 180.0) / 6.0) + 1));
  const hemisphere = lat >= 0 ? 'N' : 'S';
  const centralMeridian = -183.0 + zone * 6.0;
  const deltaLng = ((lng - centralMeridian) * Math.PI) / 180.0;
  const latRad = (lat * Math.PI) / 180.0;
  const falseEasting = 500000.0;
  const easting = Math.round(falseEasting + deltaLng * Math.cos(latRad) * 6378137.0);
  const falseNorthing = lat < 0 ? 10000000.0 : 0.0;
  const northing = Math.round(falseNorthing + latRad * 6378137.0);

  return `Zone ${zone}${hemisphere} ${easting}m E ${northing}m N`;
}

export function formatCoordinates(
  lat: number,
  lng: number,
  format: CoordinateFormat = 'DECIMAL_DEGREES'
): string {
  switch (format) {
    case 'DECIMAL_DEGREES': {
      const latDir = lat >= 0 ? 'N' : 'S';
      const lngDir = lng >= 0 ? 'E' : 'W';
      return `${Math.abs(lat).toFixed(6)}° ${latDir}, ${Math.abs(lng).toFixed(6)}° ${lngDir}`;
    }
    case 'DEGREES_MINUTES_SECONDS':
      return `${toDms(lat, true)}, ${toDms(lng, false)}`;
    case 'DEGREES_DECIMAL_MINUTES':
      return `${toDdm(lat, true)}, ${toDdm(lng, false)}`;
    case 'UTM':
      return toUtm(lat, lng);
    default:
      return `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
  }
}

export function formatAccuracy(
  accuracyMeters?: number | null,
  unitSystem: UnitSystem = 'METRIC'
): string {
  if (accuracyMeters == null || isNaN(accuracyMeters)) return '± unknown';
  if (unitSystem === 'METRIC') {
    return `±${accuracyMeters.toFixed(1)} m`;
  } else {
    const feet = accuracyMeters * 3.28084;
    return `±${feet.toFixed(1)} ft`;
  }
}

export function formatDistance(
  meters: number,
  unitSystem: UnitSystem = 'METRIC'
): string {
  if (unitSystem === 'METRIC') {
    if (meters < 1000) {
      return `${Math.round(meters)} m`;
    }
    return `${(meters / 1000).toFixed(1)} km`;
  } else {
    const feet = meters * 3.28084;
    if (feet < 5280) {
      return `${Math.round(feet)} ft`;
    }
    return `${(feet / 5280).toFixed(1)} mi`;
  }
}

/**
 * Haversine formula to compute great-circle distance in meters between two lat/lon coordinates.
 */
export function calculateDistance(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371e3; // Earth radius in metres
  const φ1 = (lat1 * Math.PI) / 180;
  const φ2 = (lat2 * Math.PI) / 180;
  const Δφ = ((lat2 - lat1) * Math.PI) / 180;
  const Δλ = ((lon2 - lon1) * Math.PI) / 180;

  const a =
    Math.sin(Δφ / 2) * Math.sin(Δφ / 2) +
    Math.cos(φ1) * Math.cos(φ2) * Math.sin(Δλ / 2) * Math.sin(Δλ / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

  return R * c; // in metres
}

/**
 * Calculates compass bearing (azimuth) in degrees (0-360°) from point 1 to point 2.
 */
export function calculateBearing(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const φ1 = (lat1 * Math.PI) / 180;
  const φ2 = (lat2 * Math.PI) / 180;
  const Δλ = ((lon2 - lon1) * Math.PI) / 180;

  const y = Math.sin(Δλ) * Math.cos(φ2);
  const x =
    Math.cos(φ1) * Math.sin(φ2) -
    Math.sin(φ1) * Math.cos(φ2) * Math.cos(Δλ);
  const θ = Math.atan2(y, x);
  return (θ * (180 / Math.PI) + 360) % 360;
}

/**
 * Converts degrees azimuth to 8-point compass cardinal direction (N, NE, E, SE, S, SW, W, NW).
 */
export function bearingToCardinal(bearing: number): string {
  const cardinals = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
  const index = Math.round(bearing / 45) % 8;
  return cardinals[index];
}

/**
 * Categorizes distance into intuitive proximity tiers.
 */
export function getProximityTier(meters: number): {
  label: string;
  colorClass: string;
} {
  if (meters < 50) {
    return { label: 'Right Here', colorClass: 'bg-emerald-100 text-emerald-800 border-emerald-200' };
  } else if (meters < 500) {
    return { label: 'Very Close', colorClass: 'bg-teal-100 text-teal-800 border-teal-200' };
  } else if (meters < 2000) {
    return { label: 'Walking Distance', colorClass: 'bg-blue-100 text-blue-800 border-blue-200' };
  } else if (meters < 10000) {
    return { label: 'Nearby', colorClass: 'bg-indigo-100 text-indigo-800 border-indigo-200' };
  } else {
    return { label: 'Far', colorClass: 'bg-slate-100 text-slate-700 border-slate-200' };
  }
}

export function getGoogleMapsUrl(lat: number, lng: number): string {
  return `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`;
}

export function getOpenStreetMapUrl(lat: number, lng: number): string {
  return `https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}#map=17/${lat}/${lng}`;
}

export function createShareText(
  place: Place,
  coordFormat: CoordinateFormat = 'DECIMAL_DEGREES'
): string {
  const parts: string[] = [];
  parts.push(`📍 ${place.name}`);
  if (place.address && place.address.trim()) {
    parts.push(`🏠 ${place.address}`);
  }
  parts.push(`🌐 Coordinates: ${formatCoordinates(place.latitude, place.longitude, coordFormat)}`);
  if (place.accuracy != null) {
    parts.push(`🎯 Accuracy: ${formatAccuracy(place.accuracy)}`);
  }
  if (place.phone && place.phone.trim()) {
    parts.push(`📞 Phone: ${place.phone}`);
  }
  if (place.note && place.note.trim()) {
    parts.push(`📝 Note: ${place.note}`);
  }
  parts.push(`🔗 Open in Maps: ${getGoogleMapsUrl(place.latitude, place.longitude)}`);
  return parts.join('\n');
}
