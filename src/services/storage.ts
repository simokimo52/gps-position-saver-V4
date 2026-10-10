import { AppSettings, Group, Place, ShareSession } from '../types';

const STORAGE_KEYS = {
  PLACES: 'gps_saver_places',
  GROUPS: 'gps_saver_groups',
  SETTINGS: 'gps_saver_settings',
  SESSIONS: 'gps_saver_sessions',
};

const DEFAULT_GROUPS: Group[] = [
  { id: 1, name: 'Home', colorHex: '#4CAF50', iconName: 'home', createdAt: Date.now() - 86400000 * 5 },
  { id: 2, name: 'Work', colorHex: '#1E88E5', iconName: 'briefcase', createdAt: Date.now() - 86400000 * 4 },
  { id: 3, name: 'Outdoor', colorHex: '#FB8C00', iconName: 'mountain', createdAt: Date.now() - 86400000 * 3 },
  { id: 4, name: 'Parking', colorHex: '#8E24AA', iconName: 'car', createdAt: Date.now() - 86400000 * 2 },
  { id: 5, name: 'Travel', colorHex: '#00ACC1', iconName: 'compass', createdAt: Date.now() - 86400000 },
];

const INITIAL_PLACES: Place[] = [
  {
    id: 1,
    name: 'Eiffel Tower Viewpoint',
    latitude: 48.858370,
    longitude: 2.294481,
    accuracy: 4.2,
    altitude: 35.0,
    address: 'Champ de Mars, 5 Avenue Anatole France, 75007 Paris, France',
    note: 'Best photo spot at sunset right across the gardens.',
    phone: '+33 892 70 12 39',
    photoUri: null,
    groupId: 5,
    isFavorite: true,
    createdAt: Date.now() - 86400000 * 2,
    updatedAt: Date.now() - 86400000 * 2,
  },
  {
    id: 2,
    name: 'Central Park North Meadow',
    latitude: 40.793200,
    longitude: -73.957500,
    accuracy: 6.8,
    altitude: 24.5,
    address: 'Central Park, New York, NY 10029, USA',
    note: 'Quiet picnic spot shaded under large elm trees.',
    phone: '+1 212-310-6600',
    photoUri: null,
    groupId: 3,
    isFavorite: true,
    createdAt: Date.now() - 86400000 * 1,
    updatedAt: Date.now() - 86400000 * 1,
  },
  {
    id: 3,
    name: 'North Gate Underground Parking',
    latitude: 48.860611,
    longitude: 2.337644,
    accuracy: 3.5,
    altitude: 28.0,
    address: 'Rue de Rivoli, 75001 Paris, France',
    note: 'Level -2, Pillar C14, ticket validated at exit.',
    phone: null,
    photoUri: null,
    groupId: 4,
    isFavorite: false,
    createdAt: Date.now() - 3600000 * 4,
    updatedAt: Date.now() - 3600000 * 4,
  },
];

const DEFAULT_SETTINGS: AppSettings = {
  coordinateFormat: 'DECIMAL_DEGREES',
  unitSystem: 'METRIC',
  defaultGroupId: null,
};

export const StorageService = {
  getPlaces(): Place[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.PLACES);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.PLACES, JSON.stringify(INITIAL_PLACES));
        return INITIAL_PLACES;
      }
      return JSON.parse(data);
    } catch {
      return INITIAL_PLACES;
    }
  },

  savePlaces(places: Place[]): void {
    try {
      localStorage.setItem(STORAGE_KEYS.PLACES, JSON.stringify(places));
    } catch (e) {
      console.error('Failed to save places', e);
    }
  },

  addPlace(place: Omit<Place, 'id' | 'createdAt' | 'updatedAt'>): Place {
    const places = this.getPlaces();
    const newId = places.length > 0 ? Math.max(...places.map((p) => p.id)) + 1 : 1;
    const now = Date.now();
    const newPlace: Place = {
      ...place,
      id: newId,
      createdAt: now,
      updatedAt: now,
    };
    places.unshift(newPlace);
    this.savePlaces(places);
    return newPlace;
  },

  updatePlace(updated: Place): void {
    const places = this.getPlaces();
    const idx = places.findIndex((p) => p.id === updated.id);
    if (idx !== -1) {
      places[idx] = { ...updated, updatedAt: Date.now() };
      this.savePlaces(places);
    }
  },

  deletePlace(id: number): void {
    const places = this.getPlaces().filter((p) => p.id !== id);
    this.savePlaces(places);
  },

  getGroups(): Group[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.GROUPS);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.GROUPS, JSON.stringify(DEFAULT_GROUPS));
        return DEFAULT_GROUPS;
      }
      return JSON.parse(data);
    } catch {
      return DEFAULT_GROUPS;
    }
  },

  saveGroups(groups: Group[]): void {
    try {
      localStorage.setItem(STORAGE_KEYS.GROUPS, JSON.stringify(groups));
    } catch (e) {
      console.error('Failed to save groups', e);
    }
  },

  addGroup(name: string, colorHex: string = '#1E88E5', iconName: string = 'pin'): Group {
    const groups = this.getGroups();
    const newId = groups.length > 0 ? Math.max(...groups.map((g) => g.id)) + 1 : 1;
    const newGroup: Group = {
      id: newId,
      name,
      colorHex,
      iconName,
      createdAt: Date.now(),
    };
    groups.push(newGroup);
    this.saveGroups(groups);
    return newGroup;
  },

  updateGroup(updated: Group): void {
    const groups = this.getGroups();
    const idx = groups.findIndex((g) => g.id === updated.id);
    if (idx !== -1) {
      groups[idx] = updated;
      this.saveGroups(groups);
    }
  },

  deleteGroup(id: number): void {
    const groups = this.getGroups().filter((g) => g.id !== id);
    this.saveGroups(groups);

    // Unassign group from places
    const places = this.getPlaces().map((p) => (p.groupId === id ? { ...p, groupId: null } : p));
    this.savePlaces(places);
  },

  getSettings(): AppSettings {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SETTINGS);
      if (!data) {
        return DEFAULT_SETTINGS;
      }
      return { ...DEFAULT_SETTINGS, ...JSON.parse(data) };
    } catch {
      return DEFAULT_SETTINGS;
    }
  },

  saveSettings(settings: AppSettings): void {
    try {
      localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(settings));
    } catch (e) {
      console.error('Failed to save settings', e);
    }
  },

  getSessions(): ShareSession[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SESSIONS);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  },

  saveSessions(sessions: ShareSession[]): void {
    try {
      localStorage.setItem(STORAGE_KEYS.SESSIONS, JSON.stringify(sessions));
    } catch (e) {
      console.error('Failed to save sessions', e);
    }
  },

  addSession(session: ShareSession): void {
    const sessions = this.getSessions();
    sessions.unshift(session);
    this.saveSessions(sessions);
  },

  clearAllData(): void {
    localStorage.removeItem(STORAGE_KEYS.PLACES);
    localStorage.removeItem(STORAGE_KEYS.GROUPS);
    localStorage.removeItem(STORAGE_KEYS.SESSIONS);
    // Restore clean defaults
    localStorage.setItem(STORAGE_KEYS.GROUPS, JSON.stringify(DEFAULT_GROUPS));
    localStorage.setItem(STORAGE_KEYS.PLACES, JSON.stringify([]));
  },
};
