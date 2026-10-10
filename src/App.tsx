import React, { useState, useEffect, useCallback } from 'react';
import { AppSettings, Group, Place, ScreenTab, ShareSession } from './types';
import { StorageService } from './services/storage';
import { useGeolocation } from './hooks/useGeolocation';
import { HomeScreen } from './components/HomeScreen';
import { PlacesScreen } from './components/PlacesScreen';
import { OsmMapView } from './components/OsmMapView';
import { LiveShareScreen } from './components/LiveShareScreen';
import { SettingsScreen } from './components/SettingsScreen';
import { PlaceDetailModal } from './components/PlaceDetailModal';
import { Navigation } from './components/Navigation';

export const App: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<ScreenTab>('home');
  const [places, setPlaces] = useState<Place[]>(() => StorageService.getPlaces());
  const [groups, setGroups] = useState<Group[]>(() => StorageService.getGroups());
  const [settings, setSettings] = useState<AppSettings>(() => StorageService.getSettings());
  const [sessions, setSessions] = useState<ShareSession[]>(() => StorageService.getSessions());
  const [selectedPlaceForDetail, setSelectedPlaceForDetail] = useState<Place | null>(null);

  const { location } = useGeolocation();

  // Find currently active live session
  const activeSession = sessions.find((s) => s.isActive && s.expiresAt > Date.now()) || null;

  // Handle Save New Position
  const handleSavePosition = useCallback(
    (placeData: {
      name?: string;
      latitude: number;
      longitude: number;
      accuracy: number;
      altitude: number | null;
      address?: string | null;
      note?: string | null;
      groupId?: number | null;
    }): Place => {
      const newPlace = StorageService.addPlace({
        name: placeData.name || 'Saved Position',
        latitude: placeData.latitude,
        longitude: placeData.longitude,
        accuracy: placeData.accuracy,
        altitude: placeData.altitude,
        address: placeData.address || null,
        note: placeData.note || null,
        phone: null,
        photoUri: null,
        groupId: placeData.groupId ?? settings.defaultGroupId,
        isFavorite: false,
      });

      setPlaces(StorageService.getPlaces());
      return newPlace;
    },
    [settings.defaultGroupId]
  );

  // Handle Update Place
  const handleUpdatePlace = useCallback((updated: Place) => {
    StorageService.updatePlace(updated);
    setPlaces(StorageService.getPlaces());
    setSelectedPlaceForDetail(updated);
  }, []);

  // Handle Delete Place
  const handleDeletePlace = useCallback((id: number) => {
    StorageService.deletePlace(id);
    setPlaces(StorageService.getPlaces());
    setSelectedPlaceForDetail((current) => (current?.id === id ? null : current));
  }, []);

  // Handle Toggle Favorite
  const handleToggleFavorite = useCallback((place: Place) => {
    const updated = { ...place, isFavorite: !place.isFavorite };
    StorageService.updatePlace(updated);
    setPlaces(StorageService.getPlaces());
  }, []);

  // Handle Batch Import
  const handleImportPlaces = useCallback((imported: Partial<Place>[]) => {
    const existing = StorageService.getPlaces();
    let maxId = existing.length > 0 ? Math.max(...existing.map((p) => p.id)) : 0;
    const now = Date.now();

    const newPlaces: Place[] = imported.map((p) => {
      maxId++;
      return {
        id: maxId,
        name: p.name || 'Imported Place',
        latitude: p.latitude || 0,
        longitude: p.longitude || 0,
        accuracy: p.accuracy ?? null,
        altitude: p.altitude ?? null,
        address: p.address ?? null,
        note: p.note ?? null,
        phone: p.phone ?? null,
        photoUri: p.photoUri ?? null,
        groupId: p.groupId ?? null,
        isFavorite: p.isFavorite ?? false,
        createdAt: p.createdAt || now,
        updatedAt: p.updatedAt || now,
      };
    });

    const combined = [...newPlaces, ...existing];
    StorageService.savePlaces(combined);
    setPlaces(combined);
  }, []);

  // Handle Create Category / Group
  const handleCreateGroup = useCallback((name: string, colorHex: string): Group => {
    const created = StorageService.addGroup(name, colorHex);
    setGroups(StorageService.getGroups());
    return created;
  }, []);

  // Handle Delete Category / Group
  const handleDeleteGroup = useCallback((id: number) => {
    StorageService.deleteGroup(id);
    setGroups(StorageService.getGroups());
    setPlaces(StorageService.getPlaces());
  }, []);

  // Handle Update Settings
  const handleUpdateSettings = useCallback((newSettings: AppSettings) => {
    StorageService.saveSettings(newSettings);
    setSettings(newSettings);
  }, []);

  // Handle Clear All Data
  const handleClearAllData = useCallback(() => {
    StorageService.clearAllData();
    setPlaces([]);
    setGroups(StorageService.getGroups());
    setSessions([]);
  }, []);

  // Handle Start Live Session
  const handleStartLiveSession = useCallback(
    (durationMinutes: number) => {
      const sessionId = Math.random().toString(36).substring(2, 12) + Date.now().toString(36);
      const startTime = Date.now();
      const expiresAt = startTime + durationMinutes * 60 * 1000;

      const shareUrl = `https://www.openstreetmap.org/?mlat=${location.latitude}&mlon=${location.longitude}#live=${sessionId}`;

      const newSession: ShareSession = {
        sessionId,
        startTime,
        durationMinutes,
        expiresAt,
        isActive: true,
        shareUrl,
        lastLatitude: location.latitude,
        lastLongitude: location.longitude,
        lastAccuracy: location.accuracy,
        lastUpdated: startTime,
      };

      StorageService.addSession(newSession);
      setSessions(StorageService.getSessions());
    },
    [location]
  );

  // Handle Stop Live Session
  const handleStopLiveSession = useCallback(() => {
    const all = StorageService.getSessions().map((s) => ({ ...s, isActive: false }));
    StorageService.saveSessions(all);
    setSessions(all);
  }, []);

  return (
    <div className="flex flex-col h-screen w-screen overflow-hidden bg-[#F8F9FF] text-[#191C20]">
      {/* Main View Area */}
      <main className="flex-1 relative overflow-hidden">
        {currentTab === 'home' && (
          <HomeScreen
            groups={groups}
            settings={settings}
            onSavePosition={handleSavePosition}
            onNavigateToPlaceDetail={(place) => setSelectedPlaceForDetail(place)}
            onNavigateToLiveShare={() => setCurrentTab('live')}
            savedPlaces={places}
          />
        )}

        {currentTab === 'places' && (
          <PlacesScreen
            places={places}
            groups={groups}
            settings={settings}
            currentLat={location.latitude}
            currentLng={location.longitude}
            onSelectPlace={(place) => setSelectedPlaceForDetail(place)}
            onToggleFavorite={handleToggleFavorite}
            onDeletePlace={handleDeletePlace}
            onImportPlaces={handleImportPlaces}
            onNavigateToSave={() => setCurrentTab('home')}
          />
        )}

        {currentTab === 'map' && (
          <div className="w-full h-full relative">
            <OsmMapView
              currentLat={location.latitude}
              currentLng={location.longitude}
              currentAccuracy={location.accuracy}
              places={places}
              groups={groups}
              onSelectPlace={(place) => setSelectedPlaceForDetail(place)}
              className="w-full h-full"
            />
          </div>
        )}

        {currentTab === 'live' && (
          <LiveShareScreen
            currentLat={location.latitude}
            currentLng={location.longitude}
            activeSession={activeSession}
            sessionsHistory={sessions}
            onStartSession={handleStartLiveSession}
            onStopSession={handleStopLiveSession}
          />
        )}

        {currentTab === 'settings' && (
          <SettingsScreen
            settings={settings}
            groups={groups}
            places={places}
            onUpdateSettings={handleUpdateSettings}
            onCreateGroup={handleCreateGroup}
            onDeleteGroup={handleDeleteGroup}
            onClearAllData={handleClearAllData}
          />
        )}
      </main>

      {/* Place Detail & Edit Modal */}
      {selectedPlaceForDetail && (
        <PlaceDetailModal
          place={selectedPlaceForDetail}
          groups={groups}
          settings={settings}
          currentLat={location.latitude}
          currentLng={location.longitude}
          onClose={() => setSelectedPlaceForDetail(null)}
          onUpdate={handleUpdatePlace}
          onDelete={handleDeletePlace}
          onCreateGroup={handleCreateGroup}
        />
      )}

      {/* Bottom Navigation */}
      <Navigation
        currentTab={currentTab}
        onTabChange={(tab) => setCurrentTab(tab)}
        placesCount={places.length}
        hasActiveLiveSession={activeSession !== null}
      />
    </div>
  );
};

export default App;
