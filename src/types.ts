export interface Place {
  id: number;
  name: string;
  latitude: number;
  longitude: number;
  accuracy?: number | null;
  altitude?: number | null;
  address?: string | null;
  note?: string | null;
  phone?: string | null;
  photoUri?: string | null;
  groupId?: number | null;
  isFavorite: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface Group {
  id: number;
  name: string;
  colorHex: string;
  iconName: string;
  createdAt: number;
}

export interface ShareSession {
  sessionId: string;
  startTime: number;
  durationMinutes: number;
  expiresAt: number;
  isActive: boolean;
  shareUrl: string;
  lastLatitude?: number | null;
  lastLongitude?: number | null;
  lastAccuracy?: number | null;
  lastUpdated?: number | null;
}

export type CoordinateFormat =
  | 'DECIMAL_DEGREES'
  | 'DEGREES_MINUTES_SECONDS'
  | 'DEGREES_DECIMAL_MINUTES'
  | 'UTM';

export interface CoordinateFormatOption {
  key: CoordinateFormat;
  displayName: string;
  example: string;
}

export type UnitSystem = 'METRIC' | 'IMPERIAL';

export interface UnitSystemOption {
  key: UnitSystem;
  displayName: string;
  caption: string;
}

export interface AppSettings {
  coordinateFormat: CoordinateFormat;
  unitSystem: UnitSystem;
  defaultGroupId: number | null;
}

export type ScreenTab = 'home' | 'places' | 'map' | 'live' | 'settings';
